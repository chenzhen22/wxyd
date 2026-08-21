package com.chenzhen.service;

import com.chenzhen.pojo.Doc;
import com.chenzhen.pojo.DocumentFile;
import com.chenzhen.pojo.Result;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

public interface TbpService {

    Result dec(String type, String password);

    Result docQry(String docname);

    void createDoc(String type);

    Workbook getExcelTemplate(String path);

    void uploadDoc(Doc doc);

    void uploadDocumentFile(DocumentFile docFile);

    String docQryAll(String type);

    Result queryDocAll(String type);

    void addDocFile(Doc doc);

    Result queryDocumentFileList(String fileName, String clientIp);

    DocumentFile queryDocumentFile(String fileUUID);

    int deleteDocumentFile(String fileUUID);
}

