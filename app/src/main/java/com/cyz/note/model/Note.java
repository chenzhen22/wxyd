package com.cyz.note.model;

/**
 * 记忆笔记，对应 /apps/shareFile/{userId}/ 目录下一个 .md 文件。
 * 元数据内嵌于 md 的 YAML frontmatter（name/category/visibility），
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
}
