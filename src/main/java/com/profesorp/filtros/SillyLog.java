package com.profesorp.filtros;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;


@Component
@Scope(value=org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST,proxyMode=ScopedProxyMode.TARGET_CLASS)
public class SillyLog {
	private static final Logger log = LoggerFactory.getLogger(SillyLog.class);

	int secuence;
	String uuid;
	String message;
	
	public SillyLog()
	{
		uuid=UUID.randomUUID().toString();
		message="";
		secuence=1;
	}
	public void debug(String msg)
	{
		msg="SillyLog: "+uuid+"/"+secuence+" "+msg;
		
		System.out.println(msg);
		secuence++;
		message+=msg+"\n\r";
	}
	public String getMessage()
	{
		return message;
	}
	public void resetSecuencia()
	{
		secuence=1;
		message="";
	}
}
