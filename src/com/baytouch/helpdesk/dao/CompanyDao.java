package com.baytouch.helpdesk.dao;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import com.baytouch.helpdesk.entities.Company;

@Named
@RequestScoped
public class CompanyDao {

	@PersistenceContext(unitName="helpdesk")
	private EntityManager emHelp;
	
	public List<Company> getAllCompanies(){
		TypedQuery<Company> tq;
		tq = emHelp.createNamedQuery("findAllCompanies",Company.class);
		return tq.getResultList();	
	}
	
	public List<Company> getCompanyByName(String keyword){
		TypedQuery<Company> tq;
    	// System.out.println("SEARCHING FOR KEYWORD: " + keyword);
		// SEARCH ALL COLUMNS FOR A KEYWORD
		tq = emHelp.createQuery("SELECT c FROM Company c WHERE c.companyName like CONCAT('%', :keyword, '%')", Company.class);
		// Insert the keyword parameter and return the search results
		tq.setParameter("keyword",keyword);
		return tq.getResultList();	
	}
	
	
}
