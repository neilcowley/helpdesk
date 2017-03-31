package com.baytouch.helpdesk.entities;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name="domainskip") 
@NamedQueries({
	@NamedQuery(name = "findAllDomains", query="SELECT ds FROM domainskip ds"),
	@NamedQuery(name = "findDomain", query="SELECT ds FROM domainskip ds WHERE ds.domainName=:domainName")
})
public class DomainSkip implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="DOMAINSKIP_ID")
	private Integer id;
	private String domainName;
	private boolean active = false ; 
	private String createdBy ; 
	@Lob
	@Column(columnDefinition="TEXT")
	private String comments ; 
	@Lob
	@Column(columnDefinition="TEXT")
	private String history ; 
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getDomainName() {
		return domainName;
	}
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public String getHistory() {
		return history;
	}
	public void setHistory(String history) {
		this.history = history;
	} 
	public String readDomainSkipActive(){
		return active ? "Yes": "No";
	}
}
