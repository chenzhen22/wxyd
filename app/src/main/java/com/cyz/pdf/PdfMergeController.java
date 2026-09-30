package com.cyz.pdf;

import com.cyz.controller.CommController;
import com.cyz.pojo.Result;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 证据材料整理控制器（路径前缀 pdf，context-path /api，完整路径 /api/pdf/merge）。
 * <p>
 * 鉴权由 AuthInterceptor 统一保障（需登录）；本控制器仅做业务参数校验与合并调用。
 * 上传文件与页码标注（meta）由前端一次性提交，服务端在内存中生成最终 PDF 后流式返回。
 */
@RestController
@RequestMapping("pdf")
@Slf4j
public class PdfMergeController implements CommController {

    private static final int MAX_FILES = 500;
    private static final long MAX_TOTAL_BYTES = 500L * 1024 * 1024; // 与 multipart 上限对齐（单文件上限由容器控制）
    private static final String DOWNLOAD_NAME = "证据材料（A4竖版·带页码）.pdf";

    /**
     * 合并证据材料为单一 A4 竖向带页码 PDF。
     * <p>
     * 请求（multipart/form-data）：<br>
     * - files: 多个 PDF / PNG / JPG 文件（顺序即输出顺序）<br>
     * - meta: JSON 字符串 { "mode": "auto"|其他, "labels": ["101","102",...] }
     *
     * @return 成功时直接返回 application/pdf 字节流；失败时返回 JSON 错误
     */
    @PostMapping("merge")
    public void merge(@RequestParam("files") MultipartFile[] files,
                      @RequestParam(value = "meta", required = false) String meta,
                      HttpServletResponse response) {
        if (files == null || files.length == 0) {
            writeError(response, "000003", "未选择文件");
            return;
        }
        if (files.length > MAX_FILES) {
            writeError(response, "000003", "文件数量过多（最多 " + MAX_FILES + " 个）");
            return;
        }

        JSONObject metaObj = (meta == null || meta.trim().isEmpty()) ? new JSONObject() : JSONObject.parseObject(meta);
        String mode = metaObj.getString("mode");
        List<String> labels = new ArrayList<>();
        JSONArray arr = metaObj.getJSONArray("labels");
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                labels.add(arr.getString(i));
            }
        }

        List<PdfMergeService.SourceFile> sources = new ArrayList<>(files.length);
        long total = 0;
        List<String> rejected = new ArrayList<>();
        for (MultipartFile mf : files) {
            if (mf == null || mf.isEmpty()) {
                continue;
            }
            String original = mf.getOriginalFilename();
            String fileName = (original == null) ? "未命名" : new java.io.File(original).getName();
            byte[] data;
            try {
                data = mf.getBytes();
            } catch (IOException e) {
                writeError(response, "000003", "读取文件失败：" + fileName);
                return;
            }
            if (data.length == 0) {
                continue;
            }
            total += data.length;
            if (total > MAX_TOTAL_BYTES) {
                writeError(response, "000003", "合并后总大小超出限制（500MB）");
                return;
            }
            if (!PdfMergeService.isPdf(data) && !PdfMergeService.isImage(data)) {
                rejected.add(fileName);
                continue;
            }
            sources.add(new PdfMergeService.SourceFile(fileName, data));
        }

        if (!rejected.isEmpty()) {
            writeError(response, "000003",
                    "存在不支持的文件类型（仅支持 PDF / PNG / JPG），已忽略：" + String.join("、", rejected));
            return;
        }
        if (sources.isEmpty()) {
            writeError(response, "000003", "没有可合并的有效文件（需 PDF / PNG / JPG）");
            return;
        }

        try {
            byte[] pdf = new PdfMergeService().merge(sources, mode, labels);
            response.setContentType("application/pdf");
            response.setContentLengthLong(pdf.length);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"evidence.pdf\"; filename*=UTF-8''"
                            + URLEncoder.encode(DOWNLOAD_NAME, StandardCharsets.UTF_8.name()).replace("+", "%20"));
            response.getOutputStream().write(pdf);
            response.getOutputStream().flush();
            log.info("证据材料合并完成：{} 个源文件 → {} KB", sources.size(), pdf.length / 1024);
        } catch (PdfMergeService.PdfMergeException e) {
            log.warn("证据材料合并业务异常：{}", e.getMessage());
            writeError(response, "000003", e.getMessage());
        } catch (Exception e) {
            log.error("证据材料合并失败", e);
            writeError(response, "999999", "处理失败：" + e.getMessage());
        }
    }

    /** 以 JSON 形式返回错误（客户端通过 content-type 区分成功/失败） */
    private void writeError(HttpServletResponse response, String code, String msg) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            String safe = msg == null ? "" : msg.replace("\\", "\\\\").replace("\"", "'");
            String body = "{\"errorCode\":\"" + code + "\",\"errorMsg\":\"" + safe + "\"}";
            response.getWriter().write(body);
        } catch (IOException ignore) {
            // 响应已无法写入，忽略
        }
    }
}
