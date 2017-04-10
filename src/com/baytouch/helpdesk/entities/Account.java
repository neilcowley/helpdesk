package com.baytouch.helpdesk.entities;

import java.util.Objects;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Account {
	
	private Integer id;
	private String firstName ; 
	private String lastName; 
	private String userName;
	private String passwd;
	private String email;
	private String role;
	private String language ;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer getId(){
		return id;
	}
	public void setId(Integer id){
		this.id = id;
	}
	public String getFirstName() {
	//	System.out.println("Returning firstName=" + firstName);
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
	//	System.out.println("Returning lastName=" + lastName);
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getUserName(){
		return userName;
	}
	public void setUserName(String userName){
		this.userName = userName;
	}
	public String getPasswd(){
		return passwd;
	}
	public void setPasswd(String passwd){
		this.passwd = passwd;
	}
	public String getEmail(){
		return email;
	}
	public void setEmail(String email){
		this.email = email;
	}
	public String getRole() {
		//System.out.println("username=" + userName + " role=" + role );
		return role;
	}
	public void setRole(String role) {
		//System.out.println("Setting roll for: " + userName + " role=" + role );
		this.role = role;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	@Override
	public boolean equals(Object obj) {
		
		if (obj == this) return true;
		
		if (!(obj instanceof Account)) {
			return false;
		}
		
		Account account = (Account) obj;
		if (email != null && account.email != null && Objects.equals(email, account.email)) 
			return true;
		
		return super.equals(obj);
	}
		
    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
	
}
