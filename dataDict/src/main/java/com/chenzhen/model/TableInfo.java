package com.chenzhen.model;

import java.util.List;

public class TableInfo
{
    private String dirLink;
    private String chName;
    private String enName;
    private String unique;
    private String noUnique;
    private String desc;
    private String order;
    private String primKeys;
    private String forKeys;
    private String tableType;
    private String tablespaceName;
    private String partitionNames;
    private List<ColumnInfo> columnList;

    public String getDirLink()
    {
        return this.dirLink;
    }

    public void setDirLink(String dirLink) {
        this.dirLink = dirLink;
    }

    public String getChName() {
        return this.chName;
    }

    public void setChName(String chName) {
        this.chName = chName;
    }

    public String getEnName() {
        return this.enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public String getUnique() {
        return this.unique;
    }

    public void setUnique(String unique) {
        this.unique = unique;
    }

    public String getNoUnique() {
        return this.noUnique;
    }

    public void setNoUnique(String noUnique) {
        this.noUnique = noUnique;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getOrder() {
        return this.order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getPrimKeys() {
        return this.primKeys;
    }

    public void setPrimKeys(String primKeys) {
        this.primKeys = primKeys;
    }

    public String getForKeys() {
        return this.forKeys;
    }

    public void setForKeys(String forKeys) {
        this.forKeys = forKeys;
    }

    public String getTableType() {
        return this.tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public String getTablespaceName() {
        return this.tablespaceName;
    }

    public void setTablespaceName(String tablespaceName) {
        this.tablespaceName = tablespaceName;
    }

    public String getPartitionNames() {
        return this.partitionNames;
    }

    public void setPartitionNames(String partitionNames) {
        this.partitionNames = partitionNames;
    }

    public List<ColumnInfo> getColumnList() {
        return this.columnList;
    }

    public void setColumnList(List<ColumnInfo> columnList) {
        this.columnList = columnList;
    }
}