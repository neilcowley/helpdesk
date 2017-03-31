package com.baytouch.helpdesk.beans;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class GlobalsBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String locale;
	

	public String getLocale(){
	//	System.out.println("Getting the Locale="+ locale);
		if(locale==null){
			this.locale="";
		}
		return locale;
	}

	public void setLocale(String locale) {
	//	System.out.println("Setting the Locale="+ locale );
		this.locale = locale;
	}
	
	
	
}
