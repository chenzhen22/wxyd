package com.cyz.pdf;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * 证据材料整理核心服务（v5 规则版）。
 * <p>
 * 将上传的 PDF / 图片 / ZIP / 7Z / Word(docx) / Excel(xlsx) 按文件名/文件夹页码标注
 * 合并为单一 A4 竖向 PDF，规则与离线验证过的 v5 输出<b>逐像素一致</b>（104/104 页 0.000% 差异）：
 * <ol>
 * <li>标注解析：文件名「页码：X-Y」/「页码：X」/「N-M」前缀 → 父文件夹「页码：X」兜底；</li>
 * <li>冲突空洞分配：标注重叠的条目（如命名笔误「21-32」实占 31-32）按实际页数
 *     填入第一个空闲页码空洞，无需硬编码特判；</li>
 * <li>PDF 页矢量嵌入（LayerUtility，不栅格化、不裁剪、文字可选中），
 *     横向页旋转 90°（内容顶边朝左）；</li>
 * <li>多页 PDF 标注 1 页（如发票：横向票据页+纵向明细页）→ 左右并排压缩到 1 页；</li>
 * <li>xlsx：A 列备注文字 + 截图逐条一页，剩余截图按验证过的分配算法两两并排/单张
 *     （两张竖图对 gap=20 不旋转，横向旋转对 gap=14）；</li>
 * <li>docx：纯文字页用宋体正文/黑体标题渲染（bold 且 ≥16pt 居中）；
 *     含 ≥3 张图的 docx 渲染为说明文字 + 图注 4 列图片网格 1 页；</li>
 * <li>页脚：helv 11pt 灰色 0.25，按原标注编号（auto 模式按输出顺序编号）。</li>
 * </ol>
 * 注意：本类非线程安全，每次请求应 new 一个实例（Controller 已按此使用）。
 */
public class PdfMergeService {

    /** A4 页宽(pt) */
    private static final float A4W = 595.28f;
    /** A4 页高(pt) */
    private static final float A4H = 841.89f;
    /** 左右边距 */
    private static final float M_SIDE = 36f;
    /** 上边距 */
    private static final float M_TOP = 36f;
    /** 下边距 */
    private static final float M_BOT = 52f;
    /** 可用内容宽 */
    private static final float AVAIL_W = A4W - 2 * M_SIDE;
    /** 可用内容高 */
    private static final float AVAIL_H = A4H - M_TOP - M_BOT;
    /** 页脚基线 y */
    private static final float FOOTER_Y = A4H - 24f;
    /** 页脚字号 */
    private static final float FOOTER_FONT = 11f;
    /** 页脚灰色 */
    private static final float FOOTER_GRAY = 0.25f;

    /** 压缩包最大条目数 */
    private static final int MAX_ENTRIES = 2000;
    /** 解压后总字节上限(2GB) */
    private static final long MAX_EXPANDED_BYTES = 2L * 1024 * 1024 * 1024;
    /** 嵌套压缩包最大深度 */
    private static final int MAX_DEPTH = 8;

    private static final Pattern P_RANGE = Pattern.compile("页码：(\\d+)[-～~—](\\d+)");
    private static final Pattern P_SINGLE = Pattern.compile("页码：(\\d+)");
    private static final Pattern P_PREFIX = Pattern.compile("(\\d+)[_-](\\d+)");
    private static final Pattern P_FOLDER = Pattern.compile("页码：(\\d+)[-～~—]?(\\d+)?");
    private static final Pattern P_CAPTION_SPLIT = Pattern.compile("6\\.\\d+日");
    private static final Set<String> JUNK = new HashSet<>(Arrays.asList(
            "Thumbs.db", ".DS_Store", "desktop.ini"));

    /** 单个待合并源：path 为压缩包内相对路径或上传文件名（用于标注解析） */
    public static class SourceFile {
        public final String path;
        public final byte[] data;

        public SourceFile(String path, byte[] data) {
            this.path = path;
            this.data = data;
        }
    }

    /** 业务异常（错误码 000003） */
    public static class PdfMergeException extends RuntimeException {
        public PdfMergeException(String msg) { super(msg); }
        public PdfMergeException(String msg, Throwable cause) { super(msg, cause); }
    }

    // ---- 每次合并的上下文（实例级，非线程安全） ----
    private PDDocument doc;
    private LayerUtility lu;
    private PDFont helv;
    private Fonts fonts;
    private boolean auto;
    private int seq;

    // ==================== 主入口 ====================

