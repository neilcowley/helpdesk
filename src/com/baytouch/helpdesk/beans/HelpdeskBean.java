package com.baytouch.helpdesk.beans;

import com.baytouch.helpdesk.dao.SearchDao;
import com.baytouch.helpdesk.dao.SupportCallDao;
import com.baytouch.helpdesk.entities.Account;
import com.baytouch.helpdesk.entities.CompanyUser;
import com.baytouch.helpdesk.entities.SupportCall;
import java.io.*;
import java.util.*;

import javax.enterprise.inject.Produces;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.*;

/**
 * LATEST VERSION
 * @author Vostro-200
 */
@Named
@ViewScoped //@SessionScoped
public class HelpdeskBean implements Serializable{

	private static final long serialVersionUID = 1L;
	// private static final String DEFAULT_LOCALE = "en"; 
	
	@PersistenceContext(unitName="helpdesk")
	private EntityManager em;
	@Inject
	private SupportCallDao scdao;
	@Inject
	private UsersBean usersBean ; 
	@Inject 
	private SearchDao sdao ; 
	private List<SupportCall> allCalls;
	private List<SupportCall> filteredCalls;
		
	@Produces @Named
	String SUPPORT_CALL_DD_COMP_USER_DEFAULT = "[Please select a company then contact]"; 
	@Produces @Named
	String SUPPORT_CALL_DD_COMPANY_DEFAULT = "[Please select a company]"; 
		
	/**
	 * Returns a list of open calls based on the logged in user. If the User is Admin then all Open calls are 
	 * returned. If the user is company associated then only the calls applicable to the company are loaded.
	 * @return
	 */
	public List<SupportCall> getOpenCalls(){
		
		if(allCalls==null){
			EntityGraph<SupportCall> graph = em.createEntityGraph(SupportCall.class);
			graph.addAttributeNodes("company");
			graph.addAttributeNodes("companyUser");
			
			TypedQuery<SupportCall> tq = null;
			if(usersBean.isAdminUser()){
				tq = em.createNamedQuery("findOpenCalls",SupportCall.class);
				List<String> statusList = Arrays.asList("new","on_hold","in_progress","update","re_assign");
				tq.setParameter("statusList", statusList);
			}else{
				Account user = usersBean.getLoggedOnUser();
				CompanyUser compUser = scdao.lookupCompanyUser(user);
				if(compUser!= null){
					tq = em.createNamedQuery("findCompanyCalls",SupportCall.class);
					tq.setParameter("companyId", compUser.getCompany().getId());
				} else{
					return null;
				}
			}
			tq.setHint("javax.persistence.loadgraph", graph);
			allCalls = tq.getResultList();
		}
		return allCalls; 
	}
	
	/**
	 * Returns a list of all Open Support Calls or the search filter options...
	 * @return
	 */
	public List<SupportCall> getAllCalls(){
	
		String searchString = sdao.getSearchString() ; 
		if(searchString != null && !searchString.isEmpty()){
			return sdao.fullTextSearch();
		}else{
			if(allCalls==null){
				EntityGraph<SupportCall> graph = em.createEntityGraph(SupportCall.class);
				graph.addAttributeNodes("company");
				graph.addAttributeNodes("companyUser");
				// Map<String, Object> hints = new HashMap<String, Object>();
				//hints.put("javax.persistence.loadgraph", graph);
				TypedQuery<SupportCall> tq = em.createNamedQuery("findAllCalls",SupportCall.class);
				tq.setHint("javax.persistence.loadgraph", graph);
				return tq.getResultList();
			}else{
				return allCalls; 
			}
		}
	}
					
	public List<SupportCall> getFilteredCalls() {
		return filteredCalls;
	}

	public void setFilteredCalls(List<SupportCall> filteredCalls) {
		this.filteredCalls = filteredCalls;
	}

	public String CallReadMail(){
		System.out.println("*** START - Manual request to check for new Support Calls ***");
		scdao.readMail();
		System.out.println("*** FINISHED - Manual request to check for new Support Calls ***");
		return ""; 
	}
		
}
