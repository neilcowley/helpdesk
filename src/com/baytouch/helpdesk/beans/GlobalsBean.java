package com.baytouch.helpdesk.beans;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import com.baytouch.helpdesk.entities.Account;
import java.io.Serializable;

@Named
@SessionScoped
public class GlobalsBean implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String DEFAULT_LANG = "en" ;
	private String locale;
	@Inject
	private UsersBean uBean ; 
	

	public String getLocale(){
		if(locale==null){
			// Check to see what language preference the currently logged on user has...
			Account curUser = uBean.getLoggedOnUser();
			if(!curUser.getLanguage().equals("")){
				this.locale=curUser.getLanguage(); 
			}else{
				this.locale="";
			}
		}
		return locale;
	}

	public void setLocale(String locale) {	
//		System.out.println("Setting the Locale="+ locale );
		this.locale = locale;
		uBean.setLoggedOnUserLanguage(this.locale); 
	}
}
