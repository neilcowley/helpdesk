package com.baytouch.helpdesk.entities;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class AttachInfo {

	@Id
	@GeneratedValue (strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false)
	private Integer id;	
	@ContainedIn
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUPPORTCALL_ID", nullable = false)
	private SupportCall supportCall;
	
	@OneToOne(fetch = FetchType.LAZY,
		mappedBy="attachInfo",
		cascade=CascadeType.ALL
	)
	// @JoinColumn(name = "ATTACHDATA_ID", nullable = false)
	private AttachData attachData;	
	@Basic(fetch=FetchType.EAGER)
	@Field(name="fileName")
	private String fileName;
	@Basic(fetch=FetchType.EAGER)
	private String contentType;
	
	@Field
	@FieldBridge(impl = FileBridge.class)
	private String tmpFileName; 
		
	public Integer getId(){
		return id;
	}
	public void setId(Integer id){
		this.id=id;
	}
	public SupportCall getSupportCall(){
		return supportCall;
	}
	public void setSupportCall(SupportCall supportCall){
		this.supportCall = supportCall;
	}
	public String getFileName(){
		return fileName;
	}
	public void setFileName(String fileName){
		this.fileName = fileName;
	}
	public String getContentType(){	
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getTmpFileName() {
		return tmpFileName;
	}
	public void setTmpFileName(String tmpFileName) {
		this.tmpFileName = tmpFileName;
	}
	public AttachData getAttachData() {
		return attachData;
	}
	public void setAttachdata(AttachData attachdata) {
		this.attachData = attachdata;
	}	
}
