package com.cyz.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.cyz.pojo.SqlQueryResultVO;

import javax.sql.DataSource;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SQL 查询服务 — 支持 SELECT 类语句分页查询 + CSV/Excel 导出。
 * <p>
 * 复用小泽助手自身的数据源（mysqlDataSource），自动检测数据库类型以选择分页语法。
 * 出于安全考虑，仅允许 SELECT / SHOW / DESC / EXPLAIN / WITH 单条语句，且只能在管理员权限下调用。
 * </p>
 */
@Service
public class SqlQueryService {

    private static final Pattern SELECT_PATTERN =
            Pattern.compile("^\\s*(SELECT|SHOW|DESC|DESCRIBE|EXPLAIN|WITH)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern MULTI_STMT_PATTERN =
            Pattern.compile(";\\s*(SELECT|SHOW|DESC|DESCRIBE|EXPLAIN|WITH|INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE)\\b",
                    Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate jdbcTemplate;
    private final String databaseType;

    public SqlQueryService(@Qualifier("mysqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.databaseType = detectDatabase(dataSource);
    }

    /**
     * 获取当前数据库所有表名。
     */
    public List<String> getTables() {
        List<String> tables = new ArrayList<>();
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            if ("mysql".equals(databaseType) || "h2".equals(databaseType)) {
                String catalog = conn.getCatalog();
                try (java.sql.ResultSet rs = meta.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                    while (rs.next()) {
                        String name = rs.getString("TABLE_NAME");
                        if (name != null) tables.add(name);
                    }
                }
            } else {
                try (java.sql.ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
                    while (rs.next()) {
                        String name = rs.getString("TABLE_NAME");
                        if (name != null) tables.add(name);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取表列表失败: " + e.getMessage());
        }
        Collections.sort(tables);
        return tables;
    }

    /**
     * 检测数据库类型。
     */
    private String detectDatabase(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String name = meta.getDatabaseProductName().toLowerCase();
            if (name.contains("mysql")) return "mysql";
            if (name.contains("h2")) return "h2";
            if (name.contains("dameng") || name.contains("dm")) return "dameng";
            return "other";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 校验 SQL 是否只包含 SELECT 类语句（单条）。
     */
    public void validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 语句不能为空");
        }
        String trimmed = trimEndSemicolon(sql);
        if (!SELECT_PATTERN.matcher(trimmed).find()) {
            throw new IllegalArgumentException("仅允许 SELECT / SHOW / DESC / EXPLAIN / WITH 语句");
        }
        if (MULTI_STMT_PATTERN.matcher(trimmed).find()) {
            throw new IllegalArgumentException("不允许执行多条 SQL 语句");
        }
    }

    /**
     * 执行分页查询。
     */
    public SqlQueryResultVO query(String sql, int page, int size) {
        validateSql(sql);
        long start = System.currentTimeMillis();

        if (size <= 0) {
            throw new IllegalArgumentException("每页条数必须大于 0");
        }

        String trimmed = trimEndSemicolon(sql);

        // 计算总条数
        String countSql = "SELECT COUNT(*) AS __total__ FROM (" + trimmed + ") __t__";
        long total = jdbcTemplate.queryForObject(countSql, Long.class);

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        if (page < 1) page = 1;
        if (totalPages > 0 && page > totalPages) page = totalPages;

        int offset = (page - 1) * size;
        String pageSql = buildPageSql(trimmed, offset, size);
        List<Map<String, Object>> rows = jdbcTemplate.query(pageSql, new ColumnMapRowMapper());

        // 提取列名（保持顺序）
        List<String> columns = new ArrayList<>();
        if (!rows.isEmpty()) {
            columns.addAll(rows.get(0).keySet());
        }

        long elapsed = System.currentTimeMillis() - start;

        SqlQueryResultVO result = new SqlQueryResultVO();
        result.setColumns(columns);
        result.setRows(rows);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages(totalPages);
        result.setElapsedMs(elapsed);
        return result;
    }

    /**
     * 根据数据库类型构建分页 SQL。
     */
    private String buildPageSql(String originalSql, int offset, int size) {
        if ("mysql".equals(databaseType) || "h2".equals(databaseType)) {
            return "SELECT * FROM (" + originalSql + ") __t__ LIMIT " + size + " OFFSET " + offset;
        } else {
            int endRow = offset + size;
            return "SELECT * FROM (SELECT __t__.*, ROWNUM AS __rn__ FROM (" + originalSql
                    + ") __t__ WHERE ROWNUM <= " + endRow + ") __sub__ WHERE __rn__ > " + offset;
        }
    }

    private String trimEndSemicolon(String sql) {
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /**
     * 导出 CSV（带 UTF-8 BOM）。
     */
    public void exportCsv(String sql, OutputStream outputStream) throws Exception {
        validateSql(sql);
        String trimmed = trimEndSemicolon(sql);
        List<Map<String, Object>> rows = jdbcTemplate.query(trimmed, new ColumnMapRowMapper());

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        try {
            writer.write('﻿'); // UTF-8 BOM，Excel 打开中文不乱码

            if (rows.isEmpty()) {
                writer.write("(无数据)\n");
                writer.flush();
                return;
            }

            List<String> columns = new ArrayList<>(rows.get(0).keySet());
            writeCsvRow(writer, columns);

            for (Map<String, Object> row : rows) {
                List<String> values = new ArrayList<>(columns.size());
                for (String col : columns) {
                    Object val = row.get(col);
                    values.add(val != null ? val.toString() : "");
                }
                writeCsvRow(writer, values);
            }
            writer.flush();
        } finally {
            // 不要关闭 writer，以免关闭调用方的 OutputStream
        }
    }

    private void writeCsvRow(OutputStreamWriter writer, List<String> values) throws Exception {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) writer.write(',');
            String val = values.get(i);
            if (val != null) {
                if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
                    writer.write('"');
                    writer.write(val.replace("\"", "\"\""));
                    writer.write('"');
                } else {
                    writer.write(val);
                }
            }
        }
        writer.write('\n');
    }

    /**
     * 导出 Excel (xlsx)，使用 SXSSFWorkbook 流式写入。
     */
    public void exportExcel(String sql, OutputStream outputStream) throws Exception {
        validateSql(sql);
        String trimmed = trimEndSemicolon(sql);
        List<Map<String, Object>> rows = jdbcTemplate.query(trimmed, new ColumnMapRowMapper());

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("查询结果");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            if (rows.isEmpty()) {
                Row emptyRow = sheet.createRow(0);
                Cell cell = emptyRow.createCell(0);
                cell.setCellValue("(无数据)");
                workbook.write(outputStream);
                outputStream.flush();
                return;
            }

            List<String> columns = new ArrayList<>(rows.get(0).keySet());
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < rows.size(); i++) {
                Row dataRow = sheet.createRow(i + 1);
                Map<String, Object> rowData = rows.get(i);
                for (int j = 0; j < columns.size(); j++) {
                    Cell cell = dataRow.createCell(j);
                    Object val = rowData.get(columns.get(j));
                    if (val != null) {
                        cell.setCellValue(val.toString());
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 256 * 8) {
                    sheet.setColumnWidth(i, 256 * 8);
                }
            }

            workbook.write(outputStream);
            outputStream.flush();
        } finally {
            workbook.dispose();
        }
    }
}
