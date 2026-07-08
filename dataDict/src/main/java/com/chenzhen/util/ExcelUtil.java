package com.chenzhen.util;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

public class ExcelUtil
{
    static HSSFCellStyle headerStyle = null;
    static HSSFCellStyle cellStyle = null;
    static HSSFCellStyle linkStyle = null;

    public static HSSFCellStyle setHeaderCellStyle(HSSFWorkbook workbook)
    {
        if (headerStyle == null) {
            headerStyle = workbook.createCellStyle();
        }

        headerStyle.setFillForegroundColor(Short.parseShort(24+""));
        headerStyle.setFillPattern(Short.parseShort(1+""));
        headerStyle.setAlignment(Short.parseShort(2+""));
        headerStyle.setVerticalAlignment(Short.parseShort(2+""));

        setBorder(headerStyle);
        return headerStyle;
    }

    public static HSSFCellStyle setBodyCellStyle(HSSFWorkbook workbook, short align)
    {
        if (cellStyle == null)
            cellStyle = workbook.createCellStyle();

        cellStyle.setFillForegroundColor(Short.parseShort(43+""));
        cellStyle.setFillPattern(Short.parseShort(1+""));
        cellStyle.setAlignment(align);
        cellStyle.setVerticalAlignment(Short.parseShort(1+""));

        setBorder(cellStyle);
        return cellStyle;
    }

    public static Font setHeaderFont(HSSFWorkbook workbook) {
        HSSFFont font = workbook.createFont();
        font.setColor(Short.parseShort(9+""));
        font.setFontHeightInPoints(Short.parseShort(10+""));
        font.setBoldweight(Short.parseShort(700+""));
        return font;
    }

    public static Font setBodyFont(HSSFWorkbook workbook) {
        HSSFFont font = workbook.createFont();
        font.setBoldweight(Short.parseShort(400+""));
        return font;
    }

    public static void setBorder(HSSFCellStyle style) {
        style.setBorderBottom(Short.parseShort(1+""));
        style.setBorderLeft(Short.parseShort(1+""));
        style.setBorderRight(Short.parseShort(1+""));
        style.setBorderTop(Short.parseShort(1+""));
    }

    public static Font setLinkFont(HSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setUnderline(Byte.parseByte("1"));
        font.setColor(IndexedColors.BLUE.index);
        return font;
    }

    public static void setCellLink(HSSFWorkbook workbook, Cell cell, String linkAddr, short index)
    {
        CreationHelper createHelper = workbook.getCreationHelper();
        Hyperlink link = createHelper.createHyperlink(2);
        link.setAddress(linkAddr);
        cell.setHyperlink(link);

        if (linkStyle == null)
            linkStyle = workbook.createCellStyle();

        setBorder(linkStyle);
        linkStyle.setFillForegroundColor(index);
        linkStyle.setFillPattern(Short.parseShort(1+""));
        linkStyle.setAlignment(Short.parseShort(1+""));
        linkStyle.setVerticalAlignment(Short.parseShort(1+""));
        Font font = setLinkFont(workbook);
        linkStyle.setFont(font);
        cell.setCellStyle(linkStyle);
    }

    public static void setMergedRegionBorder(int border, CellRangeAddress cellRangeAddress, HSSFWorkbook workbook, Sheet sheet) {
        RegionUtil.setBorderBottom(1, cellRangeAddress, sheet, workbook);
        RegionUtil.setBorderTop(1, cellRangeAddress, sheet, workbook);
        RegionUtil.setBorderLeft(1, cellRangeAddress, sheet, workbook);
        RegionUtil.setBorderRight(1, cellRangeAddress, sheet, workbook);
    }
}