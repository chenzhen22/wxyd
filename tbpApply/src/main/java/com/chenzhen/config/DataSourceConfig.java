package com.chenzhen.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

	@Bean(name="firstDataSource")
	@Qualifier("firstDataSource")
	@ConfigurationProperties(prefix="spring.datasource.first")
	public DataSource firstDataSource() {
		return new HikariDataSource();
	}
	
	@Bean(name="secondDataSource")
	@Qualifier("secondDataSource")
	@ConfigurationProperties(prefix="spring.datasource.second")
	public DataSource secondDataSource() {
		return new HikariDataSource();
	}
	
	@Bean(name="threeDataSource")
	@Qualifier("threeDataSource")
	@ConfigurationProperties(prefix="spring.datasource.three")
	public DataSource threeDataSource() {
		return new HikariDataSource();
	}
	
	@Bean(name="fourDataSource")
	@Qualifier("fourDataSource")
	@ConfigurationProperties(prefix="spring.datasource.four")
	public DataSource fourDataSource() {
		return new HikariDataSource();
	}
	
	@Bean(name="sitcbsDataSource")
	@Qualifier("sitcbsDataSource")
	@ConfigurationProperties(prefix="spring.datasource.sitcbs")
	public DataSource sitcbsDataSource() {
		return new HikariDataSource();
	}
	
	@Bean(name="sitatsDataSource")
	@Qualifier("sitatsDataSource")
	@ConfigurationProperties(prefix="spring.datasource.sitats")
	public DataSource sitatsDataSource() {
		return new HikariDataSource();
	}
	
	@Bean(name="sit2cbsDataSource")
	@Qualifier("sit2cbsDataSource")
	@ConfigurationProperties(prefix="spring.datasource.sit2cbs")
	public DataSource sit2cbsDataSource() {
		return new HikariDataSource();
	}
	
	@Bean(name="sit2atsDataSource")
	@Qualifier("sit2atsDataSource")
	@ConfigurationProperties(prefix="spring.datasource.sit2ats")
	public DataSource sit2atsDataSource() {
		return new HikariDataSource();
	}
	
	@Bean(name="mysqlDataSource")
	@Qualifier("mysqlDataSource")
	@ConfigurationProperties(prefix="spring.datasource.mysql")
	public DataSource mysqlDataSource() {
		return DataSourceBuilder.create().build();
	}
}
