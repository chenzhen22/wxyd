package com.chenzhen.excel;

import com.chenzhen.model.ColumnInfo;
import com.chenzhen.model.TableInfo;
import com.chenzhen.util.DataQuery;
import com.chenzhen.util.ExcelUtil;
import com.chenzhen.util.SysConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public class ExportExcel
{
    public static void makeTableDirHeader(HSSFWorkbook workbook, Sheet sheet)
    {
        HSSFCellStyle style = ExcelUtil.setHeaderCellStyle(workbook);

        String[] title = { "系统名称", "英文缩写", "系统模块", "表英文名", "表中文名", "表类型", "主  键", "主键字段", "表空间", "是否分区" };
        Row row = sheet.createRow(0);
        row.setHeightInPoints(20.0F);
        for (int c = 0; c < title.length; ++c) {
            Cell cell = row.createCell(c);
            cell.setCellValue(title[c]);
            cell.setCellStyle(style);
        }
    }

    public static void makeTableDirInfo(HSSFWorkbook workbook, Sheet sheet)
            throws SQLException
    {
        HSSFCellStyle style1 = ExcelUtil.setBodyCellStyle(workbook, Short.parseShort(2+""));
        HSSFCellStyle style2 = ExcelUtil.setBodyCellStyle(workbook, Short.parseShort(1+""));
        SysConfig.getInstance();
        String sysChName = SysConfig.getPropertyValue("sys.ch.name");
        String sysEnName = SysConfig.getPropertyValue("sys.en.name");
        String sysModel = SysConfig.getPropertyValue("sys.model");

        String inTabNames = SysConfig.getPropertyValue("in.tab.name");
        String outTabNames = SysConfig.getPropertyValue("out.tab.name");

        DataQuery dq = new DataQuery();

        List tableList = dq.getAllTables(inTabNames, outTabNames);
        for (int i = 0; i < tableList.size(); ++i) {
            TableInfo table = (TableInfo)tableList.get(i);
            Row row = sheet.createRow(i + 1);
            Cell cell = row.createCell(0);
            cell.setCellValue(sysChName);
            cell.setCellStyle(style1);

            cell = row.createCell(1);
            cell.setCellValue(sysEnName);
            cell.setCellStyle(style1);

            cell = row.createCell(2);
            cell.setCellValue(sysModel);
            cell.setCellStyle(style1);

            cell = row.createCell(3);
            cell.setCellValue(table.getEnName());
            cell.setCellStyle(style2);
            ExcelUtil.setCellLink(workbook, cell, table.getDirLink(), Short.parseShort(43+""));

            cell = row.createCell(4);
            cell.setCellValue(table.getChName());
            cell.setCellStyle(style2);

            cell = row.createCell(5);
            cell.setCellValue(table.getTableType());
            cell.setCellStyle(style2);

            cell = row.createCell(6);
            cell.setCellValue(table.getUnique());
            cell.setCellStyle(style2);

            cell = row.createCell(7);
            cell.setCellValue(table.getPrimKeys());
            cell.setCellStyle(style2);

            cell = row.createCell(8);
            cell.setCellValue(table.getTablespaceName());
            cell.setCellStyle(style2);

            cell = row.createCell(9);
            cell.setCellValue((table.getPartitionNames() != null) ? "Y" : "N");
            cell.setCellStyle(style2);

            Sheet tableSheet = makeTableColumnHeader(workbook, table);
            makeTableColumnBody(workbook, tableSheet, table);
        }

        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
    }

    public static Sheet makeTableColumnHeader(HSSFWorkbook workbook, TableInfo table)
    {
        HSSFCellStyle hstyle = ExcelUtil.setHeaderCellStyle(workbook);

        HSSFCellStyle bstyle2 = ExcelUtil.setBodyCellStyle(workbook, Short.parseShort(1+""));
        Sheet sheet = workbook.createSheet(table.getEnName());
        sheet.setDefaultColumnWidth(10);
        sheet.setDefaultRowHeightInPoints(20.0F);

        Row row0 = sheet.createRow(0);
        Cell cell0 = row0.createCell(0);
        cell0.setCellValue("返回目录");
        cell0.setCellStyle(bstyle2);
        ExcelUtil.setCellLink(workbook, cell0, "'表级信息'!A1", Short.parseShort(43+""));
        CellRangeAddress cellRangeAddress = new CellRangeAddress(0, 0, 0, 6);
        sheet.addMergedRegion(cellRangeAddress);
        ExcelUtil.setMergedRegionBorder(1, cellRangeAddress, workbook, sheet);

        Row row1 = sheet.createRow(1);
        Cell cell10 = row1.createCell(0);
        cell10.setCellValue("中文表名");
        cell10.setCellStyle(hstyle);
        Cell cell11 = row1.createCell(1);
        cell11.setCellValue(table.getChName());
        cell11.setCellStyle(bstyle2);
        cellRangeAddress = new CellRangeAddress(1, 1, 1, 6);
        sheet.addMergedRegion(cellRangeAddress);
        ExcelUtil.setMergedRegionBorder(1, cellRangeAddress, workbook, sheet);

        Row row2 = sheet.createRow(2);
        Cell cell20 = row2.createCell(0);
        cell20.setCellValue("英文表名");
        cell20.setCellStyle(hstyle);
        Cell cell21 = row2.createCell(1);
        cell21.setCellValue(table.getEnName());
        cell21.setCellStyle(bstyle2);
        cellRangeAddress = new CellRangeAddress(2, 2, 1, 6);
        sheet.addMergedRegion(cellRangeAddress);
        ExcelUtil.setMergedRegionBorder(1, cellRangeAddress, workbook, sheet);

        Row row3 = sheet.createRow(3);
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 6));
        Cell cell30 = row3.createCell(0);
        cell30.setCellValue("唯一索引");
        cell30.setCellStyle(hstyle);
        Cell cell31 = row3.createCell(1);
        if ((table.getPrimKeys() != null) && (!(table.getPrimKeys().equals(""))))
            cell31.setCellValue(table.getUnique() + "(" + table.getPrimKeys() + ")");
        else
            cell31.setCellValue("");

        cell31.setCellStyle(bstyle2);
        cellRangeAddress = new CellRangeAddress(3, 3, 1, 6);
        sheet.addMergedRegion(cellRangeAddress);
        ExcelUtil.setMergedRegionBorder(1, cellRangeAddress, workbook, sheet);

        Row row4 = sheet.createRow(4);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 6));
        Cell cell40 = row4.createCell(0);
        cell40.setCellValue("非唯一索引");
        cell40.setCellStyle(hstyle);
        Cell cell41 = row4.createCell(1);
        cell41.setCellValue(table.getNoUnique());
        cell41.setCellStyle(bstyle2);
        cellRangeAddress = new CellRangeAddress(4, 4, 1, 6);
        sheet.addMergedRegion(cellRangeAddress);
        ExcelUtil.setMergedRegionBorder(1, cellRangeAddress, workbook, sheet);

        Row row5 = sheet.createRow(5);
        sheet.addMergedRegion(new CellRangeAddress(5, 5, 1, 6));
        Cell cell50 = row5.createCell(0);
        cell50.setCellValue("外键");
        cell50.setCellStyle(hstyle);
        Cell cell51 = row5.createCell(1);
        cell51.setCellValue(table.getForKeys());
        cell51.setCellStyle(bstyle2);
        cellRangeAddress = new CellRangeAddress(5, 5, 1, 6);
        sheet.addMergedRegion(cellRangeAddress);
        ExcelUtil.setMergedRegionBorder(1, cellRangeAddress, workbook, sheet);

        Row row6 = sheet.createRow(6);
        sheet.addMergedRegion(new CellRangeAddress(6, 6, 1, 6));
        Cell cell60 = row6.createCell(0);
        cell60.setCellValue("分区字段");
        cell60.setCellStyle(hstyle);
        Cell cell61 = row6.createCell(1);
        cell61.setCellValue(table.getPartitionNames());
        cell61.setCellStyle(bstyle2);
        cellRangeAddress = new CellRangeAddress(6, 6, 1, 6);
        sheet.addMergedRegion(cellRangeAddress);
        ExcelUtil.setMergedRegionBorder(1, cellRangeAddress, workbook, sheet);

        return sheet;
    }

    public static void makeTableColumnBody(HSSFWorkbook workbook, Sheet sheet, TableInfo table)
    {
        HSSFCellStyle hstyle = ExcelUtil.setHeaderCellStyle(workbook);
        HSSFCellStyle bstyle1 = ExcelUtil.setBodyCellStyle(workbook, Short.parseShort(2+""));
        HSSFCellStyle bstyle2 = ExcelUtil.setBodyCellStyle(workbook, Short.parseShort(1+""));

        String[] title = { "字段序号", "字段英文名", "字段中文名", "数据类型", "默认值", "是否可空值", "备注" };
        Row row = sheet.createRow(7);
        for (int c = 0; c < title.length; ++c) {
            Cell cell = row.createCell(c);
            cell.setCellValue(title[c]);
            cell.setCellStyle(hstyle);
        }

        List columnList = table.getColumnList();

        for (int i = 0; i < columnList.size(); ++i) {
            ColumnInfo column = (ColumnInfo)columnList.get(i);
            row = sheet.createRow(i + 8);

            Cell cell = row.createCell(0);
            cell.setCellValue(column.getOrderNo());
            cell.setCellStyle(bstyle1);

            cell = row.createCell(1);
            cell.setCellValue(column.getEnName());
            cell.setCellStyle(bstyle2);

            String tableType = table.getTableType();

            cell = row.createCell(2);
            cell.setCellValue((tableType.equals("TABLE")) ? column.getChName() : "");
            cell.setCellStyle(bstyle2);
            cell = row.createCell(3);
            cell.setCellValue((tableType.equals("TABLE")) ? column.getDataType() : "");
            cell.setCellStyle(bstyle2);
            cell = row.createCell(4);
            cell.setCellValue((tableType.equals("TABLE")) ? column.getDefValue() : "");
            cell.setCellStyle(bstyle2);
            cell = row.createCell(5);
            cell.setCellValue((tableType.equals("TABLE")) ? column.getNullValidate() : "");
            cell.setCellStyle(bstyle1);
            cell = row.createCell(6);
            cell.setCellValue((tableType.equals("TABLE")) ? column.getMark() : "");
            cell.setCellStyle(bstyle2);
        }
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
    }

    public static void makeExcel(File file)
            throws IOException, SQLException
    {
        HSSFWorkbook workbook = new HSSFWorkbook();

        Sheet sheet = workbook.createSheet("表级信息");
        sheet.setDefaultColumnWidth(10);
        makeTableDirHeader(workbook, sheet);
        makeTableDirInfo(workbook, sheet);

        FileOutputStream out = new FileOutputStream(file);
        workbook.write(out);
        out.close();
    }
}