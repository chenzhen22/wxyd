package com.cyz.archive.controller;

import com.cyz.archive.model.ArchiveBundle;
import com.cyz.archive.model.ArchiveError;
import com.cyz.archive.model.ArchiveException;
import com.cyz.archive.model.ArchiveFetchRequest;
import com.cyz.archive.service.ArchiveFetchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 报文归档下载控制器。
 * <p>
 * 路径前缀 archive（wxyd context-path 为 /api，完整路径 /api/archive/fetch）。
 * 不实现 CommController，避免被 MockAspect 兜底拦截；鉴权由 AuthInterceptor 的 /archive/** 作用域保障。
 * <p>
 * 成功时直接返回 7z 二进制附件，解析信息放在 X-Archive-* 响应头里；
 * 失败时返回 JSON（前端按 Content-Type 区分）。这样一次请求即可完成「抓取 → 解析 → 下载」。
 */
@RestController
@RequestMapping("archive")
@Slf4j
public class ArchiveController {

    private final ArchiveFetchService fetchService;

    @Autowired
    public ArchiveController(ArchiveFetchService fetchService) {
        this.fetchService = fetchService;
    }

    @PostMapping("fetch")
    public ResponseEntity<?> fetch(@RequestBody(required = false) ArchiveFetchRequest req) {
        try {
            ArchiveBundle bundle = fetchService.fetch(req);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + asciiFallback(bundle.getFileName())
                            + "\"; filename*=UTF-8''" + urlEncode(bundle.getFileName()));
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(bundle.getData().length));

            // 解析明细全部走响应头，值一律做 URL 编码以保证 ASCII 安全
            put(headers, "X-Archive-Name", bundle.getFileName());
            headers.set("X-Archive-Size", String.valueOf(bundle.getDecodedSize()));
            headers.set("X-Archive-B64-Len", String.valueOf(bundle.getBase64Length()));
            headers.set("X-Archive-Html-Len", String.valueOf(bundle.getHtmlBytes()));
            headers.set("X-Archive-Elapsed-Ms", String.valueOf(bundle.getElapsedMs()));
            headers.set("X-Archive-Is7z", String.valueOf(bundle.isSevenZ()));
            headers.set("X-Archive-7z-Consistent", String.valueOf(bundle.isSevenZConsistent()));
            headers.set("X-Archive-7z-Version",
                    bundle.getSevenZVersion() == null ? "" : bundle.getSevenZVersion());
            headers.set("X-Archive-Sha256", bundle.getSha256() == null ? "" : bundle.getSha256());
            put(headers, "X-Archive-Source", bundle.getSourceUrl());
            put(headers, "X-Archive-Element-Id", bundle.getElementId());
            headers.set("X-Archive-Element-Tag",
                    bundle.getElementTag() == null ? "" : bundle.getElementTag());

            return ResponseEntity.ok().headers(headers).body(bundle.getData());

        } catch (ArchiveException e) {
            log.warn("归档抓取失败 [{}] url={} : {}", e.getStage(),
                    req == null ? null : req.getUrl(), e.getMessage());
            ArchiveError error = ArchiveError.of(e.getStage(), e.getMessage());
            if (req != null) {
                error.setSourceUrl(req.getUrl());
                error.setElementId(req.getElementId());
            }
            return ResponseEntity.status(e.getHttpStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);

        } catch (Exception e) {
            log.error("归档抓取异常 url={}", req == null ? null : req.getUrl(), e);
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ArchiveError.of("未知阶段",
                            e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    private void put(HttpHeaders headers, String name, String value) {
        headers.set(name, urlEncode(value == null ? "" : value));
    }

    /** Content-Disposition 的 ASCII 回退名：非 ASCII 与引号统一替换，避免头注入 */
    private String asciiFallback(String name) {
        if (name == null || name.isEmpty()) {
            return "archive.7z";
        }
        String s = name.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_").replace("\\", "_");
        return s.isEmpty() ? "archive.7z" : s;
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
