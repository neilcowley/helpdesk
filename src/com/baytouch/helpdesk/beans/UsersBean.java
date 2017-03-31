package com.baytouch.helpdesk.beans;
import com.baytouch.helpdesk.dao.UserDao;
import com.baytouch.helpdesk.entities.Account;
import com.baytouch.helpdesk.entities.AdminUser;
import com.baytouch.helpdesk.entities.CompanyUser;
import com.baytouch.helpdesk.entities.User;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import javax.faces.convert.Converter;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.*;
import javax.transaction.Transactional;

@Named
@ViewScoped
public class UsersBean implements Serializable{

	private static final long serialVersionUID = 1L;
	//private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\." +
	//	"[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*" +
	//	"(\\.[A-Za-z]{2,})$";
	public static final String ADMIN = "admin";
	public static final String USER = "user";
	public static final String ARCHIVED = "archived";
	private static final String PWCHANGE_YES = "Yes";
	private static final String PWCHANGE_NO = "No";	
		
	@PersistenceContext(unitName="helpdesk")
	private EntityManager emHelp;
	@PersistenceContext(unitName="adminuser")
	private EntityManager emAdmin;
	@Inject
	UserDao udao;
	@Inject
	private NavigatorBean nb;
	
	private Integer userId;
	private Account user;
	private Account manager;
	private Boolean editMode = true;
	private String newPassword;
	private String confirmNewPassword;
	private String managerUserName;
	private Account loggedOnUser;
	private List<String> roleList = null;
	private String passwordChange = PWCHANGE_NO;
	private Boolean isAdminUser;
	private boolean isNew = false;
	private String keyword;
//	private Pattern pattern;
//	private Matcher matcher;
	
	@PostConstruct
	public void initBean(){
		roleList = new ArrayList<String>();
		roleList.add(ADMIN);
		roleList.add(USER);
		roleList.add(ARCHIVED);
//		pattern = Pattern.compile(EMAIL_PATTERN);
	}
			
	public UsersBean(){
		Properties properties = new Properties();
		try{
			//from http://stackoverflow.com/questions/15972175/reading-properties-file-in-jsf2-0-which-can-work-in-war-also
			// System.out.println("Inside UsersBean #1");
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			// System.out.println("Inside UsersBean #2");
			properties.load(cl.getResourceAsStream("config.properties"));
			// System.out.println("Inside UsersBean #3");
			managerUserName = properties.getProperty("manager");	
			// System.out.println("Inside UsersBean #4");
		}catch(IOException e){
			// Do nothing ...
		} 
	}
	
	public Account getUser(){		
		if(user == null){
			if(userId == null){
				user = new User();
				// user.setRole(USER);
				passwordChange=PWCHANGE_YES;
				isNew = true;
			}else{
				user = emHelp.find(User.class, userId);
				if(user==null){
					user = emAdmin.find(AdminUser.class, userId);
				}
			}
		}	
		return user;		
	}
	
	/**
	 * Returns a list of all Users or a list of users based on a keyword search
	 * @return
	 */
	public List<User> getAllUsers(){	
		TypedQuery<User> tq;
		if(null == keyword || "".equals(keyword)) {
			//System.out.println("No keyword value to search by..")
			// tq = em.createQuery("SELECT p FROM Company p", Company.class); 
			tq = emHelp.createNamedQuery("allUsers",User.class);
	    } else {
	    	// System.out.println("SEARCHING FOR KEYWORD: " + keyword);
    		// SEARCH ALL COLUMNS FOR A KEYWORD
    		tq = emHelp.createQuery("SELECT u FROM User u WHERE u.userName like CONCAT('%', :keyword, '%')", User.class);
    		// Insert the keyword parameter and return the search results
			tq.setParameter("keyword",keyword);
	    }
		return tq.getResultList();
	}
	
	/**
	 * Returns a list of all Admin or a list of admin based on a keyword search
	 * @return
	 */
	public List<AdminUser> getAllAdminUsers(){
		TypedQuery<AdminUser> tq;
		if(null == keyword || "".equals(keyword)) {
			//System.out.println("No keyword value to search by..")
			// tq = em.createQuery("SELECT p FROM Company p", Company.class); 
			tq = emAdmin.createNamedQuery("findAllAdminUsers",AdminUser.class);
			tq.setParameter("role","admin");
	    } else {
	    	// System.out.println("SEARCHING FOR KEYWORD: " + keyword);
    		// SEARCH ALL COLUMNS FOR A KEYWORD
    		tq = emAdmin.createQuery("SELECT au FROM AdminUser au WHERE au.userName like CONCAT('%', :keyword, '%')", AdminUser.class);
    		// Insert the keyword parameter and return the search results
			tq.setParameter("keyword",keyword);
	    }
		return tq.getResultList();
	}	
	
	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	public List<String> getRoleList() {
		return roleList;
	}

