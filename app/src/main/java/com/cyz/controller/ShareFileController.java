package com.cyz.controller;

import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;

/**
 * 文件共享控制器。
 * <p>
 * 路径前缀 share（wxyd context-path 为 /api，完整路径 /api/share/**）。
 * 不实现 CommController，鉴权由 AuthInterceptor 的 /share/** 作用域保障（必须登录）。
 * 上传与删除仅限管理员（session role == 0）；列表与下载所有已登录用户可用。
 * 文件统一存放在 {@code wxyd.share.dir}（默认 /apps/shareFile/），扁平存储，按文件名访问。
 */
@RestController
@RequestMapping("share")
@Slf4j
public class ShareFileController {

    @Value("${wxyd.share.dir:/apps/shareFile}")
    private String shareDir;

    private static final long MAX_FILE_SIZE = 2000L * 1024 * 1024; // 与 spring.servlet.multipart 上限对齐

    /** 返回（按需创建）共享根目录 */
    private File rootDir() {
        File dir = new File(shareDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("共享目录创建失败：{}", dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * 根据文件名解析出根目录下的目标文件，并做路径穿越防护。
     * 拒绝包含 / \ .. 的名称，且要求规范路径必须位于根目录之内。
     */
    private File resolve(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            return null;
        }
        try {
            File root = rootDir().getCanonicalFile();
            File target = new File(root, name).getCanonicalFile();
            if (!target.getPath().startsWith(root.getPath() + File.separator)
                    && !target.getPath().equals(root.getPath())) {
                return null;
            }
            return target;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isAdmin(HttpSession session) {
        Integer role = (Integer) session.getAttribute("role");
        return role != null && role == 0;
    }

    /** 列出共享目录下的文件（所有已登录用户可用） */
    @GetMapping("list")
    public Result list() {
        Result result = Result.getInstance();
        File dir = rootDir();
        File[] files = dir.listFiles();
        List<Map<String, Object>> list = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (!f.isFile()) {
                    continue; // 仅展示普通文件
                }
                Map<String, Object> m = new HashMap<>(4);
                m.put("name", f.getName());
                m.put("size", f.length());
                m.put("lastModified", f.lastModified());
                list.add(m);
            }
            list.sort((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")));
        }
        result.setBody(list);
        return result;
    }

    /** 上传文件（仅管理员）。支持多文件、拖拽上传。 */
    @PostMapping("upload")
    public Result upload(@RequestParam("files") MultipartFile[] files, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            result.setErrorCode("000003");
            result.setErrorMsg("仅管理员可上传文件");
            return result;
        }
        if (files == null || files.length == 0) {
            result.setErrorCode("000003");
            result.setErrorMsg("未选择文件");
            return result;
        }
        File dir = rootDir();
        List<String> saved = new ArrayList<>();
        for (MultipartFile mf : files) {
            if (mf == null || mf.isEmpty()) {
                continue;
            }
            String original = mf.getOriginalFilename();
            if (original == null || original.trim().isEmpty()) {
                continue;
            }
            // 仅取文件名部分，去除路径信息，防止穿越
            String fileName = new File(original).getName();
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                continue;
            }
            if (mf.getSize() > MAX_FILE_SIZE) {
                result.setErrorCode("000003");
                result.setErrorMsg("文件超出大小限制：" + fileName);
                return result;
            }
            try {
                File target = new File(dir, fileName);
                mf.transferTo(target);
                saved.add(fileName);
                log.info("共享文件上传成功：{} ({} bytes)", target.getAbsolutePath(), mf.getSize());
            } catch (IOException e) {
                log.error("共享文件上传失败：{}", fileName, e);
                result.setErrorCode("000003");
                result.setErrorMsg("上传失败：" + fileName);
                return result;
            }
        }
        result.setBody(saved);
        return result;
    }

    // ===================== 分片上传（大文件，绕过 Cloudflare 100MB 边缘限制）=====================

    private boolean validUploadId(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]{1,64}");
    }

    private File chunkDir(String uploadId) {
        return new File(rootDir(), ".chunks" + File.separator + uploadId);
    }

    /** 收集某次分片中已落地的分片序号（用于断点续传与合并校验） */
    private List<Integer> receivedIndexes(File dir, int total) {
        List<Integer> list = new ArrayList<>();
        File[] parts = dir.listFiles();
        if (parts != null) {
            for (File p : parts) {
                if (!p.isFile()) continue;
                try {
                    int idx = Integer.parseInt(p.getName());
                    if (idx >= 0 && idx < total) list.add(idx);
                } catch (NumberFormatException ignore) {
                }
            }
        }
        Collections.sort(list);
        return list;
    }

    /** 文件名清洗：去除路径、拒绝穿越；返回安全文件名或 null */
    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String f = new File(name).getName();
        if (f.contains("..") || f.contains("/") || f.contains("\\")) return null;
        return f.isEmpty() ? null : f;
    }

