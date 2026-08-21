package com.chenzhen.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chenzhen.pojo.Doc;
import com.chenzhen.pojo.DocumentFile;
import com.chenzhen.pojo.Result;
import com.chenzhen.util.BtoAAtoB;
import com.chenzhen.util.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.dom4j.DocumentException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ApiServiceImpl implements ApiService {

    @Override
    public Object tbphx(String transData, HttpServletRequest request) {
        String clientIp = CommUtils.getClientIp(request);
        log.info("request data：{}, ip: {} ", transData, clientIp);
        Result result = Result.getInstance();
        try {
            transData = BtoAAtoB.atob(transData);
            transData = URLDecoder.decode(transData, "UTF-8");
            if (!transData.contains("||")) {
                long timestamp = System.currentTimeMillis();
                transData = transData + "||" + timestamp;
                transData = URLEncoder.encode(transData, "UTF-8");
                transData = BtoAAtoB.btoa(transData);
                Map<String, String> map = new HashMap();
                map.put("transData", transData);
                result.setBody(map);
                return result;
            }
            String[] ss = transData.split("\\|\\|", -1);
            long delTime = Long.parseLong(ss[1]) - System.currentTimeMillis();
            if (Math.abs(delTime) > 3000) {
                return result;
            }
            String reqString = ss[0];
            reqString = reqString.replace("\\", "\\\\");
            JSONObject json = JSONObject.parseObject(reqString);
            log.info("request data：{}", json);
            return MsgService.getResult(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String docQryAll(String type) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type);
            jsonObject.put("action", "docQryAll");
            Result result = MsgService.getResult(jsonObject);
            return (String) result.getBody();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String docQry(String docname) { ;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("docname", docname);
        jsonObject.put("action", "docQry");
        Result result = MsgService.getResult(jsonObject);
        JSONObject resJson = (JSONObject) result.getBody();
        Doc doc = JSONObject.toJavaObject(resJson, Doc.class);
        String fileName = doc.getBlname();
        String transCode = docname;
        String transName = doc.getTransname();
        String project = doc.getProject();

        String pa = "./doc/bl/" + project + "/";
        String xml = ".xml";
        String templatePath = "./doc/";
        String templateName = "2.xlsx";
        Workbook workBook = null;
        workBook = DocService.getExcelTemplate(templatePath, templateName);
        DocService.setStyle(workBook);
        Sheet sheet = workBook.getSheetAt(0);
        workBook.setSheetName(0, docname);
        Row row = sheet.getRow(0);
        if (row == null) {
            row = sheet.createRow(0);
        }
        Cell cell = row.getCell(1);//设置交易码
        cell.setCellValue(new XSSFRichTextString(transCode));
        cell = row.getCell(4);//设置交易名
        cell.setCellValue(new XSSFRichTextString(transName));
        row = sheet.getRow(1);
        cell = row.getCell(1);//设置交易码
        cell.setCellValue(new XSSFRichTextString(transName));
        String path = pa + fileName.replace(".", "/") + xml;
        try {
            DocService.getBLXML(path, sheet, workBook);
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        String npath = DocService.saveExcel(workBook, templatePath, docname, project);
        return npath;
    }

    @Override
    public void uploadDoc(Doc doc) {
        Result result = Result.getInstance();
        result.setBody(doc);
        JSONObject jsonObject = (JSONObject) JSONObject.toJSON(doc);
        jsonObject.put("action","uploadDoc");
        MsgService.getResult(jsonObject);
        new Thread(() -> {
            createDoc(doc.getType());
        }).start();
    }

    public void createDoc(String type) {
        String xml = ".xml";
        String templatePath = "/apps/data/wxyd/doc/";
        String templateName = "1.xlsx";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type);
        jsonObject.put("action", "queryDocAll");
        List<Doc> docListD = (List<Doc>) MsgService.getResult(jsonObject).getBody();
        List<Doc> docList = new ArrayList<>();
        for (Doc doc : docListD) {
            String blName = doc.getBlname();
            String project = doc.getProject();
            String pa = "/apps/data/wxyd/doc/bl/" + project + "/";

            String path = pa + blName.replace(".", "/") + xml;

            File file = new File(path);
            if (file.exists()) {
                docList.add(doc);
            }
        }

        Workbook workBook = DocService.getExcelTemplate(templatePath, templateName);
        DocService.setStyle(workBook);

        String dateString = CommUtils.getDateString("yyyyMMddHHmmssS");
        dateString = type + dateString;

        for (int i = 0; i < docList.size(); i++) {
            Doc doc = docList.get(i);
            log.info("parse doc xml:{}", doc);
            String fileName = doc.getBlname();
            String transCode = doc.getTranscode();
            String transName = doc.getTransname();
            String project = doc.getProject();

            String pa = "./doc/bl/" + project + "/";
            Sheet sheet = null;
            Row row = null;
            if (i == 0) {
                sheet = workBook.getSheetAt(i + 1);
                workBook.setSheetName(i + 1, transCode);
                row = sheet.getRow(0);
                if (row == null) {
                    row = sheet.createRow(0);
                }
            } else {
                sheet = workBook.createSheet();
                DocService.initCell(sheet, workBook, dateString, i);
                workBook.setSheetName(i + 1, transCode);
                row = sheet.getRow(0);
            }
            Cell cell = row.getCell(1);//设置交易码
            cell.setCellValue(new XSSFRichTextString(transCode));
            cell = row.getCell(4);//设置交易名
            cell.setCellValue(new XSSFRichTextString(transName));
            /*Cell cell_f = row.getCell(6);
            cell_f.setCellValue(new XSSFRichTextString("返回"));
            cell_f.setCellType(Cell.CELL_TYPE_FORMULA);
            cell_f.setCellFormula("HYPERLINK(\"["+dateString+".xls]'目录'!A"+(i+2)+"\",\"返回\")");
*/
            row = sheet.getRow(1);
            cell = row.getCell(1);//设置交易码
            cell.setCellValue(new XSSFRichTextString(transName));

            Sheet sheet0 = workBook.getSheetAt(0);
            Row row0 = sheet0.getRow(i + 1);
            if (row0 == null) {
                row0 = sheet0.createRow(i + 1);
            }
            Cell cell0 = row0.getCell(0);
            if (cell0 == null) {
                cell0 = row0.createCell(0);
            }
           /* cell0.setCellStyle(cell_f.getCellStyle());
            cell0.setCellType(Cell.CELL_TYPE_FORMULA);
            cell0.setCellFormula("HYPERLINK(\"["+dateString+".xls]'"+transCode+"'!A1\",\""+transCode+"\")");*/
            cell0.setCellValue(new XSSFRichTextString(transCode));

            Cell cell1 = row0.getCell(1);
            if (cell1 == null) {
                cell1 = row0.createCell(1);
            }
           /* cell1.setCellStyle(cell_f.getCellStyle());
            cell1.setCellType(Cell.CELL_TYPE_FORMULA);
            cell1.setCellFormula("HYPERLINK(\"["+dateString+".xls]'"+transCode+"'!A1\",\""+transName+"\")");*/
            cell1.setCellValue(new XSSFRichTextString(transName));

            String path = pa + fileName.replace(".", "/") + xml;
            try {
                DocService.getBLXML(path, sheet, workBook);
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }

        DocService.saveExcel(workBook, templatePath, dateString, "");
        Doc docFile = new Doc(dateString + ".xlsx", type, System.currentTimeMillis());
        JSONObject addDocFileJ = (JSONObject) JSON.toJSON(docFile);
        MsgService.getResult(addDocFileJ);
    }

    @Override
    public void uploadDocumentFile(DocumentFile docFile) {
        JSONObject jsonObject = (JSONObject) JSONObject.toJSON(docFile);
        jsonObject.put("action","uploadDocumentFile");
        MsgService.getResult(jsonObject);
    }

    @Override
    public Object queryDocumentFileList(String fileName, String type) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileName", fileName);
        jsonObject.put("type", type);
        jsonObject.put("action", "queryDocumentFileList");
        return MsgService.getResult(jsonObject);
    }

    @Override
    public int deleteDocumentFile(String fileUUID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileUUID", fileUUID);
        jsonObject.put("action", "deleteDocumentFile");
        Result result = MsgService.getResult(jsonObject);
        return (int) result.getBody();
    }

}
