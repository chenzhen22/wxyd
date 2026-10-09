package com.cyz.service;

import com.cyz.constant.ErrorEnum;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.Result;
import com.cyz.pojo.ScanPost;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 福利专区服务。
 * <p>
 * 数据来自 dataScan 抓取入库的 scan_post（同库只读），纯展示：不返回作者/
 * 点赞浏览数/原站信息等元数据。图片仅暴露 dataScan 已下载到本地的文件，
 * 通过 /welfare-img/** 静态映射访问；无本地文件的图不返回（不暴露原图 URL）。
 */
@Service
@Slf4j
public class WelfareServiceImpl implements WelfareService {

    /** dataScan 图片落地目录（storage.py 按 topic_id 建子目录），可被 env.properties 覆盖 */
    @Value("${wxyd.welfare.image-dir:/apps/shareFile/datascan}")
    private String imageDir;

    @Autowired
    MysqlMapper mysqlMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result list(String contextPath, int page, int size) {
        Result result = Result.getInstance();
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 20;
        List<ScanPost> posts = mysqlMapper.listWelfarePosts((page - 1) * size, size);
        List<Map<String, Object>> body = new ArrayList<>();
        if (posts != null) {
            for (ScanPost p : posts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", p.getId());
                row.put("title", p.getTitle());
                row.put("summary", p.getSummary());
                row.put("publishedAt", p.getPublishedAt());
                row.put("firstImage", firstLocalImage(contextPath, p.getImages()));
                body.add(row);
            }
        }
        result.setBody(body);
        return result;
    }

    @Override
    public Result detail(String contextPath, Long id) {
        Result result = Result.getInstance();
        if (id == null || id < 1) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        ScanPost p = mysqlMapper.queryWelfarePost(id);
        if (p == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000022);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", p.getId());
        body.put("title", p.getTitle());
        body.put("publishedAt", p.getPublishedAt());
        body.put("contentMd", p.getContentMd());
        body.put("images", localImages(contextPath, p.getImages()));
        result.setBody(body);
        return result;
    }

    /** 解析 images JSON，返回 [{url,webUrl,alt,width,height}]，仅含已落地本地的图 */
    private List<Map<String, Object>> localImages(String contextPath, String imagesJson) {
        List<Map<String, Object>> out = new ArrayList<>();
        String root = imageDir == null || imageDir.trim().isEmpty()
                ? "/apps/shareFile/datascan" : imageDir.trim();
        if (!root.endsWith(File.separator)) root = root + File.separator;
        // webUrl 必须带 context-path（应用部署在 /api 下），否则浏览器 404
        String base = contextPath == null ? "" : contextPath;
        for (Map<String, Object> img : parseImages(imagesJson)) {
            Object lp = img.get("local_path");
            if (lp == null) continue;
            String localPath = String.valueOf(lp);
            if (!localPath.startsWith(root)) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            // url 仅用于前端把 Markdown 里的远程图片地址替换成本地代理地址，页面不展示
            row.put("url", img.get("url"));
            row.put("webUrl", base + "/welfare-img/" + localPath.substring(root.length()));
            Object alt = img.get("alt");
            if (alt != null) row.put("alt", alt);
            Object width = img.get("width");
            if (width != null) row.put("width", width);
            Object height = img.get("height");
            if (height != null) row.put("height", height);
            out.add(row);
        }
        return out;
    }

    /** 列表首图：第一张已落地本地的图的代理地址 */
    private String firstLocalImage(String contextPath, String imagesJson) {
        for (Map<String, Object> img : localImages(contextPath, imagesJson)) {
            return String.valueOf(img.get("webUrl"));
        }
        return null;
    }

    private List<Map<String, Object>> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("scan_post.images JSON 解析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
