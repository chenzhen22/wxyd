package com.cyz.javaapi.model;

public class TestCase {
    private String name;
    private String description;
    private String assertion;
    private String code;
    private String expectedOutput;

    public TestCase() {}

    public TestCase(String name, String description, String assertion,
                    String code, String expectedOutput) {
        this.name = name;
        this.description = description;
        this.assertion = assertion;
        this.code = code;
        this.expectedOutput = expectedOutput;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssertion() { return assertion; }
    public void setAssertion(String assertion) { this.assertion = assertion; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
}
