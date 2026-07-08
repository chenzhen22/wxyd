package com.chenzhen.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = {"com.chenzhen.mapper.sitatsMapper"}, sqlSessionFactoryRef = "sqlSessionFactory6")
public class mybatisDb6Config {

	@Value("${mybatis.sitatsMapper}")
	private String mybatisMapperLocations;

	@Autowired
	@Qualifier("sitatsDataSource")
	private DataSource ds;
	
	@Bean
	@Primary
	public SqlSessionFactory sqlSessionFactory6() throws Exception {
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(ds);
		factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mybatisMapperLocations));
		return factoryBean.getObject();
	}
	
	@Bean
	@Primary
	public SqlSessionTemplate sqlSessionTemplate6() throws Exception {
		SqlSessionTemplate template = new SqlSessionTemplate(sqlSessionFactory6());
		return template;
	}
}
