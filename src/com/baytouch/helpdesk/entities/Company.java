package com.baytouch.helpdesk.entities;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@NamedQueries({
	@NamedQuery(name = "findAllCompanies", query="SELECT p FROM Company p"),
	@NamedQuery(name = "findCompaniesByCompanyUser", query="SELECT cu.company from CompanyUser cu WHERE cu.user.id=:userId")
})
@Indexed
public class Company implements Serializable {

	private static final long serialVersionUID = 1L;
		
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="COMPANY_ID")
	private Integer id;
	
	@OneToMany(
			mappedBy="company",
			fetch=FetchType.LAZY,
			cascade=CascadeType.ALL,
			orphanRemoval=true)
	@OrderBy
	private Set<History> creditHistory;
	
	// Map the Orders object by the customer,
	// cascade=CascadeType.ALL - saves/deletes/updates all changes to child objects
	@OneToMany(
		mappedBy="company",
		fetch=FetchType.LAZY,
		cascade=CascadeType.MERGE,
		orphanRemoval=true)
	@OrderBy
	private Set<CompanyUser> companyUsers;
	@Field
	private String companyName;
	private String companyCode;
	private String typeOfSupport;
	private Double credits;
	private Date dateCreated;
	
	//======= Getters and Setters =============
	
	public Integer getId(){
		return id;
	}
	public void setId(Integer id){
		this.id = id;
	}
	public String getCompanyName(){
		return companyName;
	}
	public void setCompanyName(String companyName){
		this.companyName = companyName;
	}
	public String getCompanyCode(){
		return companyCode;
	}
	public void setCompanyCode(String companyCode){
		this.companyCode = companyCode;
	}
	public String getTypeOfSupport(){
		return typeOfSupport;
	}
	public void setTypeOfSupport(String typeOfSupport){
		this.typeOfSupport = typeOfSupport;
	}
	public Double getCredits(){
		if(credits==null){
			credits=(double) 0; 
		}
		return credits;
	}
	public void setCredits(Double credits){
		this.credits = credits;
	}
	
	public Date getDateCreated(){
		if(dateCreated==null){
			dateCreated = new Date() ; 
		}
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated){
		this.dateCreated = dateCreated;
	}
	
	public Set<History> getCreditHistory(){
		return creditHistory;
	}
	
	public void setCreditHistory(Set<History> creditHistory){
		this.creditHistory = creditHistory;
	}

	public Set<CompanyUser> getCompanyUsers() {
		return companyUsers;
	}
	
	public void setCompanyUsers(Set<CompanyUser> companyUsers) {
		this.companyUsers = companyUsers;
	}
	
	public void updateCredits(Double creditsToAdd){
		credits += creditsToAdd; 
	}

	/**
	 * VERY IMPORTANT!!!! required for companyConverter otherwise the setter is not called.
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Company){
			Company exp = (Company)obj; 
		    if(this.id != null && exp.id != null && this.id == exp.id)
		        return true;
			
		}
		return super.equals(obj);
	}	
	
}