	public void setRoleList(List<String> roleList) {
		this.roleList = roleList;
	}
		
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		editMode = false;
		this.userId = userId;
	}
	
	public String getKeyword(){
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
		
	private String hashPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {		
		//http://stackoverflow.com/questions/3103652/hash-string-via-sha-256-in-java
		MessageDigest md = MessageDigest.getInstance("SHA-256");	
		md.update(password.getBytes("UTF-8")); // Change this to "UTF-16" if needed			
		//http://stackoverflow.com/questions/19743851/base64-java-encode-and-decode-a-string
		return Base64.getEncoder().encodeToString(md.digest());		 
	}
			
	public String edit(){
		setEditMode(true);
		return null;
	}
		
	@Transactional
	public String saveUser(boolean isSaveAndClose) throws NoSuchAlgorithmException, UnsupportedEncodingException{	
		
		/*
		if(newPassword != null && !newPassword.isEmpty() && confirmNewPassword!= null && !confirmNewPassword.isEmpty()){
			if(newPassword.equals(confirmNewPassword)){
				user.setPasswd(hashPassword(newPassword));
			}else{								
				setError("userForm:np:newPassword","Passwords must match");
				setError("userForm:cnp:confirmNewPassword","Passwords must match");				
				return null;
			}			
		}
		*/
		
		if(isNew || passwordChange.equals(PWCHANGE_YES)){
			user.setPasswd(hashPassword(newPassword));
		}

		if(user instanceof AdminUser){
			user = emAdmin.merge(user);
		}else{
			user = emHelp.merge(user);	
		}
		
		if(isSaveAndClose){
			return nb.getPageFrom() + "?faces-redirect=true";
		}else{
			passwordChange=PWCHANGE_NO; 
			isNew=false;
			return null;
		}
	}
		
	private EntityManager getEntityManager(){
		if(user instanceof AdminUser){
			return emAdmin; 
		}else{
			return emHelp; 
		}
	}
	
	/**
	 * Calculates if the person accessing the user document has the write to delete it.
	 * @return
	 */
	public boolean dspDeleteButton(){
		// usersBean.editMode and usersBean.isAdminUser()  and !usersBean.new and user.role!='ADMIN'
		boolean rtnVal = false; 
		Account curUser = getLoggedOnUser() ; 
			
		if(!isNew &&
			editMode && 
			isAdminUser() && 
			curUser.getRole().equals(ADMIN) && 
			!curUser.getEmail().equals(user.getEmail()) 
		){
			rtnVal = true; 
		}
					
		return rtnVal ; 
	}
		
	@Transactional
	public String  deleteUser(){
		// Get the associated entityManager 
		EntityManager tmpEM = getEntityManager(); 
		
		// Check to see if the current user is associated with any companies..
		if(user instanceof User){
			
			// System.out.println("Check to see for associated companyUsers");
			List<CompanyUser> cuList = null ;
			TypedQuery<CompanyUser> tq = emHelp.createNamedQuery("findCompanyUserByUserId",CompanyUser.class);
			tq.setParameter("userId", user.getId());
			cuList = tq.getResultList();
			if(cuList.size()>0){
				//System.out.println("Beginning loop around Company Users...");
				for(CompanyUser cu : cuList){
					//System.out.println("Removeing: " + cu.getId() + " - " + cu.getFullName() );
					emHelp.remove(emHelp.contains(cu) ? cu : emHelp.merge(cu));
				}
			}
		}
		// Now remove the user from the database... 
		tmpEM.remove(tmpEM.contains(user) ? user : tmpEM.merge(user));			
		return nb.getPageFrom() + "?faces-redirect=true";
	}
			
	/**
	 * Returns the User object of the logged on user based on their email address log-on name.
	 * @return
	 */
	@Transactional
	public Account getLoggedOnUser(){
		if(loggedOnUser==null){
			String email = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
			Account acc = udao.getUserByEmail(email);  
			loggedOnUser = acc; 
		}
		return loggedOnUser;
	}
		
	public Boolean getEditMode() {
		return editMode;
	}

	public void setEditMode(Boolean editMode) {
		this.editMode = editMode;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getConfirmNewPassword() {
		return confirmNewPassword;
	}

	public void setConfirmNewPassword(String confirmNewPassword) {
		this.confirmNewPassword = confirmNewPassword;
	}
	
	public Boolean isManager(){
		return managerUserName.equalsIgnoreCase(getLoggedOnUser().getUserName());
	}

	public String getManagerUserName(){
		return managerUserName;
	}

	public void setManagerUserName(String managerUserName) {
		this.managerUserName = managerUserName;
	}

	@Transactional
	public Account getManager() {
		if(manager==null)
			manager = emHelp.find(User.class,managerUserName);
			if(manager==null){
				manager = emAdmin.find(AdminUser.class,managerUserName);
			}
		return manager;
	}

	public void setManager(User manager) {
		this.manager = manager;
	}	
	
	public String getPasswordChange() {
		return passwordChange;
	}

	public void setPasswordChange(String passwordChange) {
		this.passwordChange = passwordChange;
	}
	
	/**
	 * Checks the current users role to see if they are an admin user
	 * @return
	 */
	public Boolean isAdminUser(){
		if(isAdminUser==null){
			isAdminUser=false; 
			Account curUser = getLoggedOnUser();
			if( curUser.getRole().equals(ADMIN)){
				isAdminUser=true;
			}
		}	
		return isAdminUser; 
	}
	
	public String changePassword(){
		
		if(passwordChange.equals(PWCHANGE_YES)){
			passwordChange=PWCHANGE_NO;
		}else{
			passwordChange=PWCHANGE_YES;
		}	
		return null; 
	}
	
	public String dummyValidate(){
		return "";
	}
	
	public void updateUserName(){
		
	}
	
	@Produces @Named 
	 public Converter getUserConverter(){
		return new Converter(){
			public Object getAsObject(FacesContext context, UIComponent component, String value) {    
				return emHelp.find(User.class, Integer.parseInt(value));		
			}
	   
			public String getAsString(FacesContext context, UIComponent component, Object value) {  
				if(value==null){
					return ""; 
				}else{ 
					return ((User)value).getId().toString();	
				}
			}
		};
	}
	
	@Produces @Named 
	public Converter getAdminConverter(){
		return new Converter(){
			public Object getAsObject(FacesContext context, UIComponent component, String value) {    
				return emAdmin.find(AdminUser.class, Integer.parseInt(value));
			}
	   
			public String getAsString(FacesContext context, UIComponent component, Object value) {  
				if(value==null){
					return "";
				}else{ 
					return ((AdminUser)value).getId().toString();	
				}
			}
		};
	}	
	
	/* NOT REQUIRED - 24/10/16 - Using PasswordValidator now to validate passwords
	
	public void validatePassword(ComponentSystemEvent event) {

		FacesContext fc = FacesContext.getCurrentInstance();
		UIComponent components = event.getComponent();
		// get password
		UIInput uiInputPassword = (UIInput) components.findComponent("userForm:np:newPassword");
		String password = uiInputPassword.getLocalValue() == null ? "" : uiInputPassword.getLocalValue().toString();
		String passwordId = uiInputPassword.getClientId();
		// get confirm password
		UIInput uiInputConfirmPassword = (UIInput) components.findComponent("userForm:cnp:confirmNewPassword");
		String confirmPassword = uiInputConfirmPassword.getLocalValue() == null ? "" : uiInputConfirmPassword.getLocalValue().toString();
		String passwordConfirmId = uiInputConfirmPassword.getClientId();
		// Let required="true" do its job.
		if (password.isEmpty() || confirmPassword.isEmpty()) {
			return;
		}

		if (!password.equals(confirmPassword)){
			FacesMessage msg = new FacesMessage(" Password must match confirm password");
			msg.setSeverity(FacesMessage.SEVERITY_ERROR);
			fc.addMessage(passwordId, msg);
			fc.addMessage(passwordConfirmId, msg);
			fc.renderResponse();
			EditableValueHolder evhpw = (EditableValueHolder) uiInputPassword;
			evhpw.setValid(false);
			EditableValueHolder evhcpw = (EditableValueHolder) uiInputConfirmPassword;
			evhcpw.setValid(false);
		}
	}
	
	// NOT REQUIRED - 24/10/16 - The error setting is now done in the PasswordValidator class. 
	private void setError(String clientId, String errorMessage){
		//http://stackoverflow.com/questions/14378437/find-component-by-id-in-jsf
		//http://stackoverflow.com/questions/25529637/mark-inputtext-as-invalid-in-invoke-applications-phase
		FacesMessage fm = new FacesMessage(errorMessage);
		fm.setSeverity(FacesMessage.SEVERITY_ERROR);
		FacesContext.getCurrentInstance().addMessage(clientId, fm);
		UIComponent component = FacesContext.getCurrentInstance().getViewRoot().findComponent(clientId);
		EditableValueHolder evh = (EditableValueHolder) component;
		evh.setValid(false);
	}
	
	// NOTE REQUIRED ANYMORE
	@Transactional
	private void commitUser(){
		user = emHelp.merge(user);
	}
	*/
}
