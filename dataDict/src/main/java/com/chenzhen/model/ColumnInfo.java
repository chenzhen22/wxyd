package com.chenzhen.model;

public class ColumnInfo
{
    private String orderNo;
    private String enName;
    private String chName;

    private String dataType;
    private String defValue;
    private String nullValidate;
    private String mark;

    public String getOrderNo()
    {
        return this.orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getEnName() {
        return this.enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public String getChName() {
        return this.chName;
    }

    public void setChName(String chName) {
        this.chName = chName;
    }

    public String getDataType() {
        return this.dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getDefValue() {
        return this.defValue;
    }

    public void setDefValue(String defValue) {
        this.defValue = defValue;
    }

    public String getNullValidate() {
        return this.nullValidate;
    }

    public void setNullValidate(String nullValidate) {
        this.nullValidate = nullValidate;
    }

    public String getMark() {
        return this.mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }
}
