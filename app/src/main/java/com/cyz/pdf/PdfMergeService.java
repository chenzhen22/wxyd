package com.cyz.pdf;

import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 证据材料整理核心服务。
 * <p>
 * 将若干 PDF / 图片（PNG/JPG）按序合并为单一 A4 竖向 PDF：
 * <ul>
 *   <li>每一份源文件占若干页，逐页放入新的 A4 竖向页面（595.28 × 841.89 pt）。</li>
 *   <li><b>横向内容自动旋转 90° 成竖向</b>：源页面/图片若为横向（宽 > 高），整体旋转后居中放入竖向页面，不裁剪、不变形。</li>
 *   <li><b>矢量保真</b>：PDF 源页以 Form XObject 方式内嵌，文字仍可选中、不栅格化、不会乱码或丢失内容。</li>
 *   <li><b>页脚页码</b>：支持「保留原标注（按文件名标注的码数）」或「顺序重新编号」两种模式；同一份多页可共享同一个标注（如发票两页都标 101）。</li>
 * </ul>
 * 设计参照一次真实需求：合并一个证据材料压缩包，按文件名里的页码标注编排，统一 A4 竖向，横向票据页旋转后并入。
 */
public class PdfMergeService {

    /** A4 竖向尺寸（点，72dpi） */
    public static final float A4W = 595.2756f;
    public static final float A4H = 841.8898f;

    /** 页边距（点） */
    private static final float MARGIN = 22f;
    /** 页脚预留高度（点），页码绘制在底部该区域之外 */
    private static final float FOOTER_H = 30f;
    /** 页脚字号 */
    private static final float FOOTER_FONT = 9f;

    /** 源文件描述 */
    public static class SourceFile {
        private final String name;
        private final byte[] data;

        public SourceFile(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public byte[] getData() {
            return data;
        }
    }

