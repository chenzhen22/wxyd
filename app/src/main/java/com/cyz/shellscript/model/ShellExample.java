package com.cyz.shellscript.model;

/**
 * Shell 示例，对应 md 文件 ## 示例 段落下的一个 ### 子节。
 * 字段参考 {@code com.cyz.javaapi.model.TestCase}，简化掉断言/期望输出，
 * 因为本模块仅展示只读 md 内容，不执行脚本。
 */
public class ShellExample {

    private String name;
    private String description;
    private String code;

    public ShellExample() {}

    public ShellExample(String name, String description, String code) {
        this.name = name;
        this.description = description;
        this.code = code;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
