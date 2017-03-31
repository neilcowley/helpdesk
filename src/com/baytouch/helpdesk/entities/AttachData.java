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
import javax.persistence.OneToOne;

@Entity
public class AttachData {

	@Id
	@GeneratedValue (strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false)
	private Integer id;	
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ATTACHINFO_ID", nullable = false)
	private AttachInfo attachInfo;	
	
	@Lob
	@Column(length = 20971520)
	@Basic(fetch=FetchType.LAZY)
	private byte[] fileData;
		
	public AttachInfo getAttachInfo(){
		return attachInfo;
	}
	public void setAttachInfo(AttachInfo attachinfo){
		this.attachInfo = attachinfo;
	}
	public byte[] getFileData(){
		return fileData;
	}
	public void setFileData(byte[] fileData){
		this.fileData = fileData;
	}
}
