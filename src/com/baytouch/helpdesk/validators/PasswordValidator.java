package com.baytouch.helpdesk.validators;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import javax.enterprise.inject.Model;
import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;

import com.baytouch.helpdesk.beans.GlobalsBean;

// https://www.mkyong.com/jsf2/multi-components-validator-in-jsf-2-0/
@Model
@FacesValidator("PasswordValidator")
public class PasswordValidator implements Validator, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	GlobalsBean gbean;

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {		
		
		String password = value.toString();
		UIInput uiInputConfirmPassword = (UIInput) getAttribute(context, component, "checkConfirmPassword"); 			
		String confirmPassword = ((EditableValueHolder) uiInputConfirmPassword).getSubmittedValue().toString();
		
		// Let required="true" do its job.
		if (password == null || password.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
			return;
		}

		if (!password.equals(confirmPassword)) {
			
			Properties props = new Properties();
	    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    	try {
	    		String locale = gbean.getLocale();
	    		String languagePropFile="com/baytouch/helpdesk/language/select" + (locale!=""?"_" + locale : "") + ".properties"; 
				props.load(cl.getResourceAsStream(languagePropFile));	
			} catch (IOException e){
				e.printStackTrace();
			}    	
			String msgVal = " " + props.getProperty("field_validate_passwordMatch"); // " Passwords must match"
			
			uiInputConfirmPassword.setValid(false);
			EditableValueHolder evhpw = (EditableValueHolder) component;
			evhpw.setValid(false);
			FacesMessage msg = new FacesMessage("Password validation failed.", msgVal );
			FacesContext.getCurrentInstance().addMessage("userForm:cnp:confirmNewPassword",msg);
			throw new ValidatorException(msg);
		}
	}

	/**
	 * Attributes passed through a composite component are attached to the parent rather than the actual component itself
	 * http://stackoverflow.com/questions/9534600/can-i-tell-jsf-that-an-fattribute-applies-to-some-part-of-my-composite-componen
	 * @param c
	 * @param component
	 * @param name
	 * @return
	 */
	private Object getAttribute(FacesContext c, UIComponent component, String name) {
	    Object result = component.getAttributes().get(name);
	    if (result == null && component.getParent() != null) {
	        result = getAttribute(c, component.getParent(), name);
	    }
	    return result;
	}
}

