package com.profesorp.filtros;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class OtherFilter implements Filter{

	@Autowired
	SillyLog sillyLog;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest=null;
		
		httpRequest= (HttpServletRequest) request;
		HttpServletResponse  myResponse= (HttpServletResponse) response;
		sillyLog.debug("OtherFilter: URL"
				+ " called: "+httpRequest.getRequestURL().toString());
		
		if (myResponse.getHeader("PROFE")!=null)
			sillyLog.debug("OtherFilter: Header contains PROFE: "+myResponse.getHeader("PROFE"));		

		chain.doFilter(request, response);
	}

}
