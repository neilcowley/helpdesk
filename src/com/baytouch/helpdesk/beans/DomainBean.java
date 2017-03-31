package com.baytouch.helpdesk.beans;

import com.baytouch.helpdesk.entities.Account;
import com.baytouch.helpdesk.entities.DomainSkip;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

@Named
@ViewScoped
public class DomainBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@PersistenceContext(unitName = "helpdesk")
	private EntityManager em;
	@Inject
	private UsersBean usersBean;
	@Inject
	private NavigatorBean nb;
	private DomainSkip domainSkip ; 
	private String keyword;
	private boolean isNew=false;
	private boolean editMode=false; 
	private Account curUser; 
	private String i;
	private List<DomainSkip> allDomains ; 
	private Map<Long, Boolean> checked = new HashMap<Long, Boolean>();
	
	
	@PostConstruct
	public void initBean(){
		HttpServletRequest hsr = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		i = hsr.getParameter("i");
		curUser = usersBean.getLoggedOnUser();
	}
		
	@Transactional
	public DomainSkip getDomain(){
		if (domainSkip == null) {
			if (i == null || i.isEmpty()) {
				isNew = true;
				editMode=true; 
				domainSkip = new DomainSkip();
				domainSkip.setCreatedBy(curUser.getUserName());
			} else {
				isNew = false;
				editMode = false;
				domainSkip = em.find(DomainSkip.class, Integer.parseInt(i));
			}
		}
		return domainSkip;
	}
		
	public List<DomainSkip> getAllDomains(){
		TypedQuery<DomainSkip> tq;
		if(null == keyword || "".equals(keyword)) {
			tq = em.createNamedQuery("findAllDomains",DomainSkip.class);
	    } else {
	    	// System.out.println("SEARCHING FOR KEYWORD: " + keyword);
    		// SEARCH ALL COLUMNS FOR A KEYWORD
    		tq = em.createQuery("SELECT ds FROM DomainSkip ds WHERE ds.domainName like CONCAT('%', :keyword, '%')", DomainSkip.class);
    		// Insert the keyword parameter and return the search results
			tq.setParameter("keyword",keyword);
	    }
		allDomains = tq.getResultList();
		return allDomains;
	}
	
	public String edit(){
		setEditMode(true);
		return null;
	}
	
	@Transactional
	public String saveDomain(boolean isSaveAndClose){
		
		if(isNew){
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
			Date date = new Date();
			String dateCreated = dateFormat.format(date);		
			domainSkip.setHistory(curUser.getUserName() + " created this rule on: " + dateCreated);
			domainSkip.setCreatedBy(curUser.getUserName());
		}
		
		if(isSaveAndClose){
			em.merge(domainSkip);
			return nb.getPageFrom() + "?faces-redirect=true";
		}else{
			domainSkip = em.merge(domainSkip);
			isNew=false;
			return null;
		}
	}
	
	@Transactional
	public String deleteDomain(){
		em.remove(em.contains(domainSkip) ? domainSkip : em.merge(domainSkip));
		return nb.getPageFrom() + "?faces-redirect=true";
	}
	
	public boolean isNew() {
		return isNew;
	}
	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}
	public boolean isEditMode() {
		return editMode;
	}
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}
		
	public String getActiveStatus(DomainSkip ds ){
		if(ds.isActive()){
			return "<i class='fa fa-check fa-lg' style='color:green'></i>"; 
		}else{
			return "<i class='fa fa-times fa-lg' style='color:red'></i>"; 
		}
	}
	
	public Map<Long, Boolean> getChecked() {
		return checked;
	}

	public void setChecked(Map<Long, Boolean> checked) {
		this.checked = checked;
	}
		
	@Transactional
	/**
	 * Changes the status of the selected domains from active to inactive 
	 */
	public void toggleSelectedDomains(){
		for(DomainSkip ds : allDomains){
			if (checked.get(ds.getId())) {
                // Entity is checked. Do your thing here.
				ds.setActive(ds.isActive()==true? false : true );
				em.merge(ds);
            }
		}
		// Clear the hashMap
		checked = new HashMap<Long, Boolean>();		
	}
	
	@Produces @Named 
	public Converter getDomainSkipConverter(){
		return new Converter(){
			public Object getAsObject(FacesContext context, UIComponent component, String value) {    
				return em.find(DomainSkip.class, Integer.parseInt(value));
			}
	   
			public String getAsString(FacesContext context, UIComponent component, Object value) {  
				//System.out.println("getAsString=" + value);
				if(value==null){
					return ""; 
				}else{ 
					return ((DomainSkip)value).getId().toString();	
				}
			}
		};
	}
}
