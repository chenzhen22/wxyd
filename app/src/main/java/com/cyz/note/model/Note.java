package com.cyz.note.model;

/**
 * 记忆笔记，对应 data-note 目录下一个 .md 文件。
 * 结构参考 {@code com.cyz.shellscript.model.ShellTopic}，但笔记为自由正文，
 * 仅保留标题(name)、分类(category) 与完整 markdown 正文(content)。
 */
public class Note {

    private String name;
    private String category;
    private String content;

    public Note() {}

    public Note(String name, String category, String content) {
        this.name = name;
        this.category = category;
        this.content = content;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
