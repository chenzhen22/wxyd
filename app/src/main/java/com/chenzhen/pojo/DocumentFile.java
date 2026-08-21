package com.chenzhen.pojo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString

public class DocumentFile {

    private String fileUUID;
    private String clientIp;
    private String userName;
    private String fileName;
    private String createTime;

    public DocumentFile(String fileUUID, String clientIp, String userName, String fileName, String createTime) {
        this.fileUUID = fileUUID;
        this.clientIp = clientIp;
        this.userName = userName;
        this.fileName = fileName;
        this.createTime = createTime;
    }

    public DocumentFile(){}
}
