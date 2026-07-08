package com.chenzhen.model;

import java.util.List;

public class DBTableInfo
{
    private String sysChName;
    private String sysEnName;
    private String sysModel;
    private String tableType;
    private String isPrimkey;
    private String majorLevel;
    private List<TableInfo> tableList;

    public String getSysChName()
    {
        return this.sysChName;
    }

    public void setSysChName(String sysChName) {
        this.sysChName = sysChName;
    }

    public String getSysEnName() {
        return this.sysEnName;
    }

    public void setSysEnName(String sysEnName) {
        this.sysEnName = sysEnName;
    }

    public String getSysModel() {
        return this.sysModel;
    }

    public void setSysModel(String sysModel) {
        this.sysModel = sysModel;
    }

    public String getTableType() {
        return this.tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public String getIsPrimkey() {
        return this.isPrimkey;
    }

    public void setIsPrimkey(String isPrimkey) {
        this.isPrimkey = isPrimkey;
    }

    public String getMajorLevel() {
        return this.majorLevel;
    }

    public void setMajorLevel(String majorLevel) {
        this.majorLevel = majorLevel;
    }

    public List<TableInfo> getTableList() {
        return this.tableList;
    }

    public void setTableList(List<TableInfo> tableList) {
        this.tableList = tableList;
    }
}