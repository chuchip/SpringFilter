package com.profesorp.filtros;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;

@Order(3)
public class CakesFilter implements Filter{	
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {		
		HttpServletResponse  myResponse= (HttpServletResponse) response;
		myResponse.addHeader("CAKE", "EATEN");	
		chain.doFilter(request, response);
	}

}
