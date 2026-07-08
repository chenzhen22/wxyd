package com.chenzhen.util;

import com.alibaba.druid.pool.DruidDataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class JDBCUtil {
    private static DataSource dataSource;
    private Connection con;

    public JDBCUtil() {
        SysConfig.getInstance();
        Properties properties = new Properties();
        String url = SysConfig.getPropertyValue("jdbc.url");
        String username = SysConfig.getPropertyValue("jdbc.username");
        String password = SysConfig.getPropertyValue("jdbc.password");
        String driverClassName = SysConfig.getPropertyValue("jdbc.driverClassName");
        properties.setProperty("url", url);
        properties.setProperty("username", username);
        properties.setProperty("password", password);
        properties.setProperty("driverClassName", driverClassName);
        try {
            dataSource = DruidDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            System.out.println("数据库连接失败!" + e.getMessage());
            System.exit(1);
        }
    }

    public static void close(PreparedStatement pst, ResultSet rs, Connection conn) throws SQLException {
        if (pst != null)
            pst.close();

        if (rs != null)
            rs.close();

        if (conn != null)
            conn.close();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection() throws SQLException {
        this.con.close();
    }

    protected void finalize() throws Throwable {
        super.finalize();
        this.con.close();
    }
}