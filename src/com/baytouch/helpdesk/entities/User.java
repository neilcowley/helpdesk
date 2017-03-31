package com.baytouch.helpdesk.entities;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
	@NamedQuery(name = "allUsers", query="SELECT u FROM User u"),
	@NamedQuery(name = "allAdminUsers", query="SELECT u FROM User u WHERE u.role=:role"),
	@NamedQuery(name = "findUserByEmail", query="SELECT u from User u WHERE u.email=:email")
})
public class User extends Account {
	
}
