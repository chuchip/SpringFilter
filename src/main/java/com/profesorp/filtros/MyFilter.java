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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class MyFilter implements Filter{

	@Autowired
	SillyLog sillyLog;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest=null;
		
		httpRequest= (HttpServletRequest) request;
		HttpServletResponse  myResponse= (HttpServletResponse) response;
		sillyLog.debug("Filter: URL"
				+ " called: "+httpRequest.getRequestURL().toString());

		
		if (httpRequest.getRequestURL().toString().endsWith("/one"))	{			
			myResponse.addHeader("PROFE", "FILTERED");						
			chain.doFilter(httpRequest, myResponse);
			return;
		}
		if (httpRequest.getRequestURL().toString().endsWith("/redirect"))	{			
			myResponse.addHeader("PROFE", "REDIRECTED");
			myResponse.sendRedirect("redirected");
			chain.doFilter(httpRequest, myResponse);
			return;
		}
		if (httpRequest.getRequestURL().toString().endsWith("/cancel"))	{			
			myResponse.addHeader("PROFE", "CANCEL");
			myResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			myResponse.getOutputStream().flush();
			myResponse.getOutputStream().println("-- Output by filter error --");
			chain.doFilter(httpRequest, myResponse);
			return;
		}

		chain.doFilter(request, response);
	}

}
