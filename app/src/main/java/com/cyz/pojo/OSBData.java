package com.cyz.pojo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class OSBData {

    @ExcelProperty(value="error_type")
    private String error_type;


    @ExcelProperty(value="screen_name")
    private String screen_name;


    @ExcelProperty(value="api_name")
    private String api_name;


    @ExcelProperty(value="error_code")
    private String error_code;


    @ExcelProperty(value="error_reason")
    private String error_reason;

    @ExcelProperty(value="count")
    private String count;
}
