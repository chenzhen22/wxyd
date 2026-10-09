package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.service.WelfareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 福利专区（只读 dataScan 抓取的 scan_post 帖子）。
 * 路径前缀 welfare（context-path /api），鉴权由 AuthInterceptor 的 /welfare/** 保障。
 */
@RestController
@Slf4j
public class WelfareController implements CommController {

    @Autowired
    WelfareService welfareService;

    /** 分页列表：[{id,title,summary,firstImage,publishedAt}] */
    @GetMapping("welfare/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size) {
        return welfareService.list(page, size);
    }

    /** 详情：{id,title,publishedAt,contentMd,images} */
    @GetMapping("welfare/detail")
    public Result detail(@RequestParam Long id) {
        return welfareService.detail(id);
    }
}
