package com.baytouch.helpdesk.validators;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.el.ValueExpression;
import javax.enterprise.inject.Model;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;

import com.baytouch.helpdesk.beans.GlobalsBean;
import com.baytouch.helpdesk.beans.UsersBean;
import com.baytouch.helpdesk.dao.UserDao;
import com.baytouch.helpdesk.entities.Account;

@Model
@FacesValidator("EmailValidator")
public class EmailValidator implements Validator, Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\." +
			"[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*" +
			"(\\.[A-Za-z]{2,})$";

	@Inject
	UserDao udao; 
	@Inject
	GlobalsBean gbean ; 
	private Pattern pattern;
	private Matcher matcher;

	public EmailValidator(){
		pattern = Pattern.compile(EMAIL_PATTERN);
	}

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {		
		
		boolean isError = false; 
		String msgVal ="";
		matcher = pattern.matcher(value.toString());
		
		Properties props = new Properties();
    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
    	try {
    		String locale = gbean.getLocale();
    		String lang = locale.equals(GlobalsBean.DEFAULT_LANG)? "" : locale; 
    		String languagePropFile="com/baytouch/helpdesk/language/select" + (lang!="" ? "_" + lang : "") + ".properties"; 
			props.load(cl.getResourceAsStream(languagePropFile));
		} catch (IOException e){
			e.printStackTrace();
		}    	
		
		// Check the format of the email is correct...
		if(!matcher.matches()){
			msgVal = " " + props.getProperty("field_validate_email_invalid"); // " Invalid E-mail format";
			isError=true;
		}
		
		// Check the email address is unique
		Account acc = udao.getUserByEmail(value.toString());
		if(acc!=null){
			// Get the currently edited account
			Application app = context.getApplication();
			ValueExpression expression = app.getExpressionFactory().createValueExpression( context.getELContext(),"#{usersBean}", Object.class );
			UsersBean uBean = (UsersBean) expression.getValue( context.getELContext() );
			Account curUser = uBean.getUser();
			// If the id of the currently edited account is different to the id of the email address then the email address already exists withing the DB
			if( !acc.getId().equals(curUser.getId())){
				msgVal = " " + props.getProperty("field_validate_email_exists"); // " eMail address already exists";
				isError=true;
			}
		}

		if(isError){
			FacesMessage msg = new FacesMessage("E-mail validation failed",msgVal);
			msg.setSeverity(FacesMessage.SEVERITY_ERROR);
			EditableValueHolder evh = (EditableValueHolder) component;
			evh.setValid(false);
			throw new ValidatorException(msg);
		}
	}

}