    private void deleteRecursively(File f) {
        if (f == null) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursively(c);
        }
        f.delete();
    }

    /** 上传单个分片（仅管理员）。分片落地到 .chunks/<uploadId>/<index>，上传完成后调用 merge 合并。 */
    @PostMapping("uploadChunk")
    public Result uploadChunk(@RequestParam("uploadId") String uploadId,
                             @RequestParam("fileName") String fileName,
                             @RequestParam("index") int index,
                             @RequestParam("total") int total,
                             @RequestParam("chunk") MultipartFile chunk,
                             HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            result.setErrorCode("000003");
            result.setErrorMsg("仅管理员可上传文件");
            return result;
        }
        if (!validUploadId(uploadId)) {
            result.setErrorCode("000003");
            result.setErrorMsg("非法 uploadId");
            return result;
        }
        String safeName = sanitizeFileName(fileName);
        if (safeName == null) {
            result.setErrorCode("000003");
            result.setErrorMsg("非法文件名");
            return result;
        }
        if (chunk == null || chunk.isEmpty()) {
            result.setErrorCode("000003");
            result.setErrorMsg("分片为空");
            return result;
        }
        if (total <= 0 || index < 0 || index >= total) {
            result.setErrorCode("000003");
            result.setErrorMsg("分片序号/总数非法");
            return result;
        }
        if (chunk.getSize() > MAX_FILE_SIZE) {
            result.setErrorCode("000003");
            result.setErrorMsg("单个分片过大");
            return result;
        }
        File dir = chunkDir(uploadId);
        if (!dir.exists() && !dir.mkdirs()) {
            result.setErrorCode("000003");
            result.setErrorMsg("分片目录创建失败");
            return result;
        }
        try {
            File part = new File(dir, String.valueOf(index));
            chunk.transferTo(part);
            Map<String, Object> body = new HashMap<>(4);
            body.put("uploadId", uploadId);
            body.put("fileName", safeName);
            body.put("received", receivedIndexes(dir, total));
            result.setBody(body);
        } catch (IOException e) {
            log.error("分片上传失败 uploadId={} index={}", uploadId, index, e);
            result.setErrorCode("000003");
            result.setErrorMsg("分片上传失败：" + index);
        }
        return result;
    }

    /** 查询已接收分片（用于断点续传），返回 0..total-1 中已存在的序号。 */
    @GetMapping("chunkStatus")
    public Result chunkStatus(@RequestParam("uploadId") String uploadId,
                             @RequestParam("total") int total) {
        Result result = Result.getInstance();
        if (!validUploadId(uploadId)) {
            result.setErrorCode("000003");
            result.setErrorMsg("非法 uploadId");
            return result;
        }
        File dir = chunkDir(uploadId);
        Map<String, Object> body = new HashMap<>(2);
        body.put("uploadId", uploadId);
        body.put("received", dir.exists() ? receivedIndexes(dir, total) : new ArrayList<Integer>());
        result.setBody(body);
        return result;
    }

    /** 合并分片为最终文件（仅管理员）。校验分片齐全后顺序拼接，并清理临时分片目录。 */
    @PostMapping("merge")
    public Result merge(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            result.setErrorCode("000003");
            result.setErrorMsg("仅管理员可上传文件");
            return result;
        }
        Object body = req == null ? null : req.getBody();
        String uploadId = null, fileName = null;
        Integer total = null;
        if (body instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) body;
            Object v1 = m.get("uploadId");
            uploadId = v1 == null ? null : String.valueOf(v1);
            Object v2 = m.get("fileName");
            fileName = v2 == null ? null : String.valueOf(v2);
            Object v3 = m.get("total");
            total = v3 == null ? null : ((Number) v3).intValue();
        }
        if (!validUploadId(uploadId)) {
            result.setErrorCode("000003");
            result.setErrorMsg("非法 uploadId");
            return result;
        }
        String safeName = sanitizeFileName(fileName);
        if (safeName == null || total == null || total <= 0) {
            result.setErrorCode("000003");
            result.setErrorMsg("参数非法");
            return result;
        }
        File dir = chunkDir(uploadId);
        if (!dir.exists() || !dir.isDirectory()) {
            result.setErrorCode("000003");
            result.setErrorMsg("分片不存在或已合并");
            return result;
        }
        List<Integer> received = receivedIndexes(dir, total);
        if (received.size() != total) {
            result.setErrorCode("000003");
            result.setErrorMsg("分片缺失，无法合并（已收 " + received.size() + "/" + total + "）");
            return result;
        }
        File target = new File(rootDir(), safeName);
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target.toPath()))) {
            byte[] buf = new byte[64 * 1024];
            for (int i = 0; i < total; i++) {
                File part = new File(dir, String.valueOf(i));
                try (InputStream in = new BufferedInputStream(Files.newInputStream(part.toPath()))) {
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }
            }
        } catch (IOException e) {
            log.error("分片合并失败 uploadId={} name={}", uploadId, safeName, e);
            result.setErrorCode("000003");
            result.setErrorMsg("合并失败：" + safeName);
            return result;
        }
        deleteRecursively(dir);
        log.info("分片合并完成：{} ({} 分片, {} bytes)", target.getAbsolutePath(), total, target.length());
        result.setBody(safeName);
        return result;
    }

    /** 下载文件（所有已登录用户可用）。直接写入 HttpServletResponse，兼容大文件流式下载。 */
    @GetMapping("download")
    public void download(@RequestParam("name") String name, HttpServletResponse response) {
        File target = resolve(name);
        if (target == null || !target.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"errorCode\":\"000404\",\"errorMsg\":\"文件不存在\"}");
            } catch (IOException ignore) {
            }
            return;
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(target.length());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + asciiFallback(target.getName())
                        + "\"; filename*=UTF-8''" + urlEncode(target.getName()));
        try (InputStream in = new BufferedInputStream(Files.newInputStream(target.toPath()));
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (IOException e) {
            log.error("共享文件下载失败：{}", name, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /** 删除文件（仅管理员） */
    @PostMapping("delete")
    public Result delete(@RequestBody(required = false) Result req, HttpSession session) {
        Result result = Result.getInstance();
        if (!isAdmin(session)) {
            result.setErrorCode("000003");
            result.setErrorMsg("仅管理员可删除文件");
            return result;
        }
        Object body = req == null ? null : req.getBody();
        String name = null;
        if (body instanceof Map) {
            Object v = ((Map<?, ?>) body).get("name");
            name = v == null ? null : String.valueOf(v);
        }
        File target = resolve(name);
        if (target == null || !target.isFile()) {
            result.setErrorCode("000003");
            result.setErrorMsg("文件不存在");
            return result;
        }
        if (!target.delete()) {
            result.setErrorCode("000003");
            result.setErrorMsg("删除失败");
            return result;
        }
        log.info("共享文件删除成功：{}", target.getAbsolutePath());
        return result;
    }

    private String asciiFallback(String name) {
        if (name == null || name.isEmpty()) {
            return "download";
        }
        String s = name.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_").replace("\\", "_");
        return s.isEmpty() ? "download" : s;
    }

    private String urlEncode(String s) {
        if (s == null) {
            return "";
        }
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
