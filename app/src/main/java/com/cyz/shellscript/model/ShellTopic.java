package com.cyz.shellscript.model;

import java.util.List;

/**
 * Shell 脚本学习主题，对应 data-shell 目录下一个 .md 文件。
 * 结构参考 {@code com.cyz.javaapi.model.ApiClass}，但不包含可执行测试用例，
 * 仅保留介绍、语法、示例三段只读内容。
 */
public class ShellTopic {

    private String name;
    private String category;
    private String intro;
    private List<ShellSyntax> syntaxes;
    private List<ShellExample> examples;

    public ShellTopic() {}

    public ShellTopic(String name, String category, String intro,
                      List<ShellSyntax> syntaxes, List<ShellExample> examples) {
        this.name = name;
        this.category = category;
        this.intro = intro;
        this.syntaxes = syntaxes;
        this.examples = examples;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
    public List<ShellSyntax> getSyntaxes() { return syntaxes; }
    public void setSyntaxes(List<ShellSyntax> syntaxes) { this.syntaxes = syntaxes; }
    public List<ShellExample> getExamples() { return examples; }
    public void setExamples(List<ShellExample> examples) { this.examples = examples; }
}
