package com.baytouch.helpdesk.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed 
public class Comments implements Comparable<Comments>, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue (strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false)
	private Integer id; 
	
	@ContainedIn
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUPPORTCALL_ID", nullable = false)
	private SupportCall supportCall;
	
	@Lob
	@Column(columnDefinition="MEDIUMTEXT")
	@Field
	private String comments;
	
	public SupportCall getSupportCall() {
		return this.supportCall;
	}
	
	public void setSupportCall(SupportCall supportCall) {
		this.supportCall = supportCall;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	@Override
	public int compareTo(Comments o) {
		// TODO Auto-generated method stub
		return 0;
	}
}