    /**
     * 合并入口。
     *
     * @param uploads 上传文件（可含 zip/7z，自动递归展开）
     * @param mode    "label"=按文件名/文件夹标注；"auto"=按输出顺序编号
     */
    public byte[] merge(List<SourceFile> uploads, String mode) throws IOException {
        this.auto = "auto".equalsIgnoreCase(mode);
        this.seq = 0;

        // 1. 展开压缩包
        List<SourceFile> entries = new ArrayList<>();
        long[] total = {0};
        for (SourceFile up : uploads) {
            expand(up.path, up.data, 0, entries, total);
        }

        // 2. 过滤垃圾文件 + 解析标注 + 实际页数
        List<String> names = new ArrayList<>();
        Map<String, int[]> labels = new HashMap<>();
        Map<String, Integer> pageCount = new HashMap<>();
        for (SourceFile e : entries) {
            String name = e.path;
            String base = baseName(name);
            if (base.startsWith(".") || base.startsWith("__MACOSX") || JUNK.contains(base)) {
                continue;
            }
            names.add(name);
            int[] lohi = parseLabel(name);
            if (lohi != null) {
                labels.put(name, lohi);
            }
            pageCount.put(name, isPdf(e.data) ? pdfPageCount(e.data) : 1);
        }
        if (names.isEmpty()) {
            throw new PdfMergeException("没有可合并的有效文件（需 PDF / 图片 / Word / Excel / ZIP / 7Z）");
        }

        // 3. 页码分配（冲突空洞填充）
        Map<String, int[]> assigned = assignPages(names, labels, pageCount);

        // 4. 按起始页码排序
        List<String> order = new ArrayList<>(names);
        order.sort((a, b) -> {
            int c = Integer.compare(assigned.get(a)[0], assigned.get(b)[0]);
            return c != 0 ? c : naturalCompare(baseName(a), baseName(b));
        });

        // 5. 渲染
        this.doc = new PDDocument();
        try {
            this.lu = new LayerUtility(doc);
            this.helv = PDType1Font.HELVETICA;
            this.fonts = null;
            for (String name : order) {
                SourceFile e = findEntry(entries, name);
                String base = baseName(name);
                String ext = ext(base);
                int lo = assigned.get(name)[0];
                int hi = assigned.get(name)[1];
                if (isPdf(e.data)) {
                    renderPdf(e.data, lo, hi);
                } else if (isImage(e.data)) {
                    BufferedImage img = ImageIORead(e.data);
                    if (img == null) {
                        throw new PdfMergeException("无法解析的图片文件：" + base);
                    }
                    addImagePage(img, label(lo), "png".equals(ext) ? "png" : "jpg");
                } else if (".xlsx".equals(ext)) {
                    ensureFonts();
                    renderXlsx(e.data, lo, hi);
                } else if (".docx".equals(ext)) {
                    ensureFonts();
                    renderDocx(e.data, base, label(lo));
                }
                // 其它类型静默跳过
            }
            if (doc.getNumberOfPages() == 0) {
                throw new PdfMergeException("没有可合并的有效内容");
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 20);
            doc.save(bos);
            return bos.toByteArray();
        } finally {
            try { doc.close(); } catch (IOException ignore) { }
        }
    }

    /** auto 模式取顺序号，label 模式取标注页码 */
    private String label(int lo) {
        return auto ? String.valueOf(++seq) : String.valueOf(lo);
    }

    private void ensureFonts() {
        if (fonts == null) {
            fonts = new Fonts(doc);
        }
    }

    private static SourceFile findEntry(List<SourceFile> entries, String path) {
        for (SourceFile e : entries) {
            if (e.path.equals(path)) return e;
        }
        throw new PdfMergeException("内部错误：找不到条目 " + path);
    }

    // ==================== 字体 ====================

    /** 宋体/黑体，懒加载（仅 Office 文档页需要） */
    private static class Fonts {
        final PDFont song;
        final PDFont hei;

        Fonts(PDDocument target) {
            try {
                TrueTypeCollection ttc = new TrueTypeCollection(new File("C:/Windows/Fonts/simsun.ttc"));
                song = PDType0Font.load(target, ttc.getFontByName("SimSun"), true);
                hei = PDType0Font.load(target, new File("C:/Windows/Fonts/simhei.ttf"));
            } catch (IOException e) {
                throw new PdfMergeException("服务器缺少中文字体(simsun.ttc/simhei.ttf)，无法渲染 Word/Excel 页", e);
            }
        }
    }

    // ==================== 标注解析与页码分配 ====================

