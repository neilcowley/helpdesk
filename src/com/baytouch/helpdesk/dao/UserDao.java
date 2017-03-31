package com.baytouch.helpdesk.dao;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import com.baytouch.helpdesk.entities.Account;
import com.baytouch.helpdesk.entities.AdminUser;
import com.baytouch.helpdesk.entities.User;

@Named
@RequestScoped
public class UserDao{
	
	@PersistenceContext(unitName="helpdesk")
	private EntityManager emUser;
	@PersistenceContext(unitName="adminuser")
	private EntityManager emAdmin;
			
	public Account getUserByEmail(String email){
		List<Account> rtnList = new ArrayList<Account>(); 
		TypedQuery<User> findUser = emUser.createNamedQuery("findUserByEmail",User.class);
		findUser.setParameter("email", email);
		rtnList.addAll(findUser.getResultList()); 	
		if(rtnList.size()==0){
			rtnList.add(getAdminUserByEmail(email));
		}
		return rtnList.get(0); 
	}	
			
	public Account getAdminUserByEmail(String email){
		TypedQuery<AdminUser> findAdmin = emAdmin.createNamedQuery("findAdminUserByEmail",AdminUser.class);
		//TypedQuery<AdminUser> findAdmin = emAdmin.createQuery("SELECT u from AdminUser u WHERE u.email=:email", AdminUser.class); 
		findAdmin.setParameter("email", email);
		AdminUser aUser = null;
		if(findAdmin.getResultList().size()>0){
			aUser = findAdmin.getResultList().get(0);
		}
		return (Account) aUser ;
	}
	
	public List<AdminUser> getAllAdminHelpdeskNotifications(){
		TypedQuery<AdminUser> findAdmin = emAdmin.createNamedQuery("findAllHelpDeskNotification",AdminUser.class);
		// TypedQuery<AdminUser> tq = emAdmin.createQuery("SELECT u FROM AdminUser u WHERE u.helpDeskNotification=:helpDeskNotification", AdminUser.class); 
		findAdmin.setParameter("helpDeskNotification", true);
		return findAdmin.getResultList();
	}
			
	public List<Integer> getAllAdminUsersAsIds(){
		
		TypedQuery<AdminUser> findAdmin = emAdmin.createNamedQuery("findAllAdminUsers", AdminUser.class);
		List<AdminUser> auList = findAdmin.getResultList();
		// List<AdminUser> auList = getAllAdminUsers(); 
		List<Integer> idList = new ArrayList<Integer>(); 
		for(AdminUser au : auList){
			idList.add(au.getId()); 	
		}
		return idList ; 
	}
	
	public AdminUser getAdminUserById(Integer adminUserId){
		if(adminUserId!=null){ 
			//TypedQuery<AdminUser> tq = null ; 
			TypedQuery<AdminUser> findAdmin = emAdmin.createNamedQuery("findAdminUserById" , AdminUser.class); 
			//tq = emAdmin.createQuery("SELECT u FROM AdminUser u WHERE u.id=:id", AdminUser.class); 
			findAdmin.setParameter("id",adminUserId);
			if(findAdmin.getResultList().size()==1){
				return (AdminUser) findAdmin.getResultList().get(0);
			}
		}
		return null; 
	}
	
	public List<AdminUser> getAllAdminUsers(){
		TypedQuery<AdminUser> tq = emAdmin.createNamedQuery("findAllAdminUsers", AdminUser.class);
	//	TypedQuery<AdminUser> tq = emAdmin.createQuery("SELECT u FROM AdminUser u WHERE u.role=:role", AdminUser.class); 
		tq.setParameter("role","admin");
		return tq.getResultList();
	}
	
	/*
	NC 22/03/17 - these methods are now in the UsersBean class as they include the keyword search functionality which is front end
	
	public List<User> getAllUsers(){	
		return emUser.createNamedQuery("allUsers",User.class).getResultList();	
	}
	
	// TODO - NamedQuery doens't work even though its exactly the same code as the createQuery ?????
	public List<AdminUser> getAllAdminUsers(){
		TypedQuery<AdminUser> tq = emAdmin.createNamedQuery("findAllAdminUsers", AdminUser.class);
	//	TypedQuery<AdminUser> tq = emAdmin.createQuery("SELECT u FROM AdminUser u WHERE u.role=:role", AdminUser.class); 
		tq.setParameter("role","admin");
		return tq.getResultList();
	}
	 */
}

