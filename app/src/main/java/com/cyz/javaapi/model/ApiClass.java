package com.cyz.javaapi.model;

import java.util.List;

public class ApiClass {
    private String name;
    private String packageName;
    private String intro;
    private List<ApiMethod> methods;
    private List<TestCase> testCases;

    public ApiClass() {}

    public ApiClass(String name, String packageName, String intro,
                    List<ApiMethod> methods, List<TestCase> testCases) {
        this.name = name;
        this.packageName = packageName;
        this.intro = intro;
        this.methods = methods;
        this.testCases = testCases;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
    public List<ApiMethod> getMethods() { return methods; }
    public void setMethods(List<ApiMethod> methods) { this.methods = methods; }
    public List<TestCase> getTestCases() { return testCases; }
    public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }
}
