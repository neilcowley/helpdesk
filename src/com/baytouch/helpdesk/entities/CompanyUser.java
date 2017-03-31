package com.baytouch.helpdesk.entities;

import java.io.Serializable;
import javax.persistence.*;

@Entity
@NamedQueries({
	@NamedQuery(name = "findCompanyUser", query="SELECT cu from CompanyUser cu WHERE cu.user.id=:userId"),
	@NamedQuery(name = "findCompanyUserByCompanyId" , query="SELECT cu from CompanyUser cu WHERE cu.company.id=:companyId"),
	@NamedQuery(name = "findCompanyUserByUserId" , query="SELECT cu from CompanyUser cu WHERE cu.user.id=:userId"),
	@NamedQuery(name="findCompanyUserByCompAndUserId",
    query="SELECT cu " +
          "FROM CompanyUser cu " +
          "WHERE cu.user.id = :userId AND " +
          "      cu.company.id = :companyId")
})
public class CompanyUser implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(unique = true, nullable = false)
	private Integer id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "COMPANY_ID", nullable = false)
	private Company company;
	
	@OneToOne
	private User user;
	
	// GETTERS AND SETTERS ===============================
		
	public Integer getId(){
		return id;
	}
	public void setId(Integer id){
		this.id = id;
	}	
	public Company getCompany(){
		return company;
	}
	public void setCompany(Company company){
		this.company = company;
	}
	public User getUser(){
		return user;
	}
	public void setUser(User user){
		this.user = user;
	}
	
	/**
	 * Required for selection lists to return the users full name without having to go through the user object
	 * @return
	 */
	public String getFullName(){
		return user.getUserName();
	}
	
	/**
	 * VERY IMPORTANT!!!! Required for companyConverter otherwise the setter is not called.
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof CompanyUser){
			CompanyUser exp = (CompanyUser)obj; 
		    if(this.id != null && exp.id != null && this.id == exp.id)
		        return true;
		}
		return super.equals(obj);
	}	

}
