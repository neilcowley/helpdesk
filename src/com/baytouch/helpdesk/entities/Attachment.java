package com.baytouch.helpdesk.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

@Entity
public class Attachment {
	
	@Id
	@GeneratedValue (strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false)
	private Integer id;	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUPPORTCALL_ID", nullable = false)
	private SupportCall supportCall;	
	@Lob
	@Column
	@Basic(fetch=FetchType.LAZY)
	private byte[] fileData;
	@Basic(fetch=FetchType.EAGER)
	private String fileName;
	@Basic(fetch=FetchType.EAGER)
	private String contentType;
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
	public byte[] getFileData(){
		System.out.println("Getting fileData"); 
		return fileData;
	}
	public void setFileData(byte[] fileData){
		this.fileData = fileData;
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
}
