package com.cyz.javaapi.model;

import java.util.List;

public class ApiMethod {
    private String name;
    private String signature;
    private String description;
    private String returnType;
    private List<String> params;

    public ApiMethod() {}

    public ApiMethod(String name, String signature, String description,
                     String returnType, List<String> params) {
        this.name = name;
        this.signature = signature;
        this.description = description;
        this.returnType = returnType;
        this.params = params;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public List<String> getParams() { return params; }
    public void setParams(List<String> params) { this.params = params; }
}
