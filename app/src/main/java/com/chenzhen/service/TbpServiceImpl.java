package com.chenzhen.service;

import cn.hutool.core.util.StrUtil;
import com.chenzhen.mapper.mysqlMapper.MysqlMapper;
import com.chenzhen.pojo.Doc;
import com.chenzhen.pojo.DocumentFile;
import com.chenzhen.pojo.Result;
import com.chenzhen.util.CommUtils;
import com.chenzhen.util.DesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;

@Service
@Slf4j
public class TbpServiceImpl implements TbpService {
    private static ThreadLocal<CellStyle> threadLocal = new ThreadLocal<>();
    private static ThreadLocal<CellStyle> threadLocal2 = new ThreadLocal<>();

    @Resource
    MysqlMapper mysqlMapper;

    @Autowired
    UserService userService;

    private static void setStyle(Workbook workBook) {
        CellStyle style = workBook.createCellStyle();
        Font font = workBook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        threadLocal.set(style);

        CellStyle style2 = workBook.createCellStyle();
        style2.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        Font font2 = workBook.createFont();
        font2.setFontName("微软雅黑");
        font2.setFontHeightInPoints((short) 9);
        font2.setBold(true);
        style2.setFont(font2);
        style2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style2.setBorderBottom(BorderStyle.THIN);
        style2.setBorderLeft(BorderStyle.THIN);
        style2.setBorderTop(BorderStyle.THIN);
        style2.setBorderRight(BorderStyle.THIN);
        threadLocal2.set(style2);
    }

