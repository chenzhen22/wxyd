package com.cyz.pojo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SQL 分页查询结果 VO。
 */
@Data
public class SqlQueryResultVO {

    private List<String> columns;
    private List<Map<String, Object>> rows;
    private long total;
    private int page;
    private int size;
    private int totalPages;
    private long elapsedMs;

}
