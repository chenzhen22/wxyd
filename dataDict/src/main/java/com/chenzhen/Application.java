package com.chenzhen;

import com.chenzhen.excel.ExportExcel;
import com.chenzhen.util.SysConfig;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class Application {
    public static void main(String[] args)
            throws IOException, SQLException {
        SysConfig.getInstance();
        String filePath = SysConfig.getPropertyValue("sys.model") + "-数据字典.xls";
        long start = System.currentTimeMillis();
        System.out.println("正在生成《" + filePath + "》，请稍候。。。");

        File file = new File(filePath);
        if (!(file.exists())) {
            file.createNewFile();
        }

        ExportExcel.makeExcel(file);

        long end = System.currentTimeMillis();
        System.out.println("\n\n数据字典已生成在本目录下，请查看！");
        System.out.println("\n\n总花时间：" + ((end - start) / 1000L) + "秒\n\n\n");
    }
}