package com.cyz.note.model;

import java.util.List;

/**
 * 记忆笔记，对应 /apps/shareFile/{userId}/ 目录下一个 .md 文件。
 * 元数据内嵌于 md 的 YAML frontmatter（name/category/visibility/tags/source），
 * 正文为完整 markdown。
 */
public class Note {

    private String name;
    private String category;
    private String content;
    /** public / private（缺省 public，兼容旧笔记） */
    private String visibility;
    /** 属主用户 id（列表/搜索场景填充） */
    private Long ownerId;
    /** 属主展示名（公共搜索场景填充） */
    private String ownerName;
    /** 文件最后更新时间（yyyy-MM-dd HH:mm） */
    private String updateTime;
    /** 列表预览（正文截 150 字） */
    private String preview;

    /* ---------- v2：个人知识库新增字段（全部可选，老笔记零迁移） ---------- */

    /** 标签列表（frontmatter tags，逗号分隔存储；可为空） */
    private List<String> tags;
    /**
     * 来源条目 key（frontmatter source）：
     * javaapi/{name} | shell/{name} | dubbo/{configId}；空=非摘录笔记。
     * 用于笔记 → 资料页正向跳转。
     */
    private String sourceKey;
    /** 来源展示标题（搜索/列表场景填充，用于渲染「参考：xxx」跳转链接） */
    private String sourceTitle;
    /** 来源分类（资料页 category，用于摘录笔记的默认分类） */
    private String sourceCategory;

    /* ---------- 搜索命中标记（由检索层填充，不落库） ---------- */

    /** 标题命中关键词 */
    private Boolean titleMatched;
    /** 正文命中关键词（他人笔记恒为 false，因正文不入库 —— Q1 决策） */
    private Boolean contentMatched;
    /**
     * 该结果的正文是否已入库（可做内容命中检索）。
     * false 表示来自他人笔记（Q1 决策：他人 public 仅索引元数据），
     * 前端应提示「仅按标题匹配」，避免用户误以为功能失效。
     */
    private Boolean contentSearchable;

    public Note() {}

    public Note(String name, String category, String content) {
        this.name = name;
        this.category = category;
        this.content = content;
    }

    /** 用正文生成纯文本预览（截 150 字） */
    public void setPreview() {
        String plain = content == null ? "" : content.replaceAll("<[^>]+>", "").trim();
        this.preview = plain.length() > 150 ? plain.substring(0, 150) + "..." : plain;
    }

    /**
     * 内容命中时生成以关键词为中心的上下文片段（±40 字），
     * 比「正文开头 150 字」更能让用户确认是不是他要找的那篇。
     * 未命中则退化为常规预览。
     */
    public void setPreviewAround(String keyword) {
        if (content == null) {
            this.preview = "";
            return;
        }
        String plain = content.replaceAll("<[^>]+>", "").trim();
        if (keyword == null || keyword.trim().isEmpty()) {
            setPreview();
            return;
        }
        int at = plain.toLowerCase().indexOf(keyword.trim().toLowerCase());
        if (at < 0) {
            setPreview();
            return;
        }
        int from = Math.max(0, at - 40);
        int to = Math.min(plain.length(), at + keyword.length() + 40);
        this.preview = (from > 0 ? "..." : "") + plain.substring(from, to)
                + (to < plain.length() ? "..." : "");
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }
    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
    public String getSourceCategory() { return sourceCategory; }
    public void setSourceCategory(String sourceCategory) { this.sourceCategory = sourceCategory; }
    public Boolean getTitleMatched() { return titleMatched; }
    public void setTitleMatched(Boolean titleMatched) { this.titleMatched = titleMatched; }
    public Boolean getContentMatched() { return contentMatched; }
    public void setContentMatched(Boolean contentMatched) { this.contentMatched = contentMatched; }
    public Boolean getContentSearchable() { return contentSearchable; }
    public void setContentSearchable(Boolean contentSearchable) { this.contentSearchable = contentSearchable; }
}
