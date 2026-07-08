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
@MapperScan(basePackages = {"com.chenzhen.mapper.sit2atsMapper"}, sqlSessionFactoryRef = "sqlSessionFactory8")
public class mybatisDb8Config {

	@Value("${mybatis.sit2atsMapper}")
	private String mybatisMapperLocations;

	@Autowired
	@Qualifier("sit2atsDataSource")
	private DataSource ds;
	
	@Bean
	@Primary
	public SqlSessionFactory sqlSessionFactory8() throws Exception {
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(ds);
		factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mybatisMapperLocations));
		return factoryBean.getObject();
	}
	
	@Bean
	@Primary
	public SqlSessionTemplate sqlSessionTemplate8() throws Exception {
		SqlSessionTemplate template = new SqlSessionTemplate(sqlSessionFactory8());
		return template;
	}
}
