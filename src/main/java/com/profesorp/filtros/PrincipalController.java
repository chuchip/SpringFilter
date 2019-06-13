package com.profesorp.filtros;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrincipalController {
	@Autowired
	SillyLog sillyLog;
	
	@GetMapping("*")
	public String entryOther(HttpServletRequest request,HttpServletResponse response)
	{	
		sillyLog.debug("In entryOther");
		if (response.getHeader("CAKE")!=null)
			sillyLog.debug("Header contains CAKE: "+response.getHeader("CAKE"));
		if (response.getHeader("PROFE")!=null)
			sillyLog.debug("Header contains PROFE: "+response.getHeader("PROFE"));
		
		return "returning by function entryOther\r\n"+
				sillyLog.getMessage();
	}
	@GetMapping(value={"/","one"})
	public String entryOne(HttpServletRequest request,HttpServletResponse response	)
	{
		sillyLog.debug("In entryOne");
		if (response.getHeader("PROFE")!=null)
		{
			sillyLog.debug("Header contains PROFE: "+response.getHeader("PROFE"));
			return entryTwo(response);				
		}
		return "returning by function entryOne\r\n"+
				sillyLog.getMessage();
	}
	@GetMapping("two")
	public String entryTwo(HttpServletResponse response)
	{
		sillyLog.debug("In entryTwo");
		if (response.getHeader("PROFE")!=null)
			sillyLog.debug("Header contains PROFE: "+response.getHeader("PROFE"));
		return "returning by function entryTwo\r\n"+
				sillyLog.getMessage();
	}
	@GetMapping("three")
	public String entryThree()
	{
		sillyLog.debug("In entryThree");
		return "returning by function entryThree\n"+
				sillyLog.getMessage();
	}
	@GetMapping("redirected")
	public String entryRedirect(HttpServletRequest request)
	{
		sillyLog.debug("In redirected");
		return "returning by function entryRedirect\n"+
				sillyLog.getMessage();
	}
}
