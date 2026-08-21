package com.chenzhen.pojo;

import lombok.Data;

@Data
public class Doc {
    private String transcode;
    private String transname;
    private String project;
    private String blname;
    private String type;
    private String fileName;
    private Long time;

    public Doc() {

    }

    public Doc(String transcode, String transname, String project, String blname, String type) {
        this.transcode = transcode;
        this.transname = transname;
        this.project = project;
        this.blname = blname;
        this.type = type;
    }

    public Doc(String fileName, String type, Long time) {
        this.fileName = fileName;
        this.type = type;
        this.time = time;
    }
}
