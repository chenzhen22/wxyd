package com.cyz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自研轻量滑块验证码：Java2D 程序化绘制背景与拼图块（无静态图片依赖），
 * 内存存储 captchaId -> 缺口坐标，校验滑动位置容差后签发一次性 ticket。
 */
@Service
@Slf4j
public class SliderCaptchaService {

    public static final int BG_W = 300;
    public static final int BG_H = 150;
    public static final int PIECE = 44;
    private static final int PIECE_IMG = PIECE + 8; // 两侧留 bump 空间
    private static final long CAPTCHA_TTL = 5 * 60 * 1000L;
    private static final long TICKET_TTL = 2 * 60 * 1000L;
    private static final int TOLERANCE = 6;

    /** captchaId -> [targetX, targetY, expireAt] */
    private final Map<String, long[]> captchaStore = new ConcurrentHashMap<>();
    /** ticket -> expireAt */
    private final Map<String, Long> ticketStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** 生成一题：返回 {captchaId, bg(base64 PNG), piece(base64 PNG), y} */
    public Map<String, Object> generate() {
        evictExpired();
        int targetX = PIECE + 8 + random.nextInt(BG_W - 2 * (PIECE + 8));
        int targetY = 8 + random.nextInt(BG_H - PIECE - 16);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        captchaStore.put(captchaId, new long[]{targetX, targetY, System.currentTimeMillis() + CAPTCHA_TTL});

        BufferedImage bg = paintBackground();
        // 先从干净背景上裁出拼图块，再在背景上挖缺口阴影（避免阴影混入拼图块）
        BufferedImage piece = new BufferedImage(PIECE_IMG, PIECE_IMG, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = piece.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pg.translate(-targetX + 4, -targetY + 4);
        pg.drawImage(bg, 0, 0, null);
        pg.dispose();
        applyShapeMask(piece);

        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 110));
        g.fill(pieceShape(targetX, targetY));
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(2f));
        g.draw(pieceShape(targetX, targetY));
        g.dispose();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("captchaId", captchaId);
        data.put("bg", toBase64Png(bg));
        data.put("piece", toBase64Png(piece));
        data.put("y", targetY - 4);
        return data;
    }

    /** 校验滑动位置；通过则签发一次性 ticket，否则销毁本题（防重试爆破）。 */
    public String verify(String captchaId, long x) {
        evictExpired();
        long[] entry = captchaId == null ? null : captchaStore.remove(captchaId);
        if (entry == null) {
            throw new IllegalArgumentException("验证码已过期，请刷新重试");
        }
        if (Math.abs(x - entry[0]) > TOLERANCE) {
            throw new IllegalArgumentException("滑块位置不正确，请重试");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        ticketStore.put(ticket, System.currentTimeMillis() + TICKET_TTL);
        return ticket;
    }

    /** 消费一次性 ticket：有效返回 true 并销毁。 */
    public boolean consumeTicket(String ticket) {
        if (ticket == null) {
            return false;
        }
        Long expireAt = ticketStore.remove(ticket);
        return expireAt != null && expireAt > System.currentTimeMillis();
    }

    /** 拼图形状：圆形（与拼图块圆形遮罩一致，缺口视觉吻合） */
    private Ellipse2D.Float pieceShape(int x, int y) {
        return new Ellipse2D.Float(x, y, PIECE, PIECE);
    }

    private void applyShapeMask(BufferedImage piece) {
        int cx = PIECE_IMG / 2, cy = PIECE_IMG / 2, r = PIECE / 2;
        for (int py = 0; py < PIECE_IMG; py++) {
            for (int px = 0; px < PIECE_IMG; px++) {
                double d = Math.sqrt((px - cx) * (double) (px - cx) + (py - cy) * (double) (py - cy));
                if (d > r) {
                    piece.setRGB(px, py, 0);
                }
            }
        }
    }

    /** 程序化绘制随机背景：渐变底 + 随机圆与线条，干扰拼图定位 */
    private BufferedImage paintBackground() {
        BufferedImage img = new BufferedImage(BG_W, BG_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c1 = randomPastel(), c2 = randomPastel();
        g.setPaint(new java.awt.GradientPaint(0, 0, c1, BG_W, BG_H, c2));
        g.fillRect(0, 0, BG_W, BG_H);
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(255, 255, 255, 30 + random.nextInt(40)));
            int d = 18 + random.nextInt(50);
            g.fillOval(random.nextInt(BG_W) - d / 2, random.nextInt(BG_H) - d / 2, d, d);
        }
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(255, 255, 255, 25 + random.nextInt(30)));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(random.nextInt(BG_W), random.nextInt(BG_H), random.nextInt(BG_W), random.nextInt(BG_H));
        }
        g.dispose();
        return img;
    }

    private Color randomPastel() {
        return new Color(140 + random.nextInt(90), 140 + random.nextInt(90), 150 + random.nextInt(90));
    }

    private String toBase64Png(BufferedImage img) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return java.util.Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        captchaStore.entrySet().removeIf(e -> e.getValue()[2] < now);
        ticketStore.entrySet().removeIf(e -> e.getValue() < now);
    }
}
