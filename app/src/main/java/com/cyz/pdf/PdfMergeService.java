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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;

/**
 * 证据材料整理核心服务。
 * <p>
 * 将若干 PDF / 图片（PNG/JPG），或含这些文件的 ZIP / 7z 压缩包，按序合并为单一 A4 竖向 PDF：
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

    /** 压缩包展开上限（防止 zip bomb） */
    private static final int MAX_EXPAND_ENTRIES = 2000;
    private static final long MAX_EXPAND_BYTES = 2L * 1024 * 1024 * 1024; // 2GB
    private static final int MAX_ARCHIVE_DEPTH = 8;

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
                // 优先从文件名解析范围标注（如"(页码：61-69)"），匹配多页源时逐页生成标签
                int[] range = parseLabelRange(sf.getName());
                String baseLabel;
                if (auto) {
                    baseLabel = null;
                } else {
                    String raw = (labels != null && i < labels.size()) ? labels.get(i) : null;
                    String cleaned = sanitizeLabel(raw);
                    baseLabel = (cleaned == null || cleaned.isEmpty()) ? String.valueOf(i + 1) : cleaned;
                }

                if (isPdfByName(sf.getName()) || isPdf(data)) {
                    byte[] pdfData = normalizePdf(data);
                    PDDocument src = PDDocument.load(pdfData);
                    try {
                        int n = src.getNumberOfPages();
                        int[] pdfRange = range;
                        if (pdfRange != null && pdfRange[1] - pdfRange[0] + 1 != n) {
                            pdfRange = null; // 范围与页数不匹配，回退为单一标注
                        }
                        for (int p = 0; p < n; p++) {
                            String label;
                            if (auto) {
                                label = String.valueOf(++autoSeq);
                            } else if (pdfRange != null) {
                                label = String.valueOf(pdfRange[0] + p);
                            } else {
                                label = baseLabel;
                            }
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
                    String label = auto ? String.valueOf(++autoSeq)
                            : (range != null ? String.valueOf(range[0]) : baseLabel);
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

    /** 按扩展名识别 PDF（部分 PDF 头部有垃圾数据，魔数不在开头，需配合 isPdf 字节检测） */
    static boolean isPdfByName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".pdf");
    }

    static boolean isPdf(byte[] data) {
        if (data == null || data.length < 5) return false;
        if (data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F' && data[4] == '-') return true;
        // 有些 PDF 头部有垃圾数据（如加密包装头），搜索前 1024 字节内的 %PDF- 标记
        int limit = Math.min(data.length, 1024);
        for (int i = 1; i <= limit - 5; i++) {
            if (data[i] == '%' && data[i + 1] == 'P' && data[i + 2] == 'D'
                    && data[i + 3] == 'F' && data[i + 4] == '-') return true;
        }
        return false;
    }

    /** 规范化 PDF 字节流：若 %PDF- 不在开头，截取从 %PDF- 开始的部分（剥除头部垃圾数据） */
    static byte[] normalizePdf(byte[] data) {
        if (data == null || data.length < 5) return data;
        if (data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F' && data[4] == '-') return data;
        int limit = Math.min(data.length, 1024);
        for (int i = 1; i <= limit - 5; i++) {
            if (data[i] == '%' && data[i + 1] == 'P' && data[i + 2] == 'D'
                    && data[i + 3] == 'F' && data[i + 4] == '-') {
                byte[] out = new byte[data.length - i];
                System.arraycopy(data, i, out, 0, out.length);
                return out;
            }
        }
        return data;
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

    // ===================== 压缩包识别与展开 =====================

    /** ZIP 魔数：PK\x03\x04（普通）/ PK\x05\x06（空包）/ PK\x07\x08（分卷） */
    static boolean isZip(byte[] d) {
        return d != null && d.length >= 4
                && d[0] == 'P' && d[1] == 'K'
                && ((d[2] == 0x03 && d[3] == 0x04)
                    || (d[2] == 0x05 && d[3] == 0x06)
                    || (d[2] == 0x07 && d[3] == 0x08));
    }

    /** 7z 魔数：37 7A BC AF 27 1C（"7z¼¯'."） */
    static boolean is7z(byte[] d) {
        return d != null && d.length >= 6
                && d[0] == 0x37 && d[1] == 0x7A && d[2] == (byte) 0xBC
                && d[3] == (byte) 0xAF && d[4] == 0x27 && d[5] == 0x1C;
    }

    /** 按扩展名识别 xlsx（xlsx 本质是 zip，需在 zip 递归前拦截） */
    static boolean isXlsxByName(String name) {
        return name != null && name.toLowerCase().endsWith(".xlsx");
    }

    /**
     * 从文件名解析页码范围标注，如 "(页码：61-69)" / "（页码：5-13）" → [61,69] / [5,13]。
     * 范围长度上限 200，防止异常输入。无范围返回 null。
     */
    static int[] parseLabelRange(String name) {
        if (name == null) return null;
        String base = name;
        int s = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (s >= 0) base = name.substring(s + 1);
        Pattern p = Pattern.compile("页码[:：]?\\s*(\\d+)\\s*[-—–]\\s*(\\d+)");
        Matcher m = p.matcher(base);
        if (m.find()) {
            try {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                if (b >= a && b - a + 1 <= 200) return new int[]{a, b};
            } catch (NumberFormatException ignore) {
            }
        }
        return null;
    }

    /**
     * 从文件名（可含路径）提取页码标注，与前端 parseLabel 保持一致：
     * 优先 "页码101" / "（101）" / "(101)"，否则取首个数字串；无则返回 ""。
     */
    static String parseLabelFromName(String name) {
        if (name == null) return "";
        String base = name;
        int s = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (s >= 0) base = name.substring(s + 1);
        String m1 = findGroup(base, "页码[:：]?\\s*(\\d+)");
        if (m1 != null) return m1;
        String m2 = findGroup(base, "[（(]\\s*(\\d+)\\s*[）)]");
        if (m2 != null) return m2;
        String m3 = findGroup(base, "(\\d+)");
        return m3 == null ? "" : m3;
    }

    private static String findGroup(String s, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    /** 从文件名提取页码数字作为排序键（无数字返回 MAX_VALUE 排末尾） */
    static int labelAsInt(String name) {
        String lbl = parseLabelFromName(name);
        if (lbl == null || lbl.isEmpty()) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(lbl);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * 自然序比较：数字段按数值比（"10" 排在 "9" 之后），非数字段按字符比。
     * 用于压缩包内文件名排序，确保 "1、xxx" < "2、xxx" < ... < "10、xxx"。
     */
    static int naturalCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        int ia = 0, ib = 0;
        int la = a.length(), lb = b.length();
        while (ia < la && ib < lb) {
            char ca = a.charAt(ia), cb = b.charAt(ib);
            boolean da = ca >= '0' && ca <= '9';
            boolean db = cb >= '0' && cb <= '9';
            if (da && db) {
                int ja = ia, jb = ib;
                while (ja < la && a.charAt(ja) >= '0' && a.charAt(ja) <= '9') ja++;
                while (jb < lb && b.charAt(jb) >= '0' && b.charAt(jb) <= '9') jb++;
                long na = 0, nb = 0;
                try {
                    na = Long.parseLong(a.substring(ia, ja));
                    nb = Long.parseLong(b.substring(ib, jb));
                } catch (NumberFormatException e) {
                    return a.substring(ia).compareTo(b.substring(ib));
                }
                if (na != nb) return na < nb ? -1 : 1;
                if (ja - ia != jb - ib) return (ja - ia) - (jb - ib);
                ia = ja;
                ib = jb;
            } else {
                if (ca != cb) return ca - cb;
                ia++;
                ib++;
            }
        }
        return (la - ia) - (lb - ib);
    }

    /**
     * 展开压缩包（ZIP / 7z）为可直接合并的源文件列表。
     * <ul>
     *   <li>条目按完整名称排序，保证合并顺序确定；</li>
     *   <li>递归展开嵌套压缩包（深度上限 {@link #MAX_ARCHIVE_DEPTH}）；</li>
     *   <li>仅 PDF / 图片进入结果，压缩包递归处理，其余类型（docx/txt 等）直接跳过；</li>
     *   <li>内置条目数 / 解压总大小 / 单文件大小 / 嵌套深度防护，防止 zip bomb。</li>
     * </ul>
     * 返回的 SourceFile 顺序即合并顺序；页码标注由调用方按名称解析。
     */
    static List<SourceFile> expandArchive(byte[] data, String archiveName) throws IOException {
        return expandArchive(data, archiveName, 0);
    }

    private static List<SourceFile> expandArchive(byte[] data, String archiveName, int depth) throws IOException {
        if (depth > MAX_ARCHIVE_DEPTH) {
            throw new PdfMergeException("压缩包嵌套过深（超过 " + MAX_ARCHIVE_DEPTH + " 层）：" + archiveName);
        }
        List<ArchiveItem> items = new ArrayList<>();
        if (isZip(data)) {
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    if (e.isDirectory()) continue;
                    byte[] content = readAll(zis, e.getSize());
                    items.add(new ArchiveItem(e.getName(), content));
                }
            }
        } else if (is7z(data)) {
            try (SevenZFile szf = new SevenZFile(new SeekableInMemoryByteChannel(data))) {
                SevenZArchiveEntry e;
                while ((e = szf.getNextEntry()) != null) {
                    if (e.isDirectory()) continue;
                    long size = e.getSize();
                    if (size > Integer.MAX_VALUE - 8) {
                        throw new PdfMergeException("压缩包内单个文件过大：" + e.getName());
                    }
                    byte[] content = new byte[(int) size];
                    int off = 0, r;
                    while (off < content.length && (r = szf.read(content, off, content.length - off)) > 0) {
                        off += r;
                    }
                    items.add(new ArchiveItem(e.getName(), content));
                }
            }
        } else {
            throw new PdfMergeException("无法识别的压缩包格式：" + archiveName);
        }

        Collections.sort(items, new Comparator<ArchiveItem>() {
            public int compare(ArchiveItem a, ArchiveItem b) {
                // 优先按文件名内的页码数字排序（与 v5 顺序一致），相同则按路径自然序
                int la = labelAsInt(a.name);
                int lb = labelAsInt(b.name);
                if (la != lb) return Integer.compare(la, lb);
                return naturalCompare(a.name, b.name);
            }
        });

        List<SourceFile> out = new ArrayList<>();
        long total = 0;
        for (ArchiveItem it : items) {
            if (out.size() >= MAX_EXPAND_ENTRIES) {
                throw new PdfMergeException("压缩包内文件过多（超过 " + MAX_EXPAND_ENTRIES + " 个），请分批处理：" + archiveName);
            }
            total += it.data.length;
            if (total > MAX_EXPAND_BYTES) {
                throw new PdfMergeException("压缩包解压后过大（超过 2GB）：" + archiveName);
            }
            if (isXlsxByName(it.name)) {
                try {
                    out.addAll(expandXlsx(it.data, it.name));
                } catch (PdfMergeException pe) {
                    throw pe;
                } catch (Exception e) {
                    throw new PdfMergeException("xlsx 解析失败：" + it.name + "（" + e.getMessage() + "）");
                }
            } else if (isPdfByName(it.name) || isPdf(it.data) || isImage(it.data)) {
                out.add(new SourceFile(it.name, it.data));
            } else if (isZip(it.data) || is7z(it.data)) {
                out.addAll(expandArchive(it.data, it.name, depth + 1));
            }
            // 其它类型（docx/txt 等）跳过
        }
        return out;
    }

    /**
     * 提取 xlsx 中的嵌入图片（POI XSSFWorkbook），按 drawing 锚点 (sheet, col1, row1) 排序。
     * 每张图作为一个 SourceFile，文件名注入 "(页码：N)" 让 parseLabelFromName 提取标签；
     * 若 xlsx 文件名含范围（如"页码：5-13"），按图序从范围起点递增标注。
     */
    static List<SourceFile> expandXlsx(byte[] data, String name) throws IOException {
        List<SourceFile> out = new ArrayList<>();
        List<int[]> orderIdx = new ArrayList<>(); // [col1, row1, imgIdx]
        List<byte[]> imgBytes = new ArrayList<>();
        List<String> imgExt = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            int imgIdx = 0;
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                XSSFSheet sheet = wb.getSheetAt(si);
                XSSFDrawing drawing = sheet.getDrawingPatriarch();
                if (drawing == null) continue;
                for (XSSFShape shape : drawing.getShapes()) {
                    if (!(shape instanceof XSSFPicture)) continue;
                    XSSFPicture pic = (XSSFPicture) shape;
                    XSSFPictureData pd = pic.getPictureData();
                    if (pd == null) continue;
                    int col1 = 0, row1 = 0;
                    if (pic.getAnchor() instanceof XSSFClientAnchor) {
                        XSSFClientAnchor a = (XSSFClientAnchor) pic.getAnchor();
                        col1 = a.getCol1();
                        row1 = a.getRow1();
                    }
                    // 加入 sheet 序号到 col1 高位以便跨 sheet 排序
                    orderIdx.add(new int[]{si * 100000 + col1, row1, imgIdx});
                    imgBytes.add(pd.getData());
                    imgExt.add(pd.suggestFileExtension());
                    imgIdx++;
                }
            }
        }
        Collections.sort(orderIdx, new Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                if (a[0] != b[0]) return a[0] - b[0];
                return a[1] - b[1];
            }
        });
        int[] range = parseLabelRange(name);
        int seq = 0;
        for (int[] o : orderIdx) {
            int idx = o[2];
            String labelNum;
            if (range != null) {
                int n = range[0] + seq;
                if (n > range[1]) n = range[1]; // 超出范围上限则重复末位（模拟 v5 多图共享标注）
                labelNum = String.valueOf(n);
            } else {
                labelNum = String.valueOf(seq + 1);
            }
            // subName 只含单个页码标注（不含原始范围），避免 parseLabelRange 重复匹配
            String subName = "xlsx_img" + (seq + 1) + "(页码：" + labelNum + ")." + imgExt.get(idx);
            out.add(new SourceFile(subName, imgBytes.get(idx)));
            seq++;
        }
        return out;
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

    private static class ArchiveItem {
        final String name;
        final byte[] data;
        ArchiveItem(String name, byte[] data) {
            this.name = name;
            this.data = data;
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
