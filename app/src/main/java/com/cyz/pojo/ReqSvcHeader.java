package com.chenzhen.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class ReqSvcHeader {

    private String consumerId;
    private String tranCode;
    private String tranSeqNo;
    private String globalSeqNo;
    private String tranDate;
    private String tranTime;
    private String tranTellerNo;
    private String branchId;
    private String acctDate;
    private String sourceSysId;
    private String terminalCode;
    private String cityCode;
    private String authrTellerNo;
    private String reviewAuthrTellerNo;
    private String authrTellerSeqNo;
    private String authrPwd;
    private String userId;
    private String orgId;
    private String extendContent;
    private String channel;
    private String UNIQUE_SEQ_NUM;
    private String SRC_MODULE;

    @JSONField(name = "UNIQUE_SEQ_NUM")
    public String getUNIQUE_SEQ_NUM() {
        return UNIQUE_SEQ_NUM;
    }

    public void setUNIQUE_SEQ_NUM(String UNIQUE_SEQ_NUM) {
        this.UNIQUE_SEQ_NUM = UNIQUE_SEQ_NUM;
    }

    @JSONField(name = "SRC_MODULE")
    public String getSRC_MODULE() {
        return SRC_MODULE;
    }

    public void setSRC_MODULE(String SRC_MODULE) {
        this.SRC_MODULE = SRC_MODULE;
    }
}
