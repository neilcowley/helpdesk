package com.baytouch.helpdesk.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter("stringConverter")
public class StringConverter implements Converter {

	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		return value.trim();
	}
	
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		return (String)value;
	}

}
