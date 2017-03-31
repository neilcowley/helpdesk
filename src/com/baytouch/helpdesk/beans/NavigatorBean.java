package com.baytouch.helpdesk.beans;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;

@Named  //Named as CustomerBean in JSF
@SessionScoped //Exists for lifetime of a page, requires Serializable
public class NavigatorBean implements Serializable {

	private static String DEFAULT_PAGE = "/home.xhtml"; 
	private static final long serialVersionUID = 1L;
	private String pageFrom;

	public String getPageFrom(){
		String rtnVal = DEFAULT_PAGE; 
		if(pageFrom!=null){
			rtnVal = pageFrom;
		}
		return rtnVal; 
	}

	public void setPageFrom(String pageFrom) {
		this.pageFrom = pageFrom;
	}	
}