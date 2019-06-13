package com.profesorp.filtros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FiltrosApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(FiltrosApplication.class, args);
	}
	
	@Bean
	public FilterRegistrationBean<CakesFilter> cakesFilter()
	{
		FilterRegistrationBean<CakesFilter> registrationBean = new FilterRegistrationBean<>();				
		registrationBean.setFilter(new CakesFilter());
		registrationBean.addUrlPatterns("/cakes/*");
		return registrationBean;
	}
}
