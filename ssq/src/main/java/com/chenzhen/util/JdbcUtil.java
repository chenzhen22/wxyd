package com.chenzhen.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class JdbcUtil {

    @Autowired
    @Qualifier("mysqlDataSource")
    private static DataSource dataSource;


    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close(Connection conn, Statement st) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
