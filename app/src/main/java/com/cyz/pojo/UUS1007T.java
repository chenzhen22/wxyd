package com.chenzhen.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class UUS1007T implements SvcBody{

    private String VER;
    private String APP;
    private String sysId;
    private String subUnitno;
    private String templateCode;
    private String domainId;

    @JSONField(name="VER")
    public String getVER() {
        return VER;
    }

    public void setVER(String VER) {
        this.VER = VER;
    }

    @JSONField(name="APP")
    public String getAPP() {
        return APP;
    }

    public void setAPP(String APP) {
        this.APP = APP;
    }
}
