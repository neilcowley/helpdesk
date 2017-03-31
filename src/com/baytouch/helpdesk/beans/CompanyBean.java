package com.baytouch.helpdesk.beans;

import com.baytouch.helpdesk.entities.*;

import java.io.Serializable;
import java.text.*;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.*;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

@Named  //Named as CustomerBean in JSF
@ViewScoped //Exists for lifetime of a page, requires Serializable
public class CompanyBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final String SELECT_NEW_CONTACT = "comp_list_newUser_default";
	private static final String SELECT_SUPPORT_TYPE = "comp_list_supportType_default";
	private static final String CONTRACT = "supportType_contract";
	private static final String CREDIT = "supportType_credit";
	
	@PersistenceContext(unitName="helpdesk") 
	private EntityManager em;
	private Double creditsToAdd;
	private Integer companyId;
	private String keyword;

	private Company company;
	private CompanyUser companyUser;
	@Inject
	private NavigatorBean nb;
	@Inject
	private UsersBean usersBean;
	// TODO @Inject private CompanyDao cdao ; - NC 03/11/16 - Add this and use it to replace dao operations...
	private User newCompanyContact;
	private Account curUser;
	private String i;
	private Boolean editMode = false;
	private boolean isNew = false;
	private List<String> supportTypeList=null;
	private List<SupportCall> filteredCalls;
	private List<SupportCall> allCompCalls;
	
	public static String getSelectNewContact() {
		return SELECT_NEW_CONTACT;
	}

	public static String getSelectSupportType() {
		return SELECT_SUPPORT_TYPE;
	}
		
	@PostConstruct
	public void initBean(){	
		curUser = usersBean.getLoggedOnUser();	
		HttpServletRequest hsr = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		i = hsr.getParameter("i");
		supportTypeList = new ArrayList<String>();
		supportTypeList.add(CONTRACT); 
		supportTypeList.add(CREDIT); 
	}
		
	// GETTERS AND SETTERS =========================================
	@Transactional
	public Company getCompany(){
		if(company == null){
			if(i == null || i.isEmpty()){
				isNew = true;
				editMode = true;
				company = new Company();
				company.setCreditHistory(new LinkedHashSet<History>());	
				company.setCompanyUsers(new LinkedHashSet<CompanyUser>());		
			}else{ 
				//company = em.find(Company.class, Integer.parseInt(i));
				//company.getCreditHistory().size();
				//company.getCompanyUsers().size();				
				EntityGraph<Company> graph = em.createEntityGraph(Company.class);
				graph.addAttributeNodes("creditHistory");
				graph.addAttributeNodes("companyUsers");
				Map<String, Object> hints = new HashMap<String, Object>();
				hints.put("javax.persistence.loadgraph", graph);
				company = em.find(Company.class, Integer.parseInt(i), hints);	
				editMode = false;
			}
		}	
		return company;
	}
	
	public void setCompany(Company company) {
		this.company = company;
	}
	
	public Integer getCompanyId() {
		return companyId;
	}
	
	public boolean isNew() {
		return isNew;
	}
	
	public void setCompanyId(Integer companyId){
		this.companyId = companyId;
	}
	
	public String getKeyword(){
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	
	public Boolean getEditMode(){
		return editMode;
	}

	public void setEditMode(Boolean editMode) {
		this.editMode = editMode;
	}
	
	public Double getCreditsToAdd() {
		return creditsToAdd;
	}
	public void setCreditsToAdd(Double creditsToAdd) {
		this.creditsToAdd = creditsToAdd;
	}
		
	public CompanyUser getCompanyUser() {
		return companyUser;
	}

	public void setCompanyUser(CompanyUser companyUser) {
		this.companyUser = companyUser;
	}
	
	public User getNewCompanyContact() {
		return newCompanyContact;
	}

	public void setNewCompanyContact(User newCompanyContact) {
		this.newCompanyContact = newCompanyContact;
	}
	
	public List<String> getSupportTypeList() {
		return supportTypeList;
	}

	public void setSupportTypeList(List<String> supportTypeList) {
		this.supportTypeList = supportTypeList;
	}
	
	public List<SupportCall> getFilteredCalls() {
		return filteredCalls;
	}

	public void setFilteredCalls(List<SupportCall> filteredCalls) {
		this.filteredCalls = filteredCalls;
	}
	
	// END - GETTERS AND SETTERS =========================================

	/**
	 * Sets the editMode from the Company 'Edit' button
	 * @return
	 */
	public String edit(){
		setEditMode(true);
		return null;
	}
	
	public Boolean isEditable(){
		//String thisUser = curUser.getUserName();
		//String manager = usersBean.getManagerUserName();
		String role = curUser.getRole() ; 
		return !editMode && role.equals(UsersBean.ADMIN);
	}
	
	@Transactional //Adds transaction code for writing to db to this method so we don't have to worry about it.
	public String saveCompany(boolean isSaveAndClose){
		if(isSaveAndClose){
			em.merge(company);
			// System.out.println("Company - SAVE & CLOSE=" + nb.getPageFrom());
			return nb.getPageFrom() + "?faces-redirect=true" ;
		} else {
			company = em.merge(company);
			isNew = false;
			return null ;
		}
	}
	
	public List<Company> getCompanies(){
		TypedQuery<Company> tq;
		if(null == keyword || "".equals(keyword)) {
			//System.out.println("No keyword value to search by..")
			// tq = em.createQuery("SELECT p FROM Company p", Company.class); 
			tq = em.createNamedQuery("findAllCompanies",Company.class);
	    } else {
	    	// System.out.println("SEARCHING FOR KEYWORD: " + keyword);
    		// SEARCH ALL COLUMNS FOR A KEYWORD
    		tq = em.createQuery("SELECT c FROM Company c WHERE c.companyName like CONCAT('%', :keyword, '%')", Company.class);
    		// Insert the keyword parameter and return the search results
			tq.setParameter("keyword",keyword);
	    }
		return tq.getResultList();
	}
	
	public List<SupportCall> getCompanySupportCalls(){
		if(allCompCalls==null){
			TypedQuery<SupportCall> tq = em.createNamedQuery("findCompanyCalls",SupportCall.class);
			tq.setParameter("companyId",company.getId());
			allCompCalls =  tq.getResultList(); 
		}
		return allCompCalls; 
	}
			
	public void addCredits(){
		DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		// System.out.println(dateFormat.format(cal.getTime())); //2014/08/06 16:00:22
		String detail = curUser.getUserName() + " added " + creditsToAdd + " support units on " + dateFormat.format(cal.getTime()) + " at " + timeFormat.format(cal.getTime()); 
		History history = new History();
		history.setDetail(detail);
		history.setCompany(company);
		company.getCreditHistory().add(history);	
		company.updateCredits(creditsToAdd);
		setCreditsToAdd(0.0); // reset CreditsToAdd 
	}
	
	public void closeCredits(){
		// NOT NEEDED ???
	}
		
	public String openUserDialog(CompanyUser compUser){
	//	setEditCompUser(false);
		this.companyUser = compUser;
		return null;
	}
		
	/**
	 * Called by the Add newUserDialog in the Company document
	 */
	@Transactional 
	public String addUser(){
		if(newCompanyContact==null){
			// System.out.println("The company contact has not been selected");
			FacesMessage errorMessage = new FacesMessage("Please select a contact"); 
			errorMessage.setSeverity(FacesMessage.SEVERITY_ERROR);
			FacesContext.getCurrentInstance().addMessage(null,errorMessage);
			return(null);
		}else{
			companyUser = new CompanyUser();
			companyUser.setCompany(company);
			companyUser.setUser(newCompanyContact);
			company.getCompanyUsers().add(companyUser); // Required in order to display in the form after refresh
			return saveCompany(false);
		}
	}
	
	@Transactional
	public String removeCompanyUser(){
		if(companyUser != null){
			company.getCompanyUsers().remove(companyUser);
			em.remove(em.contains(companyUser) ? companyUser : em.merge(companyUser));
			saveCompany(false);
		}
		return null; 
	}
	
	/* TODO - IS THIS REQUIRED NC 20/09/16 - find a way of returning a list of users who have not already been selected as companyUsers.
	public List<User> getUnselectedUsers(){
		// TypedQuery<User> tq = em.createQuery("SELECT u FROM user u WHERE u not in (SELECT cu from CompanyUser cu WHERE cu.company.id=:companyId)", User.class);
		// TypedQuery<User> tq = em.createQuery("SELECT u FROM User u WHERE u in (SELECT cu from CompanyUser cu WHERE cu.company.id=:companyId)", User.class);
		// tq.setParameter("companyId", companyId);
		return udao.getAllUsers();
	}
	*/
	
	@Produces @Named 
	 public Converter getCompanyConverter(){
		return new Converter(){
			public Object getAsObject(FacesContext context, UIComponent component, String value) {    
				return em.find(Company.class, Integer.parseInt(value));
			}
	   
			public String getAsString(FacesContext context, UIComponent component, Object value) {  
				//System.out.println("getAsString=" + value);
				if(value==null){
					return ""; 
				}else{ 
					return ((Company)value).getId().toString();	
				}
			}
		};
	 }

	public void changeSupportType(){
		// System.out.println("Changing support Type to " + company.getTypeOfSupport());
	}
}
