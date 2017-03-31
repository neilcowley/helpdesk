package com.baytouch.helpdesk.entities;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
	@NamedQuery(name = "findAllAdminUsers", query="SELECT u FROM AdminUser u WHERE u.role=:role"),
	@NamedQuery(name = "findAdminUserByEmail", query="SELECT u from AdminUser u WHERE u.email=:email"),
	@NamedQuery(name = "findAdminUserById" , query="SELECT u FROM AdminUser u WHERE u.id=:id"),
	@NamedQuery(name = "findAllHelpDeskNotification", query="SELECT u FROM AdminUser u WHERE u.helpDeskNotification=:helpDeskNotification")
})
public class AdminUser extends Account { 
	
	private boolean helpDeskNotification = false;
		
	public boolean getHelpDeskNotification() {
		return helpDeskNotification;
	}
	public void setHelpDeskNotification(boolean helpDeskNotification) {
		this.helpDeskNotification = helpDeskNotification;
	}
	
	public String readHelpdeskNotification(){
		return helpDeskNotification ? "Yes": "No";
	}
	
	public String iconHelpdeskNotification(){
		if(helpDeskNotification){
			return "<i class='fa fa-check fa-lg' style='color:green'></i>"; 
		}else{
			return "<i class='fa fa-times fa-lg' style='color:red'></i>"; 
		}
	}
	
	public String toString(){
		return getId().toString(); 
	}
}
