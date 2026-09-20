package com.cyz.shellscript.model;

/**
 * Shell 语法条目，对应 md 文件 ## 语法 段落下的一个 ### 子节。
 * 字段参考 {@code com.cyz.javaapi.model.ApiMethod}，简化掉返回值/参数列表。
 */
public class ShellSyntax {

    private String name;
    private String signature;
    private String description;

    public ShellSyntax() {}

    public ShellSyntax(String name, String signature, String description) {
        this.name = name;
        this.signature = signature;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
