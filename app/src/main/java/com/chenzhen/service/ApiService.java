package com.chenzhen.service;

import com.chenzhen.pojo.Doc;
import com.chenzhen.pojo.DocumentFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface ApiService {

    Object tbphx(String transData, HttpServletRequest request);

    String docQryAll(String type);

    String docQry(String docname);

    void uploadDoc(Doc doc);

    void uploadDocumentFile(DocumentFile docFile);

    Object queryDocumentFileList(String fileName, String type);

    int deleteDocumentFile(String fileUUID);
}
