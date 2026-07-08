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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = {"com.chenzhen.mapper.threeMapper"}, sqlSessionFactoryRef = "sqlSessionFactory3")
public class mybatisDb3Config {

	@Value("${mybatis.threeMapper}")
	private String mybatisMapperLocations;

	@Autowired
	@Qualifier("threeDataSource")
	private DataSource ds;
	
	@Bean
	public SqlSessionFactory sqlSessionFactory3() throws Exception {
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(ds);
		factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mybatisMapperLocations));
		return factoryBean.getObject();
	}
	
	@Bean
	public SqlSessionTemplate sqlSessionTemplate3() throws Exception {
		SqlSessionTemplate template = new SqlSessionTemplate(sqlSessionFactory3());
		return template;
	}
}
