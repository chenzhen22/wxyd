package com.cyz.wbgateway.provider;

import java.util.List;

/** 模型目录的通用视图（各上游 provider 各自实现）。 */
public interface ModelCatalog {

    /** 全部可选模型 id（对外展示/请求使用） */
    List<String> ids();

    /** 免费（模型开关关闭时静默映射用）模型 id */
    List<String> freeIds();

    /** 精确/大小写不敏感命中，返回规范 id；未命中返回 null */
    String match(String requested);

    /** live=实时拉取 fallback=内置兜底 */
    String source();
}
