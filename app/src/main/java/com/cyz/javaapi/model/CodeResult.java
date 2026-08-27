package com.cyz.javaapi.model;

public class CodeResult {
    private boolean success;
    private boolean passed;
    private String output;
    private String error;
    private long executionTime;

    public CodeResult() {}

    public CodeResult(boolean success, boolean passed, String output,
                      String error, long executionTime) {
        this.success = success;
        this.passed = passed;
        this.output = output;
        this.error = error;
        this.executionTime = executionTime;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
}