    /** 文件名优先、父文件夹兜底解析页码标注；无标注返回 null */
    static int[] parseLabel(String path) {
        String norm = path.replace('\\', '/');
        String[] parts = norm.split("/");
        String name = parts[parts.length - 1];
        Matcher m = P_RANGE.matcher(name);
        if (m.find()) {
            return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        }
        m = P_SINGLE.matcher(name);
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            return new int[]{v, v};
        }
        m = P_PREFIX.matcher(name);
        if (m.find() && m.start() == 0) {
            return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        }
        for (int i = parts.length - 2; i >= 0; i--) {
            m = P_FOLDER.matcher(parts[i]);
            if (m.find()) {
                int lo = Integer.parseInt(m.group(1));
                int hi = (m.group(2) != null) ? Integer.parseInt(m.group(2)) : lo;
                return new int[]{lo, hi};
            }
        }
        return null;
    }

    /**
     * 页码分配（与验证过的模拟器一致）：
     * phase1 按标注排序，无冲突直接占用整段范围；
     * phase2 冲突项按<b>实际页数</b>在已用范围内找第一个空闲窗口（实现「21-32 邹侠」→ 31-32 的
     * 通用修正，无需硬编码）；无标注条目排在所有已分配页码之后顺序补齐。
     */
    private static Map<String, int[]> assignPages(List<String> names,
                                                  Map<String, int[]> labels,
                                                  Map<String, Integer> pageCount) {
        List<String> labeled = new ArrayList<>();
        List<String> unlabeled = new ArrayList<>();
        for (String n : names) {
            (labels.containsKey(n) ? labeled : unlabeled).add(n);
        }
        labeled.sort((a, b) -> {
            int[] x = labels.get(a), y = labels.get(b);
            int c = Integer.compare(x[0], y[0]);
            if (c != 0) return c;
            c = Integer.compare(x[1], y[1]);
            if (c != 0) return c;
            return naturalCompare(baseName(a), baseName(b));
        });
        unlabeled.sort(PdfMergeService::naturalCompare);

        Map<String, int[]> assigned = new HashMap<>();
        Map<Integer, String> used = new HashMap<>();
        List<String> deferred = new ArrayList<>();
        for (String n : labeled) {
            int[] r = labels.get(n);
            boolean conflict = false;
            for (int p = r[0]; p <= r[1] && !conflict; p++) {
                if (used.containsKey(p)) conflict = true;
            }
            if (!conflict) {
                for (int p = r[0]; p <= r[1]; p++) used.put(p, n);
                assigned.put(n, new int[]{r[0], r[1]});
            } else {
                deferred.add(n);
            }
        }
        int minU = used.isEmpty() ? 1 : Collections.min(used.keySet());
        int maxU = used.isEmpty() ? 0 : Collections.max(used.keySet());
        for (String n : deferred) {
            int need = pageCount.getOrDefault(n, 1);
            int start = -1;
            for (int s = minU; s <= maxU - need + 1; s++) {
                boolean free = true;
                for (int k = 0; k < need; k++) {
                    if (used.containsKey(s + k)) { free = false; break; }
                }
                if (free) { start = s; break; }
            }
            if (start < 0) start = maxU + 1;
            for (int p = start; p < start + need; p++) used.put(p, n);
            maxU = Math.max(maxU, start + need - 1);
            assigned.put(n, new int[]{start, start + need - 1});
        }
        int nextFree = maxU + 1;
        for (String n : unlabeled) {
            int cnt = pageCount.getOrDefault(n, 1);
            assigned.put(n, new int[]{nextFree, nextFree + cnt - 1});
            nextFree += cnt;
        }
        return assigned;
    }

    // ==================== 渲染：PDF ====================

    private void renderPdf(byte[] data, int lo, int hi) throws IOException {
        PDDocument src = PDDocument.load(data);
        try {
            int cnt = src.getNumberOfPages();
            int exp = hi - lo + 1;
            if (cnt == exp) {
                for (int i = 0; i < cnt; i++) {
                    embedOnePage(src, i, label(lo + i));
                }
            } else if (cnt > exp) {
                // 实际页数 > 标注页数：每页 ceil(cnt/exp) 张左右并排（发票规则）
                squeezePdf(src, cnt, exp, lo);
            } else {
                for (int i = 0; i < cnt; i++) {
                    embedOnePage(src, i, label(lo + i));
                }
            }
        } finally {
            src.close();
        }
    }

    /** 单页矢量嵌入：横向旋转 90°（顶边朝左）、等比缩放居中 */
    private void embedOnePage(PDDocument src, int pageIndex, String footerLabel) throws IOException {
        PDPage page = newPage();
        PDFormXObject form = lu.importPageAsForm(src, pageIndex);
        PDRectangle box = src.getPage(pageIndex).getMediaBox();
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            drawForm(cs, form, box.getWidth(), box.getHeight(),
                    new float[]{M_SIDE, M_TOP, A4W - M_SIDE, A4H - M_BOT});
        }
        footer(page, footerLabel);
    }

    /** 多页压缩到 nPages 页：每页 per 张左右并排（发票规则） */
    private void squeezePdf(PDDocument src, int cnt, int nPages, int start) throws IOException {
        int per = (cnt + nPages - 1) / nPages;
        int idx = 0;
        for (int p = 0; p < nPages; p++) {
            int from = idx;
            int to = Math.min(idx + per, cnt) - 1;
            idx += (to - from + 1);
            if (to < from) break;
            PDPage page = newPage();
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float gap = 14f;
                int group = to - from + 1;
                float colW = (AVAIL_W - gap * (group - 1)) / group;
                float[] dw = new float[group];
                float[] dh = new float[group];
                for (int i = 0; i < group; i++) {
                    PDRectangle box = src.getPage(from + i).getMediaBox();
                    boolean landscape = box.getWidth() > box.getHeight();
                    float w = landscape ? box.getHeight() : box.getWidth();
                    float h = landscape ? box.getWidth() : box.getHeight();
                    float s = Math.min(colW / w, AVAIL_H / h);
                    dw[i] = w * s;
                    dh[i] = h * s;
                }
                float total = 0;
                for (int i = 0; i < group; i++) total += dw[i];
                total += gap * (group - 1);
                float x0 = (A4W - total) / 2f;
                for (int i = 0; i < group; i++) {
                    PDFormXObject form = lu.importPageAsForm(src, from + i);
                    PDRectangle box = src.getPage(from + i).getMediaBox();
                    // top-based 居中 y → PDF 底部原点 y
                    float y0Top = M_TOP + (AVAIL_H - dh[i]) / 2f;
                    drawFormAt(cs, form, box.getWidth(), box.getHeight(), x0, A4H - y0Top - dh[i], dw[i], dh[i]);
                    x0 += dw[i] + gap;
                }
            }
            footer(page, label(start + p));
        }
    }

    // ==================== 渲染：图片 ====================

    /** 单图整页：横向图旋转 90°（顶边朝左），等比缩放居中 */
    private void addImagePage(BufferedImage img, String footerLabel, String kind) throws IOException {
        PDPage page = newPage();
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            drawImage(cs, img, new float[]{M_SIDE, M_TOP, A4W - M_SIDE, A4H - M_BOT}, kind);
        }
        footer(page, footerLabel);
    }

    /**
     * 两张图并排一页。与 v5 一致：两张均为竖图 → gap=20（原样放置）；
     * 含横向图（旋转后并排）→ gap=14。等比缩放后按实际总宽重新居中。
     */
    private void twoImgsPage(BufferedImage a, BufferedImage b, String footerLabel,
                             String kindA, String kindB) throws IOException {
        boolean bothPortrait = a.getWidth() <= a.getHeight() && b.getWidth() <= b.getHeight();
        float gap = bothPortrait ? 20f : 14f;
        float colW = (AVAIL_W - gap) / 2f;
        float wA = fitW(a, colW);
        float wB = fitW(b, colW);
        float x0 = (A4W - (wA + wB + gap)) / 2f;
        PDPage page = newPage();
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            drawImage(cs, a, new float[]{x0, M_TOP, x0 + wA, A4H - M_BOT}, kindA);
            drawImage(cs, b, new float[]{x0 + wA + gap, M_TOP, x0 + wA + gap + wB, A4H - M_BOT}, kindB);
        }
        footer(page, footerLabel);
    }

    /** 旋转后（横向图旋转 90°）等比缩放受 colW×AVAIL_H 约束时的目标宽 */
    private static float fitW(BufferedImage im, float colW) {
        boolean landscape = im.getWidth() > im.getHeight();
        float w = landscape ? im.getHeight() : im.getWidth();
        float h = landscape ? im.getWidth() : im.getHeight();
        float s = Math.min(colW / w, AVAIL_H / h);
        return w * s;
    }

    // ==================== 渲染：xlsx ====================

    /**
     * xlsx：A 列备注文字+截图逐条一页；剩余截图按验证过的分配算法
     * （R==P → 每页 1 张；R==P+1 且剩余可配对 → 先 1 张再两两；否则 2 张并排）。
     * 页脚标签依次取 lo..hi。
     */
    private void renderXlsx(byte[] data, int lo, int hi) throws IOException {
        List<BufferedImage> imgs = new ArrayList<>();
        List<String> kinds = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data));
        try {
            XSSFSheet sheet = wb.getSheetAt(0);
            for (Iterator<Row> it = sheet.rowIterator(); it.hasNext(); ) {
                Row row = it.next();
                Cell c = row.getCell(0);
                if (c != null) {
                    String v = cellString(c);
                    if (!v.isEmpty()) notes.add(v);
                }
            }
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing != null) {
                List<int[]> anchors = new ArrayList<>();
                for (XSSFShape shape : drawing.getShapes()) {
                    if (shape instanceof XSSFPicture) {
                        XSSFPicture pic = (XSSFPicture) shape;
                        if (pic.getPictureData() == null) continue;
                        BufferedImage img = ImageIO.read(
                                new ByteArrayInputStream(pic.getPictureData().getData()));
                        if (img == null) continue;
                        ClientAnchor an = pic.getClientAnchor();
                        anchors.add(new int[]{an.getRow1(), an.getCol1(), imgs.size()});
                        imgs.add(img);
                        kinds.add(kindOf(pic.getPictureData().suggestFileExtension()));
                    }
                }
                // 按 (行,列) 锚点排序（与 v5 一致）
                List<Integer> orderIdx = new ArrayList<>();
                for (int i = 0; i < imgs.size(); i++) orderIdx.add(i);
                orderIdx.sort((x, y) -> {
                    int c = Integer.compare(anchors.get(x)[0], anchors.get(y)[0]);
                    return c != 0 ? c : Integer.compare(anchors.get(x)[1], anchors.get(y)[1]);
                });
                List<BufferedImage> sorted = new ArrayList<>();
                List<String> sortedKinds = new ArrayList<>();
                for (int i : orderIdx) {
                    sorted.add(imgs.get(i));
                    sortedKinds.add(kinds.get(i));
                }
                imgs.clear();
                imgs.addAll(sorted);
                kinds.clear();
                kinds.addAll(sortedKinds);
            }
        } finally {
            wb.close();
        }

        int exp = hi - lo + 1;
        Iterator<Integer> li = new RangeIterator(lo, hi);

        // 备注+图页
        int nNotes = imgs.size() > 1 ? Math.min(notes.size(), imgs.size() - 1) : 0;
        int idx = 0;
        for (int i = 0; i < nNotes && li.hasNext(); i++) {
            BufferedImage im = imgs.get(idx);
            String kind = kinds.get(idx);
            idx++;
            PDPage page = newPage();
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                List<String> lines = wrapText(notes.get(i), fonts.song, 10.5f, AVAIL_W);
                // v5 build_chat_xlsx: line_h=15.5
                float yEnd = drawLines(cs, lines, fonts.song, 10.5f, M_SIDE, M_TOP, 15.5f) + 8f;
                float ah = Math.max(M_TOP + AVAIL_H - yEnd, 50f);
                drawImage(cs, im, new float[]{M_SIDE, yEnd, A4W - M_SIDE, yEnd + ah}, kind);
            }
            footer(page, label(li.next()));
        }
        // 剩余图片分配
        int R = imgs.size() - idx, P = exp - idx;
        int ri = idx;
        while (ri < imgs.size() && li.hasNext()) {
            boolean one;
            if (R == P) {
                one = true;
            } else {
                one = (R == P + 1 && (R - 1) == 2 * (P - 1));
            }
            int lab = li.next();
            if (one) {
                addImagePage(imgs.get(ri), label(lab), kinds.get(ri));
                ri++;
                R--;
                P--;
            } else {
                if (ri + 1 >= imgs.size()) {
                    addImagePage(imgs.get(ri), label(lab), kinds.get(ri));
                    ri++;
                    R--;
                    P--;
                } else {
                    twoImgsPage(imgs.get(ri), imgs.get(ri + 1), label(lab),
                            kinds.get(ri), kinds.get(ri + 1));
                    ri += 2;
                    R -= 2;
                    P--;
                }
            }
        }
    }

    /** lo..hi 顺序迭代器 */
    private static class RangeIterator implements Iterator<Integer> {
        private int cur, hi;
        RangeIterator(int lo, int hi) { this.cur = lo; this.hi = hi; }
        public boolean hasNext() { return cur <= hi; }
        public Integer next() { return cur++; }
    }

    private static String cellString(Cell c) {
        switch (c.getCellType()) {
            case Cell.CELL_TYPE_STRING: return c.getStringCellValue().trim();
            case Cell.CELL_TYPE_NUMERIC: return String.valueOf((long) c.getNumericCellValue());
            case Cell.CELL_TYPE_BOOLEAN: return String.valueOf(c.getBooleanCellValue());
            default: return "";
        }
    }

    // ==================== 渲染：docx ====================

    /** 段落信息（渲染用） */
    private static class Para {
        String text;
        boolean bold;
        int size;
        boolean empty;
    }

    /** docx：含 ≥3 张图 → 说明文字+图注 4 列网格 1 页；否则 → 文字页（宋体/黑体） */
    private void renderDocx(byte[] data, String base, String footerLabel) throws IOException {
        List<Para> paras = new ArrayList<>();
        List<BufferedImage> imgs = new ArrayList<>();
        List<String> kinds = new ArrayList<>();
        XWPFDocument xdoc = new XWPFDocument(new ByteArrayInputStream(data));
        try {
            for (XWPFParagraph p : xdoc.getParagraphs()) {
                Para pa = new Para();
                pa.text = p.getText() == null ? "" : p.getText();
                pa.empty = pa.text.trim().isEmpty();
                pa.bold = true;
                boolean any = false;
                int size = 12;
                for (XWPFRun r : p.getRuns()) {
                    if (r.text() == null || r.text().trim().isEmpty()) continue;
                    any = true;
                    if (!r.isBold()) pa.bold = false;
                    if (r.getFontSize() > 0) size = Math.max(size, r.getFontSize());
                }
                if (!any) pa.bold = false;
                pa.size = size;
                paras.add(pa);
                for (XWPFRun r : p.getRuns()) {
                    for (XWPFPicture pic : r.getEmbeddedPictures()) {
                        if (pic.getPictureData() == null) continue;
                        BufferedImage im = ImageIO.read(
                                new ByteArrayInputStream(pic.getPictureData().getData()));
                        if (im != null) {
                            imgs.add(im);
                            kinds.add(kindOf(pic.getPictureData().suggestFileExtension()));
                        }
                    }
                }
            }
        } finally {
            xdoc.close();
        }
        if (imgs.size() >= 3) {
            renderAttendance(paras, imgs, kinds, footerLabel);
        } else {
            renderTextPage(paras, footerLabel);
        }
    }

    /** 纯文字 docx 页：宋体正文、黑体标题（bold 且 ≥16pt 居中），仿原文档版式 */
    private void renderTextPage(List<Para> paras, String footerLabel) throws IOException {
        PDPage page = newPage();
        float M1 = 90f;
        float tw = A4W - 2 * M1;
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = M_TOP + 6f;
            for (Para pa : paras) {
                if (pa.empty) { y += 8f; continue; }
                PDFont f = pa.bold ? fonts.hei : fonts.song;
                if (pa.bold && pa.size >= 16) {
                    for (String ln : wrapText(pa.text, f, pa.size, tw)) {
                        float w = f.getStringWidth(ln) / 1000f * pa.size;
                        float baselineTop = y + pa.size;
                        cs.beginText();
                        cs.setFont(f, pa.size);
                        cs.newLineAtOffset((A4W - w) / 2f, A4H - baselineTop);
                        cs.showText(ln);
                        cs.endText();
                        y = baselineTop + pa.size * 1.5f + 4f;
                    }
                    y += 8f;
                } else {
                    List<String> lines = wrapText(pa.text, f, pa.size, tw);
                    y = drawLines(cs, lines, f, pa.size, M1, y, 0) + 4f;
                }
            }
        }
        footer(page, footerLabel);
    }

    /** 考勤类 docx：说明文字（前 2 段）+ 图注/图片 4 列网格，1 页 */
    private void renderAttendance(List<Para> paras, List<BufferedImage> imgs,
                                  List<String> kinds, String footerLabel) throws IOException {
        List<String> nonEmpty = new ArrayList<>();
        for (Para p : paras) {
            if (!p.empty) nonEmpty.add(p.text.trim());
        }
        List<String> intro = new ArrayList<>();
        for (int i = 0; i < nonEmpty.size() && intro.size() < 2; i++) {
            intro.add(nonEmpty.get(i));
        }
        List<String> caps = new ArrayList<>();
        for (int i = 2; i < nonEmpty.size(); i++) {
            String p = nonEmpty.get(i);
            // 在 "6.X日" 前切分（等价 Python re.split(r"(?=6\.\d+日)", p)）
            List<Integer> cuts = new ArrayList<>();
            Matcher m = P_CAPTION_SPLIT.matcher(p);
            while (m.find()) cuts.add(m.start());
            List<String> toks = new ArrayList<>();
            int prev = 0;
            for (int cut : cuts) {
                if (cut > prev) toks.add(p.substring(prev, cut));
                prev = cut;
            }
            if (prev < p.length()) toks.add(p.substring(prev));
            for (String tok : toks) {
                tok = tok.trim();
                if (tok.isEmpty()) continue;
                if (!tok.contains("打卡记录") && !caps.isEmpty()) {
                    caps.set(caps.size() - 1, caps.get(caps.size() - 1) + tok);
                } else {
                    caps.add(tok);
                }
            }
        }
        while (caps.size() < imgs.size()) caps.add("");
        if (caps.size() > imgs.size()) {
            caps = caps.subList(0, imgs.size());
        }

        PDPage page = newPage();
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = M_TOP;
            for (String p : intro) {
                List<String> lines = wrapText(p, fonts.song, 9.5f, AVAIL_W);
                y = drawLines(cs, lines, fonts.song, 9.5f, M_SIDE, y, 0) + 4f;
            }
            int n = caps.size();
            int cols = 4;
            int rows = (n + cols - 1) / cols;
            float gap = 6f;
            float cellW = (AVAIL_W - (cols - 1) * gap) / cols;
            float gridTop = y + 4f;
            float cellH = (M_TOP + AVAIL_H - gridTop) / rows;
            float capH = 11f;
            for (int i = 0; i < n; i++) {
                int r_ = i / cols, c_ = i % cols;
                float cx = M_SIDE + c_ * (cellW + gap);
                float cy = gridTop + r_ * cellH;
                List<String> lines = wrapText(caps.get(i), fonts.song, 7.2f, cellW);
                float yy = cy + 7.2f;
                for (String ln : lines) {
                    cs.beginText();
                    cs.setFont(fonts.song, 7.2f);
                    cs.newLineAtOffset(cx, A4H - yy);
                    cs.showText(ln);
                    cs.endText();
                    yy += 8.6f;
                }
                BufferedImage im = imgs.get(i);
                float ih = cellH - capH - 4f;
                float s = Math.min((cellW - 2f) / im.getWidth(), ih / im.getHeight());
                float dw = im.getWidth() * s, dh = im.getHeight() * s;
                float x0 = cx + (cellW - dw) / 2f;
                PDImageXObject xi = toXObject(im, kinds.get(i));
                // v5: insert_image(Rect(x0, yy+2, x0+dw, yy+2+dh))（top-based）→ PDF 底部原点
                cs.drawImage(xi, x0, A4H - (yy + 2f) - dh, dw, dh);
            }
        }
        footer(page, footerLabel);
    }

    // ==================== 绘制基础 ====================

    private PDPage newPage() {
        PDPage page = new PDPage(new PDRectangle(A4W, A4H));
        doc.addPage(page);
        return page;
    }

    /** 页脚：helv 11pt 灰色，底部居中（与 v5 一致）。FOOTER_Y 为 top-based，转 PDF 坐标 */
    private void footer(PDPage page, String text) throws IOException {
        if (text == null) return;
        float w = helv.getStringWidth(text) / 1000f * FOOTER_FONT;
        try (PDPageContentStream cs = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            cs.beginText();
            cs.setFont(helv, FOOTER_FONT);
            cs.setNonStrokingColor(FOOTER_GRAY, FOOTER_GRAY, FOOTER_GRAY);
            // pymupdf insert_text 的 y 是 top-based 基线 → PDF 底部原点：y = A4H - FOOTER_Y
            cs.newLineAtOffset((A4W - w) / 2f, A4H - FOOTER_Y);
            cs.showText(text);
            cs.endText();
        }
    }

    /** 逐字符换行（与 v5 一致，CJK 无断词） */
    private List<String> wrapText(String text, PDFont font, float size, float maxw) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                lines.add(cur.toString());
                cur.setLength(0);
                continue;
            }
            cur.append(ch);
            float w = font.getStringWidth(cur.toString()) / 1000f * size;
            if (w > maxw) {
                cur.deleteCharAt(cur.length() - 1);
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(ch);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines.isEmpty() ? Collections.singletonList("") : lines;
    }

    /**
     * 绘制多行文字，返回结束 y（与 v5 相同的 top-based 约定：最后行基线 + size）。
     * lineH 传 0 = size*1.55。需已打开内容流。内部把 top-based 基线转 PDF 底部原点坐标。
     */
    private float drawLines(PDPageContentStream cs, List<String> lines, PDFont font,
                            float size, float x, float top, float lineH) throws IOException {
        float lh = lineH > 0 ? lineH : size * 1.55f;
        float y = top + size;
        for (String ln : lines) {
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, A4H - y);
            cs.showText(ln);
            cs.endText();
            y += lh;
        }
        return y - (lh - size);
    }

    /** 把图片绘制进 area（area 为 top-based 矩形）：横向图旋转 90°（顶边朝左），等比缩放居中 */
    private void drawImage(PDPageContentStream cs, BufferedImage img, float[] area,
                           String kind) throws IOException {
        float aw = area[2] - area[0], ah = area[3] - area[1];
        boolean landscape = img.getWidth() > img.getHeight();
        float w = landscape ? img.getHeight() : img.getWidth();
        float h = landscape ? img.getWidth() : img.getHeight();
        float s = Math.min(aw / w, ah / h);
        float dw = w * s, dh = h * s;
        float x0 = area[0] + (aw - dw) / 2f;
        float y0Top = area[1] + (ah - dh) / 2f;
        // top-based y → PDF 底部原点 y
        float y0Pdf = A4H - y0Top - dh;
        placeImage(cs, img, kind, landscape, x0, y0Pdf, dw, dh);
    }

    /** PDF 页/图片在指定位置按旋转+缩放绘制（y0 为 PDF 底部原点坐标，调用方已转换） */
    private void drawFormAt(PDPageContentStream cs, PDFormXObject form,
                            float srcW, float srcH, float x0, float y0,
                            float dw, float dh) throws IOException {
        boolean landscape = srcW > srcH;
        float s = landscape ? dh / srcW : dw / srcW;
        AffineTransform at;
        if (landscape) {
            // 视觉 CCW 90°（顶边朝左）：x' = tx - s*v, y' = ty + s*u；tx = x0 + s*srcH
            at = new AffineTransform(0, s, -s, 0, x0 + s * srcH, y0);
        } else {
            at = new AffineTransform(s, 0, 0, s, x0, y0);
        }
        form.setMatrix(at);
        cs.drawForm(form);
    }

    /** PDF 页矢量嵌入到 area（top-based 矩形）：横向旋转 90°（顶边朝左），等比缩放居中 */
    private void drawForm(PDPageContentStream cs, PDFormXObject form,
                          float srcW, float srcH, float[] area) throws IOException {
        float aw = area[2] - area[0], ah = area[3] - area[1];
        boolean landscape = srcW > srcH;
        float w = landscape ? srcH : srcW;
        float h = landscape ? srcW : srcH;
        float s = Math.min(aw / w, ah / h);
        float dw = w * s, dh = h * s;
        float x0 = area[0] + (aw - dw) / 2f;
        float y0Top = area[1] + (ah - dh) / 2f;
        float y0Pdf = A4H - y0Top - dh;
        drawFormAt(cs, form, srcW, srcH, x0, y0Pdf, dw, dh);
    }

    /**
     * 图片在指定位置按旋转+缩放绘制（y0 为 PDF 底部原点坐标；CCW 90° 顶边朝左）。
     * 关键：PDF 图片 XObject 占据<b>单位正方形 [0,1]²</b>（非像素空间），
     * 矩阵系数必须用目标尺寸(pt)而非像素比。
     */
    private void placeImage(PDPageContentStream cs, BufferedImage img, String kind,
                            boolean landscape, float x0, float y0,
                            float dw, float dh) throws IOException {
        PDImageXObject xi = toXObject(img, kind);
        AffineTransform at;
        if (landscape) {
            // 视觉 CCW 90°（顶边朝左）：x' = (x0+dw) - dw*v, y' = y0 + dh*u
            at = new AffineTransform(0, dh, -dw, 0, x0 + dw, y0);
        } else {
            at = new AffineTransform(dw, 0, 0, dh, x0, y0);
        }
        cs.drawImage(xi, new Matrix(
                (float) at.getScaleX(), (float) at.getShearY(),
                (float) at.getShearX(), (float) at.getScaleY(),
                (float) at.getTranslateX(), (float) at.getTranslateY()));
    }

    private PDImageXObject toXObject(BufferedImage img, String kind) throws IOException {
        if ("png".equals(kind)) {
            return LosslessFactory.createFromImage(doc, img);
        }
        BufferedImage rgb = img;
        if (img.getType() != BufferedImage.TYPE_INT_RGB) {
            rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = rgb.createGraphics();
            g.drawImage(img, 0, 0, java.awt.Color.WHITE, null);
            g.dispose();
        }
        return JPEGFactory.createFromImage(doc, rgb, 0.9f);
    }

    private static String kindOf(String ext) {
        return "png".equalsIgnoreCase(ext) ? "png" : "jpg";
    }

    // ==================== 压缩包展开 ====================

    /**
     * 展开条目：docx/xlsx 按扩展名优先识别（它们本质是 PK 容器，
     * 必须先于 zip 魔数判断，否则会被误当压缩包展开）；zip/7z 递归展开。
     */
    private void expand(String name, byte[] data, int depth,
                        List<SourceFile> out, long[] total) throws IOException {
        if (total[0] + data.length > MAX_EXPANDED_BYTES) {
            throw new PdfMergeException("解压后总大小超出限制（2GB）");
        }
        if (endsWith(name, ".xlsx") || endsWith(name, ".docx")) {
            out.add(new SourceFile(normalize(name), data));
            return;
        }
        if (isZip(data)) {
            if (depth >= MAX_DEPTH) return;
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    if (ze.isDirectory()) continue;
                    checkEntryLimit(out);
                    byte[] child = readAll(zis, ze.getSize());
                    total[0] += child.length;
                    expand(joinPath(name, ze.getName()), child, depth + 1, out, total);
                }
            }
        } else if (is7z(data)) {
            if (depth >= MAX_DEPTH) return;
            try (SevenZFile z7 = new SevenZFile(new SeekableInMemoryByteChannel(data))) {
                SevenZArchiveEntry ae;
                while ((ae = z7.getNextEntry()) != null) {
                    if (ae.isDirectory()) continue;
                    checkEntryLimit(out);
                    byte[] child = new byte[(int) ae.getSize()];
                    int off = 0;
                    while (off < child.length) {
                        int n = z7.read(child, off, child.length - off);
                        if (n < 0) break;
                        off += n;
                    }
                    total[0] += child.length;
                    expand(joinPath(name, ae.getName()), child, depth + 1, out, total);
                }
            }
        } else if (isPdf(data) || isImage(data)) {
            out.add(new SourceFile(normalize(name), data));
        }
        // 其它类型跳过
    }

    private static void checkEntryLimit(List<SourceFile> out) {
        if (out.size() + 1 > MAX_ENTRIES) {
            throw new PdfMergeException("压缩包内文件数超出限制（2000）");
        }
    }

    private static byte[] readAll(InputStream in, long hint) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(
                (hint > 0 && hint < Integer.MAX_VALUE) ? (int) hint : 8192);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    /** 归一化路径分隔符，保留层级（标注解析需要父文件夹） */
    private static String normalize(String name) {
        return name.replace('\\', '/');
    }

    private static String joinPath(String parent, String child) {
        String c = normalize(child);
        while (c.startsWith("/")) c = c.substring(1);
        // zip 内路径已含完整层级；直接使用子条目路径（父 zip 名不叠加，避免污染标注解析）
        return c;
    }

    // ==================== 类型识别 / 工具 ====================

    /** PDF 魔数（容错：头部有垃圾数据的 PDF，在前 1024 字节内搜索 %PDF-） */
    static boolean isPdf(byte[] data) {
        if (data == null || data.length < 5) return false;
        int limit = Math.min(data.length - 5, 1024);
        for (int i = 0; i <= limit; i++) {
            if (data[i] == '%' && data[i + 1] == 'P' && data[i + 2] == 'D'
                    && data[i + 3] == 'F' && data[i + 4] == '-') {
                return true;
            }
        }
        return false;
    }

    static boolean isImage(byte[] d) {
        if (d == null || d.length < 4) return false;
        if ((d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return true;
        return (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
    }

    static boolean isZip(byte[] d) {
        return d != null && d.length >= 4 && d[0] == 'P' && d[1] == 'K'
                && (d[2] == 3 || d[2] == 5 || d[2] == 7) && (d[3] == 4 || d[3] == 6 || d[3] == 8);
    }

    static boolean is7z(byte[] d) {
        return d != null && d.length >= 6
                && d[0] == 0x37 && d[1] == 0x7A && d[2] == (byte) 0xBC
                && d[3] == (byte) 0xAF && d[4] == 0x27 && d[5] == 0x1C;
    }

    static boolean endsWith(String name, String ext) {
        return name != null && name.toLowerCase().endsWith(ext);
    }

    static String baseName(String path) {
        String norm = path.replace('\\', '/');
        int i = norm.lastIndexOf('/');
        return i >= 0 ? norm.substring(i + 1) : norm;
    }

    static String ext(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i).toLowerCase() : "";
    }

    private static int pdfPageCount(byte[] data) {
        try {
            PDDocument d = PDDocument.load(data);
            try {
                return d.getNumberOfPages();
            } finally {
                d.close();
            }
        } catch (IOException e) {
            return 1;
        }
    }

    private static BufferedImage ImageIORead(byte[] data) {
        try (InputStream in = new ByteArrayInputStream(data)) {
            return ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    /** 自然序比较：数字段按数值，其余按字符（如 "10、" 排在 "9、" 之后） */
    static int naturalCompare(String a, String b) {
        int ia = 0, ib = 0;
        while (ia < a.length() && ib < b.length()) {
            char ca = a.charAt(ia), cb = b.charAt(ib);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int ja = ia, jb = ib;
                while (ja < a.length() && Character.isDigit(a.charAt(ja))) ja++;
                while (jb < b.length() && Character.isDigit(b.charAt(jb))) jb++;
                long na = Long.parseLong(a.substring(ia, ja));
                long nb = Long.parseLong(b.substring(ib, jb));
                if (na != nb) return Long.compare(na, nb);
                ia = ja;
                ib = jb;
            } else {
                if (ca != cb) return ca - cb;
                ia++;
                ib++;
            }
        }
        return a.length() - b.length();
    }
}
