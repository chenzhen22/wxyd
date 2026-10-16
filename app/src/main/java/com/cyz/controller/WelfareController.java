package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.service.WelfareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 福利专区（只读 dataScan 抓取的 scan_post 帖子）。
 * 路径前缀 welfare（context-path /api），鉴权由 AuthInterceptor 的 /welfare/** 保障。
 */
@RestController
@Slf4j
public class WelfareController implements CommController {

    @Autowired
    WelfareService welfareService;

    /** 分页列表：[{id,title,summary,firstImage,publishedAt}]；keyword 可选，模糊匹配标题/正文 */
    @GetMapping("welfare/list")
    public Result list(HttpServletRequest request,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size) {
        return welfareService.list(request.getContextPath(), keyword, page, size);
    }

    /** 详情：{id,title,publishedAt,contentMd,images} */
    @GetMapping("welfare/detail")
    public Result detail(HttpServletRequest request, @RequestParam Long id) {
        return welfareService.detail(request.getContextPath(), id);
    }
}
