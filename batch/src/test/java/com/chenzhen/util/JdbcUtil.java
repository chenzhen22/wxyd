package com.chenzhen.util;


import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class JdbcUtil {
	private static DataSource dataSource;
	static {
		InputStream is = JdbcUtil.class.getClassLoader().getResourceAsStream("db.properties");
		Properties properties = new Properties();
		try {
			if(null == is ) {
				String reportModelFile = "resources/db.properties";
				ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
				Resource[] resources = resolver.getResources(reportModelFile);
				Resource resource = resources[0];
				is = resource.getInputStream();
			}
			properties.load(is);
			dataSource = DruidDataSourceFactory.createDataSource(properties);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}
	
	public static void close(Connection conn, Statement st) {
		if(conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		if(st != null) {
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
