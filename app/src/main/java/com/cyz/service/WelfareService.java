package com.cyz.service;

import com.cyz.pojo.Result;

/**
 * 福利专区：只读展示 dataScan 抓取的 scan_post 帖子。
 */
public interface WelfareService {

    Result list(String contextPath, String keyword, int page, int size);

    Result detail(String contextPath, Long id);
}
