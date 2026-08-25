package com.cyz.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ESBPojo {

    @JsonProperty(value = "ReqSvcHeader")
    private ReqSvcHeader ReqSvcHeader;

    @JsonProperty(value = "SvcBody")
    private SvcBody SvcBody;

    private String esbServiceName;

    @JSONField(name = "ReqSvcHeader")
    public ReqSvcHeader getReqSvcHeader() {
        return ReqSvcHeader;
    }

    public void setReqSvcHeader(com.cyz.pojo.ReqSvcHeader reqSvcHeader) {
        ReqSvcHeader = reqSvcHeader;
    }

    @JSONField(name = "SvcBody")
    public SvcBody getSvcBody() {
        return SvcBody;
    }

    public void setSvcBody(com.cyz.pojo.SvcBody svcBody) {
        SvcBody = svcBody;
    }

    public String getEsbServiceName() {
        return esbServiceName;
    }

    public void setEsbServiceName(String esbServiceName) {
        this.esbServiceName = esbServiceName;
    }
}