    /**
     * 合并多份源文件为单个 A4 竖向 PDF 的字节流。
     *
     * @param sources 源文件列表（原始顺序即输出顺序）
     * @param mode    "auto"=顺序重新编号；其他=保留原标注（使用 labels）
     * @param labels  与 sources 等长的页码标注（保留原标注模式生效，空白则回退为序号）
     * @return 生成的 PDF 字节流
     */
    public byte[] merge(List<SourceFile> sources, String mode, List<String> labels) throws IOException {
        boolean auto = "auto".equalsIgnoreCase(mode);
        PDDocument out = new PDDocument();
        try {
            LayerUtility lu = new LayerUtility(out);
            PDFont font = PDType1Font.HELVETICA_BOLD;
            int autoSeq = 0;
            for (int i = 0; i < sources.size(); i++) {
                SourceFile sf = sources.get(i);
                byte[] data = sf.getData();
                String baseLabel;
                if (auto) {
                    baseLabel = null;
                } else {
                    String raw = (labels != null && i < labels.size()) ? labels.get(i) : null;
                    String cleaned = sanitizeLabel(raw);
                    baseLabel = (cleaned == null || cleaned.isEmpty()) ? String.valueOf(i + 1) : cleaned;
                }

                if (isPdf(data)) {
                    PDDocument src = PDDocument.load(data);
                    try {
                        int n = src.getNumberOfPages();
                        for (int p = 0; p < n; p++) {
                            String label = auto ? String.valueOf(++autoSeq) : baseLabel;
                            addPdfPage(out, lu, src, p, label, font);
                        }
                    } finally {
                        src.close();
                    }
                } else if (isImage(data)) {
                    BufferedImage img = ImageIORead(data);
                    if (img == null) {
                        throw new PdfMergeException("无法解析的图片文件：" + sf.getName());
                    }
                    String label = auto ? String.valueOf(++autoSeq) : baseLabel;
                    addImagePage(out, img, sf.getName(), label, font);
                } else {
                    throw new PdfMergeException("不支持的文件类型（仅支持 PDF / PNG / JPG）：" + sf.getName());
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            out.save(bos);
            return bos.toByteArray();
        } finally {
            out.close();
        }
    }

    private void addPdfPage(PDDocument out, LayerUtility lu, PDDocument src, int pageIndex,
                           String label, PDFont font) throws IOException {
        PDPage srcPage = src.getPage(pageIndex);
        PDRectangle mb = srcPage.getMediaBox();
        float w = mb.getWidth();
        float h = mb.getHeight();
        int rot = srcPage.getRotation();
        boolean swap = (rot == 90 || rot == 270);
        float ew = swap ? h : w;       // 视觉宽
        float eh = swap ? w : h;       // 视觉高
        boolean landscape = ew > eh;

        PDPage outPage = new PDPage(new PDRectangle(A4W, A4H));
        out.addPage(outPage);

        PDFormXObject form = lu.importPageAsForm(src, pageIndex);
        PDPageContentStream cs = new PDPageContentStream(out, outPage);
        try {
            float scale = computeScale(landscape, ew, eh);
            AffineTransform m = buildMatrix(landscape, scale, w, h);
            form.setMatrix(m);
            cs.drawForm(form);
        } finally {
            cs.close();
        }
        drawFooter(out, outPage, label, font);
    }

    private void addImagePage(PDDocument out, BufferedImage img, String name,
                              String label, PDFont font) throws IOException {
        float w = img.getWidth();
        float h = img.getHeight();
        boolean landscape = w > h;

        PDImageXObject xi;
        String lower = name.toLowerCase();
        if (lower.endsWith(".png") || isPngMagic(img)) {
            xi = LosslessFactory.createFromImage(out, img);
        } else {
            xi = JPEGFactory.createFromImage(out, img);
        }

        PDPage outPage = new PDPage(new PDRectangle(A4W, A4H));
        out.addPage(outPage);

        PDPageContentStream cs = new PDPageContentStream(out, outPage);
        try {
            float scale = computeScale(landscape, w, h);
            AffineTransform m = buildMatrix(landscape, scale, w, h);
            Matrix im = new Matrix(
                    (float) m.getScaleX(), (float) m.getShearY(),
                    (float) m.getShearX(), (float) m.getScaleY(),
                    (float) m.getTranslateX(), (float) m.getTranslateY());
            cs.drawImage(xi, im);
        } finally {
            cs.close();
        }
        drawFooter(out, outPage, label, font);
    }

    /** 计算内容缩放比例：留边距并预留页脚高度，等比缩放后完整放入 */
    private float computeScale(boolean landscape, float ew, float eh) {
        float cw = A4W - 2 * MARGIN;
        float ch = A4H - 2 * MARGIN - FOOTER_H;
        if (landscape) {
            return Math.min(cw / eh, ch / ew);
        }
        return Math.min(cw / ew, ch / eh);
    }

    /** 构造「居中 + （横向时旋转 90°） + 等比缩放」的仿射矩阵 */
    private AffineTransform buildMatrix(boolean landscape, float scale, float w, float h) {
        float cx = A4W / 2f;
        float cy = (A4H + FOOTER_H) / 2f; // 内容区垂直中心（页脚之上）
        AffineTransform m = new AffineTransform();
        m.translate(cx, cy);
        if (landscape) {
            m.rotate(Math.toRadians(90));
        }
        m.scale(scale, scale);
        m.translate(-w / 2f, -h / 2f);
        return m;
    }

    private void drawFooter(PDDocument out, PDPage page, String label, PDFont font) throws IOException {
        if (label == null || label.isEmpty()) {
            return;
        }
        PDPageContentStream cs = new PDPageContentStream(out, page,
                PDPageContentStream.AppendMode.APPEND, true, true);
        try {
            cs.beginText();
            cs.setFont(font, FOOTER_FONT);
            cs.setNonStrokingColor(90f / 255f, 90f / 255f, 90f / 255f);
            float tw = font.getStringWidth(label) * FOOTER_FONT / 1000f;
            cs.newLineAtOffset((A4W - tw) / 2f, 16f);
            cs.showText(label);
            cs.endText();
        } finally {
            cs.close();
        }
    }

    // ===================== 文件类型识别 =====================

    static boolean isPdf(byte[] data) {
        return data != null && data.length >= 5
                && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F' && data[4] == '-';
    }

    static boolean isImage(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        // PNG: 89 50 4E 47
        if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return true;
        }
        // JPEG: FF D8 FF
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return true;
        }
        return false;
    }

    private static boolean isPngMagic(BufferedImage img) {
        return img != null;
    }

    private static BufferedImage ImageIORead(byte[] data) {
        try (InputStream in = new ByteArrayInputStream(data)) {
            return javax.imageio.ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 清洗页码标注：仅保留数字、字母、括号、连字符、逗号、句号与空格，限长 24。
     * Helvetica 不含中文字形，标注请使用数字/ASCII（与证据材料页码场景一致）。
     */
    static String sanitizeLabel(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (sb.length() >= 24) {
                break;
            }
            if (Character.isLetterOrDigit(c) || "()（）-—.,，。 ".indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    /** 业务异常：携带面向用户的错误信息 */
    public static class PdfMergeException extends RuntimeException {
        public PdfMergeException(String msg) {
            super(msg);
        }
    }
}