    /**
     * 获取下载excel模板
     *
     * @param templatePath
     * @param templateName
     * @return
     */
    public static Workbook getExcelTemplate(String templatePath, String templateName) {

        Workbook workBook = null;
        FileInputStream fis = null;
        try {
            File file = new File(templatePath, templateName);
            log.info("文件存在--> {}", file.exists());
            if (!file.exists()) {
                log.info("创建文件成功--> {}", file.createNewFile());
            }
            fis = new FileInputStream(file);
            workBook = new XSSFWorkbook(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return workBook;
    }

    /**
     * 解析xml文件参数
     *
     * @param path
     * @param sheet
     * @return
     * @throws DocumentException
     */
    public static Map getBLXML(String path, Sheet sheet, Workbook workBook) throws DocumentException {

        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(path));
        Element root = document.getRootElement();
        Iterator bl_businessLogic = root.elementIterator();
        Map dataDictionaryMap = new HashMap();
        Map listMap = new HashMap();
        while (bl_businessLogic.hasNext()) {
            Element businessLogic_element = (Element) bl_businessLogic.next();//businessLogic
            Iterator bl_dataDictionary = businessLogic_element.elementIterator();
            Element dataDictionary_element = (Element) bl_dataDictionary.next();//dataDictionary
            if (dataDictionary_element.getName().equals("dataDictionary")) {
                Iterator bl_dataField = dataDictionary_element.elementIterator();
                while (bl_dataField.hasNext()) {
                    Element dataField_element = (Element) bl_dataField.next();
                    if (dataField_element.getName().equals("dataField")) {
                        dataDictionaryMap.put(dataField_element.attributeValue("name"), "dataField:" + dataField_element.attributeValue("desc"));
                    } else if (dataField_element.getName().equals("dataList")) {
                        saveDataList(dataDictionaryMap, listMap, dataField_element, dataField_element.attributeValue("name"));
                    }
                }
            }
            Element input_element = (Element) bl_dataDictionary.next();//bl:input
            int rowNo = 4;
            Row row = null;
            Cell cell = null;
            String vl = "";
            if (input_element.getName().equals("input")) {
                Iterator input_inField = input_element.elementIterator();
                while (input_inField.hasNext()) {
                    Element inField_element = (Element) input_inField.next();
                    vl = (String) dataDictionaryMap.get(inField_element.attributeValue("name"));
                    if (StringUtils.isEmpty(vl)) {
                        //单个字段
                        writeCell(sheet, inField_element.attributeValue("name"), vl, "String", rowNo, workBook);
                        rowNo++;
                    } else if (vl.startsWith("dataField:")) {
                        //单个字段
                        writeCell(sheet, inField_element.attributeValue("name"), vl.substring(10), "String", rowNo, workBook);
                        rowNo++;
                    } else {
                        //集合字段
                        rowNo = writeList(sheet, inField_element.attributeValue("name"), rowNo, dataDictionaryMap, listMap, "", workBook);
                    }
                    //System.out.println(inField_element.attributeValue("name"));
                }
            }
            row = sheet.createRow(rowNo);
            sheet.addMergedRegion(new CellRangeAddress(rowNo, rowNo, 0, 6));
            cell = row.createCell(0);

            cell.setCellValue("输出");
            CellStyle style = threadLocal2.get();
            cell.setCellStyle(style);

            Cell cell1 = row.createCell(1);
            cell1.setCellStyle(style);

            Cell cell2 = row.createCell(2);
            cell2.setCellStyle(style);

            Cell cell3 = row.createCell(3);
            cell3.setCellStyle(style);

            Cell cell4 = row.createCell(4);
            cell4.setCellStyle(style);

            Cell cell5 = row.createCell(5);
            cell5.setCellStyle(style);

            Cell cell6 = row.createCell(6);
            cell6.setCellStyle(style);

            rowNo++;

            Element output_element = (Element) bl_dataDictionary.next();//bl:output
            if (output_element.getName().equals("output")) {
                Iterator output_inField = output_element.elementIterator();
                while (output_inField.hasNext()) {
                    Element outField_element = (Element) output_inField.next();
                    vl = (String) dataDictionaryMap.get(outField_element.attributeValue("name"));
                    if (vl == null) {
                        continue;
                    }
                    if (vl.startsWith("dataField:")) {
                        //单个字段
                        writeCell(sheet, outField_element.attributeValue("name"), vl.substring(10), "String", rowNo, workBook);
                        rowNo++;
                    } else {
                        rowNo = writeList(sheet, outField_element.attributeValue("name"), rowNo, dataDictionaryMap, listMap, "", workBook);
                    }
                }
            }
        }
        return null;
    }

    public static void saveDataList(Map dataDictionaryMap, Map listMap, Element dataField_element, String key) {
        dataDictionaryMap.put(key, "dataList:" + dataField_element.attributeValue("desc"));
        Map map = new HashMap();
        Iterator list_columnField = dataField_element.elementIterator();
        while (list_columnField.hasNext()) {
            Element columnField_element = (Element) list_columnField.next();
            if (columnField_element.getName().equals("columnField")) {
                map.put(columnField_element.attributeValue("name"), "columnField:" + columnField_element.attributeValue("desc"));
            } else if (columnField_element.getName().equals("dataList")) {
                map.put(columnField_element.attributeValue("name"), "dataList:" + columnField_element.attributeValue("desc"));
                saveDataList(dataDictionaryMap, listMap, columnField_element, columnField_element.attributeValue("name"));
            }
        }
        listMap.put(key, map);
    }

    public static void writeCell(Sheet sheet, String code, String name, String type, int rowNo, Workbook workBook) {
        //单个字段
        Row row = sheet.createRow(rowNo);
        Cell cell = row.createCell(0);
        cell.setCellValue(code);
        Cell cell1 = row.createCell(1);
        cell1.setCellValue(name);
        Cell cell2 = row.createCell(2);
        cell2.setCellValue(type);

        CellStyle style = threadLocal.get();

        cell.setCellStyle(style);
        cell1.setCellStyle(style);
        cell2.setCellStyle(style);

        Cell cell3 = row.createCell(3);
        cell3.setCellStyle(style);

        Cell cell4 = row.createCell(4);
        cell4.setCellStyle(style);

        Cell cell5 = row.createCell(5);
        cell5.setCellStyle(style);

        Cell cell6 = row.createCell(6);
        cell6.setCellStyle(style);

    }

    public static int writeList(Sheet sheet, String listName, int rowNo, Map dataDictionaryMap, Map listMap, String kong, Workbook workBook) {
        String vl = (String) dataDictionaryMap.get(listName);
        writeCell(sheet, kong + listName, vl.substring(9), "list", rowNo, workBook);

        String kongString = "    ";
        if (kong.equals("")) {
            kongString = "    ";
        } else {
            kongString = kongString + kong;
        }

        rowNo++;

        Map columnFieldMap = (Map) listMap.get(listName);
        for (Iterator iterator = columnFieldMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            String value = (String) columnFieldMap.get(key);
            if (value.startsWith("columnField:")) {
                //单个字段
                writeCell(sheet, kongString + key, value.substring(12), "String", rowNo, workBook);
                rowNo++;
            } else {
                //集合
                rowNo = writeList(sheet, key, rowNo, dataDictionaryMap, listMap, kongString, workBook);
            }
        }
        writeCell(sheet, kong + listName, vl.substring(9), "list", rowNo, workBook);
        rowNo++;
        return rowNo;
    }

    //获取下载excel模板
    public static String saveExcel(Workbook workbook, String templatePath, String docname, String project) {

        String path = templatePath + "/" + project + "/" + docname + ".xlsx";
        File file = new File(path);
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            workbook.write(os);
            os.flush();
            os.close();
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
    }

    @Override
    public Result dec(String type, String password) {
        Result result = Result.getInstance();
        String secretKey = CommUtils.getParamValue("passworkKey");
        if ("1".equals(type)) {
            password = DesUtil.encode(secretKey, password);
        } else if ("2".equals(type)) {
            password = DesUtil.decode(secretKey, password);
        }
        result.setBody(password.trim());
        return result;
    }

    @Override
    public Result docQry(String docname) {
        Result result = Result.getInstance();
        Doc doc = mysqlMapper.queryDoc(docname);
        result.setBody(doc);
        return result;
    }

    @Override
    public void createDoc(String type) {
        String xml = ".xml";
        String templatePath = "/apps/data/wxyd/doc/";
        String templateName = "1.xlsx";

        List<Doc> docListD = mysqlMapper.queryDocAll(type);
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

        Workbook workBook = getExcelTemplate(templatePath, templateName);
        setStyle(workBook);

        String dateString = CommUtils.getDateString("yyyyMMddHHmmssS");
        dateString = type + dateString;

        for (int i = 0; i < docList.size(); i++) {
            Doc doc = docList.get(i);
            log.info("parse doc xml:{}", doc);
            String fileName = doc.getBlname();
            String transCode = doc.getTranscode();
            String transName = doc.getTransname();
            String project = doc.getProject();

            String pa = "/apps/data/wxyd/doc/bl/" + project + "/";
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
                initCell(sheet, workBook, dateString, i);
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
                getBLXML(path, sheet, workBook);
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }

        String npath = saveExcel(workBook, templatePath, dateString, "");
        Doc docFile = new Doc(dateString + ".xlsx", type, System.currentTimeMillis());
        mysqlMapper.addDocFile(docFile);
    }

    private void initCell(Sheet sheet, Workbook workBook, String dateString, int i) {
        Sheet sheet0 = workBook.cloneSheet(1);
        sheet.setColumnWidth(0, sheet0.getColumnWidth(0));
        sheet.setColumnWidth(1, sheet0.getColumnWidth(1));
        sheet.setColumnWidth(2, sheet0.getColumnWidth(2));
        sheet.setColumnWidth(3, sheet0.getColumnWidth(3));
        Row row = sheet.getRow(0);
        if (row == null) {
            row = sheet.createRow(0);
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 4, 6));

        Cell cell00 = row.getCell(0);
        if (cell00 == null) {
            cell00 = row.createCell(0);
        }
        cell00.setCellValue(sheet0.getRow(0).getCell(0).getStringCellValue());
        cell00.setCellStyle(sheet0.getRow(0).getCell(0).getCellStyle());

        Cell cell01 = row.getCell(1);
        if (cell01 == null) {
            cell01 = row.createCell(1);
        }
        cell01.setCellValue(sheet0.getRow(0).getCell(1).getStringCellValue());
        cell01.setCellStyle(sheet0.getRow(0).getCell(1).getCellStyle());

        Cell cell02 = row.getCell(2);
        if (cell02 == null) {
            cell02 = row.createCell(2);
        }
        cell02.setCellValue(sheet0.getRow(0).getCell(2).getStringCellValue());
        cell02.setCellStyle(sheet0.getRow(0).getCell(2).getCellStyle());

        Cell cell03 = row.getCell(3);
        if (cell03 == null) {
            cell03 = row.createCell(3);
        }
        cell03.setCellValue(sheet0.getRow(0).getCell(3).getStringCellValue());
        cell03.setCellStyle(sheet0.getRow(0).getCell(3).getCellStyle());

        Cell cell04 = row.getCell(4);
        if (cell04 == null) {
            cell04 = row.createCell(4);
        }
        cell04.setCellValue(sheet0.getRow(0).getCell(4).getStringCellValue());
        cell04.setCellStyle(sheet0.getRow(0).getCell(4).getCellStyle());

        Cell cell05 = row.getCell(5);
        if (cell05 == null) {
            cell05 = row.createCell(5);
        }
        cell05.setCellValue(sheet0.getRow(0).getCell(5).getStringCellValue());
        cell05.setCellStyle(sheet0.getRow(0).getCell(5).getCellStyle());

        Cell cell06 = row.getCell(6);
        if (cell06 == null) {
            cell06 = row.createCell(6);
        }
        cell06.setCellStyle(sheet0.getRow(0).getCell(6).getCellStyle());
        cell06.setCellValue(sheet0.getRow(0).getCell(5).getStringCellValue());
       /* cell06.setCellType(Cell.CELL_TYPE_FORMULA);
        cell06.setCellFormula("HYPERLINK(\"["+dateString+".xls]'目录'!A"+(i+2)+"\",\"返回\")");*/

        Row row1 = sheet.getRow(1);
        if (row1 == null) {
            row1 = sheet.createRow(1);
        }
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 6));
        Cell cell10 = row1.getCell(0);
        if (cell10 == null) {
            cell10 = row1.createCell(0);
        }
        cell10.setCellValue(sheet0.getRow(1).getCell(0).getStringCellValue());
        cell10.setCellStyle(sheet0.getRow(1).getCell(0).getCellStyle());

        Cell cell11 = row1.getCell(1);
        if (cell11 == null) {
            cell11 = row1.createCell(1);
        }
        cell11.setCellValue(sheet0.getRow(1).getCell(1).getStringCellValue());
        cell11.setCellStyle(sheet0.getRow(1).getCell(1).getCellStyle());

        Cell cell12 = row1.getCell(2);
        if (cell12 == null) {
            cell12 = row1.createCell(2);
        }
        cell12.setCellValue(sheet0.getRow(1).getCell(2).getStringCellValue());
        cell12.setCellStyle(sheet0.getRow(1).getCell(2).getCellStyle());

        Cell cell13 = row1.getCell(3);
        if (cell13 == null) {
            cell13 = row1.createCell(3);
        }
        cell13.setCellValue(sheet0.getRow(1).getCell(3).getStringCellValue());
        cell13.setCellStyle(sheet0.getRow(1).getCell(3).getCellStyle());

        Cell cell14 = row1.getCell(4);
        if (cell14 == null) {
            cell14 = row1.createCell(4);
        }
        cell14.setCellValue(sheet0.getRow(1).getCell(4).getStringCellValue());
        cell14.setCellStyle(sheet0.getRow(1).getCell(4).getCellStyle());

        Cell cell15 = row1.getCell(5);
        if (cell15 == null) {
            cell15 = row1.createCell(5);
        }
        cell15.setCellValue(sheet0.getRow(1).getCell(5).getStringCellValue());
        cell15.setCellStyle(sheet0.getRow(1).getCell(5).getCellStyle());

        Cell cell16 = row1.getCell(6);
        if (cell16 == null) {
            cell16 = row1.createCell(6);
        }
        cell16.setCellValue(sheet0.getRow(1).getCell(6).getStringCellValue());
        cell16.setCellStyle(sheet0.getRow(1).getCell(6).getCellStyle());

        Row row2 = sheet.getRow(2);
        if (row2 == null) {
            row2 = sheet.createRow(2);
        }
        Cell cell20 = row2.getCell(0);
        if (cell20 == null) {
            cell20 = row2.createCell(0);
        }
        cell20.setCellValue(sheet0.getRow(2).getCell(0).getStringCellValue());
        cell20.setCellStyle(sheet0.getRow(2).getCell(0).getCellStyle());

        Cell cell21 = row2.getCell(1);
        if (cell21 == null) {
            cell21 = row2.createCell(1);
        }
        cell21.setCellValue(sheet0.getRow(2).getCell(1).getStringCellValue());
        cell21.setCellStyle(sheet0.getRow(2).getCell(1).getCellStyle());

        Cell cell22 = row2.getCell(2);
        if (cell22 == null) {
            cell22 = row2.createCell(2);
        }
        cell22.setCellValue(sheet0.getRow(2).getCell(2).getStringCellValue());
        cell22.setCellStyle(sheet0.getRow(2).getCell(2).getCellStyle());

        Cell cell23 = row2.getCell(3);
        if (cell23 == null) {
            cell23 = row2.createCell(3);
        }
        cell23.setCellValue(sheet0.getRow(2).getCell(3).getStringCellValue());
        cell23.setCellStyle(sheet0.getRow(2).getCell(3).getCellStyle());

        Cell cell24 = row2.getCell(4);
        if (cell24 == null) {
            cell24 = row2.createCell(4);
        }
        cell24.setCellValue(sheet0.getRow(2).getCell(4).getStringCellValue());
        cell24.setCellStyle(sheet0.getRow(2).getCell(4).getCellStyle());

        Cell cell25 = row2.getCell(5);
        if (cell25 == null) {
            cell25 = row2.createCell(5);
        }
        cell25.setCellValue(sheet0.getRow(2).getCell(5).getStringCellValue());
        cell25.setCellStyle(sheet0.getRow(2).getCell(5).getCellStyle());

        Cell cell26 = row2.getCell(6);
        if (cell26 == null) {
            cell26 = row2.createCell(6);
        }
        cell26.setCellValue(sheet0.getRow(2).getCell(6).getStringCellValue());
        cell26.setCellStyle(sheet0.getRow(2).getCell(6).getCellStyle());

        Row row3 = sheet.getRow(3);
        if (row3 == null) {
            row3 = sheet.createRow(3);
        }
        Cell cell30 = row3.getCell(0);
        if (cell30 == null) {
            cell30 = row3.createCell(0);
        }
        cell30.setCellValue(sheet0.getRow(3).getCell(0).getStringCellValue());
        cell30.setCellStyle(sheet0.getRow(3).getCell(0).getCellStyle());

        Cell cell31 = row3.getCell(1);
        if (cell31 == null) {
            cell31 = row3.createCell(1);
        }
        cell31.setCellValue(sheet0.getRow(3).getCell(1).getStringCellValue());
        cell31.setCellStyle(sheet0.getRow(3).getCell(1).getCellStyle());

        Cell cell32 = row3.getCell(2);
        if (cell32 == null) {
            cell32 = row3.createCell(2);
        }
        cell32.setCellValue(sheet0.getRow(3).getCell(2).getStringCellValue());
        cell32.setCellStyle(sheet0.getRow(3).getCell(2).getCellStyle());

        Cell cell33 = row3.getCell(3);
        if (cell33 == null) {
            cell33 = row3.createCell(3);
        }
        cell33.setCellValue(sheet0.getRow(3).getCell(3).getStringCellValue());
        cell33.setCellStyle(sheet0.getRow(3).getCell(3).getCellStyle());

        Cell cell34 = row3.getCell(4);
        if (cell34 == null) {
            cell34 = row3.createCell(4);
        }
        cell34.setCellValue(sheet0.getRow(3).getCell(4).getStringCellValue());
        cell34.setCellStyle(sheet0.getRow(3).getCell(4).getCellStyle());

        Cell cell35 = row3.getCell(5);
        if (cell35 == null) {
            cell35 = row3.createCell(5);
        }
        cell35.setCellValue(sheet0.getRow(3).getCell(5).getStringCellValue());
        cell35.setCellStyle(sheet0.getRow(3).getCell(5).getCellStyle());

        Cell cell36 = row3.getCell(6);
        if (cell36 == null) {
            cell36 = row3.createCell(6);
        }
        cell36.setCellValue(sheet0.getRow(3).getCell(6).getStringCellValue());
        cell36.setCellStyle(sheet0.getRow(3).getCell(6).getCellStyle());

        workBook.removeSheetAt(workBook.getSheetIndex(sheet0));
    }

    /**
     * 获取下载excel模板
     *
     * @param path
     * @return
     */
    public Workbook getExcelTemplate(String path) {

        Workbook workBook = null;
        FileInputStream fis = null;
        try {
            File file = new File(path);
            fis = new FileInputStream(file);
            workBook = new XSSFWorkbook(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return workBook;
    }

    @Override
    public void uploadDoc(Doc doc) {
        if (mysqlMapper.queryDoc(doc.getTranscode()) == null) {
            mysqlMapper.addDoc(doc);
        }
        {
            mysqlMapper.updateDoc(doc);
        }
    }

    @Override
    public void uploadDocumentFile(DocumentFile docFile) {
        docFile.setClientIp(CommUtils.getClientIpByMDC());
        String userName = userService.getUserName(CommUtils.getClientIpByMDC());
        if (StrUtil.isEmpty(userName)) {
            userName = "游客";
        }
        docFile.setUserName(userName);
        mysqlMapper.addDocumentFile(docFile);
    }

    @Override
    public String docQryAll(String type) {
        Doc doc = mysqlMapper.queryDocFile(type);
        if (doc != null) {
            String fileName = doc.getFileName();
            return fileName;
        }

        return "";
    }

    @Override
    public Result queryDocAll(String type) {
        List<Doc> docListD = mysqlMapper.queryDocAll(type);
        Result result = Result.getInstance();
        result.setBody(docListD);
        return result;
    }

    @Override
    public void addDocFile(Doc doc) {
        mysqlMapper.addDocFile(doc);
    }

    @Override
    public Result queryDocumentFileList(String fileName, String clientIp) {
        Result result = Result.getInstance();
        List<DocumentFile> list = mysqlMapper.queryDocumentFileList(fileName, clientIp);
        result.setBody(list);
        return result;
    }

    @Override
    public DocumentFile queryDocumentFile(String fileUUID) {
        return mysqlMapper.queryDocumentFile(fileUUID);
    }

    @Override
    public int deleteDocumentFile(String fileUUID) {
        return mysqlMapper.deleteDocumentFile(fileUUID);
    }

}
