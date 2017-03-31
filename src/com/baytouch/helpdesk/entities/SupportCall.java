package com.baytouch.helpdesk.entities;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

// JPQL - Java Persistence Query Language 
@Entity
@Table(name = "supportCall") 
@NamedQueries({
	@NamedQuery(name = "findAllCalls", query="SELECT sc FROM SupportCall sc ORDER BY sc.dateCreated DESC"),
	@NamedQuery(name = "findOpenCalls", query="SELECT sc FROM SupportCall sc  WHERE sc.status IN :statusList ORDER BY sc.dateCreated DESC"), 
	@NamedQuery(name = "findCompanyCalls", query="SELECT sc FROM SupportCall sc WHERE sc.company.id=:companyId ORDER BY sc.dateCreated DESC"),
	@NamedQuery(name = "findCallsByStatus", query="SELECT sc FROM SupportCall sc WHERE sc.status=:status ORDER BY sc.dateCreated DESC"),
	@NamedQuery(name = "findCallsByAssignedByStatus", query="SELECT sc FROM SupportCall sc WHERE sc.assignedToId=:adminId and sc.status=:status ORDER BY sc.dateCreated DESC") 
})
@NamedEntityGraph(name = "graph.supportCall.children", 
	attributeNodes = {@NamedAttributeNode("attachinfo"), @NamedAttributeNode("callComments")}
)
@Indexed
public class SupportCall implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="SUPPORTCALL_ID")
	private Integer id;
	@OneToOne
	@IndexedEmbedded
	private Company company;
	@OneToOne
	private CompanyUser companyUser;
	@OneToOne
	private User createdByUser;	
	@OneToMany(
		mappedBy="supportCall",
		fetch = FetchType.LAZY,
		cascade=CascadeType.ALL,
		orphanRemoval=true)
	@OrderBy // NOTE!!! This pulls the set out of the table in the order the comments were created
	@IndexedEmbedded
	private Set<Comments> callComments; // = new LinkedHashSet<Comments>(0);
	@OneToMany(
		mappedBy="supportCall",
		fetch=FetchType.LAZY,
		cascade=CascadeType.ALL,
		orphanRemoval=true)
	@OrderBy
	@IndexedEmbedded
	private Set<AttachInfo> attachinfo;
	
	@Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
	private String callRefs;
	@Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
	private String subject;
	@Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
	private String callType;
	@Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
	private String status;
	@Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
	private String senderEmail;
	@Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
	private String senderName;
	@Lob
	@Column(columnDefinition="MEDIUMTEXT")
	@Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
	private String callDetails;
	
	private Date dateCreated;
	private Date dateCompleted;
	private Double timeTaken;
	private Integer assignedToId;
	private Integer createdByAdminId;
	
	public Integer getId(){
		return id;
	}
	public void setId(Integer id){
		this.id = id;
	}
	public Date getDateCreated(){
		if(dateCreated==null){
			dateCreated = new Date(); 
		}
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated){
		this.dateCreated = dateCreated;
	}
	public Date getDateCompleted(){
		return dateCompleted;
	}
	public void setDateCompleted(Date dateCompleted){
		this.dateCompleted = dateCompleted;
	}
	public Double getTimeTaken(){
		return timeTaken;
	}
	public void setTimeTaken(Double timeTaken) {
		this.timeTaken = timeTaken;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getCallType() {
		return callType;
	}
	public void setCallType(String callType) {
		this.callType = callType;
	}
	public String getCallDetails() {
		return callDetails;
	}
	public void setCallDetails(String callDetails) {
		this.callDetails = callDetails;
	}	
	public Set<Comments> getCallComments() {
		return this.callComments;
	}
	public void setCallComments(Set<Comments> callComments) {
		this.callComments = callComments;
	}
	public Set<AttachInfo> getAttachments() {
		return attachinfo;
	}
	public void setAttachments(Set<AttachInfo> attachinfo) {
		this.attachinfo = attachinfo;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Company getCompany(){
		return company;
	}
	public void setCompany(Company company) {
		this.company = company;
	}
	public CompanyUser getCompanyUser() {
		return companyUser;
	}
	public void setCompanyUser(CompanyUser companyUser) {
		this.companyUser = companyUser;
	}			
	public Integer getAssignedToId(){
		return assignedToId;
	}	
	public void setAssignedToId(Integer assignedToId) {
		this.assignedToId = assignedToId;
	}
	
	public String getCallRefs() {
		if(callRefs==null || callRefs.equals("")){
			SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy-HHmmss");
			Date date = new Date();
			callRefs = dateFormat.format(date);		
		//	Calendar cal = Calendar.getInstance();
		//	Date date =  cal.getTime();
		//	DateFormat dateFormat = new SimpleDateFormat("ddmmyy");
		//	DateFormat timeFormat = new SimpleDateFormat("HHmmss");
		//	callRefs = dateFormat.format(date) + "-" + timeFormat.format(cal.getTime()); 
		}
		return callRefs;
	}
	
	public String getCallRefs(Integer count) {
		getCallRefs(); 
		if(count!=null)
			callRefs += "-" + ++count ; 
		return callRefs;
	}
		
	public void setCallRefs(String callRefs) {
		this.callRefs = callRefs;
	}
	
	public String getSenderEmail() {
		return senderEmail;
	}

	public void setSenderEmail(String senderEmail) {
		this.senderEmail = senderEmail;
	}
		
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}	
	
	public String getSenderName(){
		return this.senderName; 
	}
		
	public User getCreatedByUser() {
		return createdByUser;
	}

	public void setCreatedByUser(User createdByUser) {
		this.createdByUser = createdByUser;
	}

	public Integer getCreatedByAdminId() {
		return createdByAdminId;
	}

	public void setCreatedByAdminId(Integer createdByAdminId) {
		this.createdByAdminId = createdByAdminId;
	}	
}

