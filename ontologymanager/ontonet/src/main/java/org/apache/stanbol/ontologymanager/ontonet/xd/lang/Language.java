/**
 * 
 */
package org.apache.stanbol.ontologymanager.ontonet.xd.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Enrico Daga
 * 
 */
public enum Language {
    EN("en"), IT("it"), FR("fr"), DE("de"), ES("es");
    private String value = "";

    Language(String s) {
	this.value = s;
    }

    public String getValue() {
	return this.value;
    }

    @Override
    public String toString() {
	return this.value;
    }

    public String toValue() {
	return this.value;
    }

    public static String[] allValues() {
	List<String> str = new ArrayList<String>();
	for (Language l : Language.values()) {
	    str.add(l.getValue());
	}
	return str.toArray(new String[str.size()]);
    }

    public static Language getInstance(String xx) {
	return Language.valueOf(xx.toUpperCase());
    }

    public static Language getDefault() {
	return EN;
    }
    
}
