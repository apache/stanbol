package org.apache.stanbol.commons.jsonld;

public class JsonLdPropertyValue {

    private Object value;
    private String type;
    private String language;
    
    public JsonLdPropertyValue() {
        
    }
    
    public JsonLdPropertyValue(Object value) {
        if (value instanceof JsonLdIRI) {
            JsonLdIRI iriValue = (JsonLdIRI) value;
            this.value = iriValue.getIRI();
            this.type = JsonLdCommon.IRI;
        }
        else {
            this.value = value;
        }
    }

    public Object getValue() {
        return value;
    }

    public String getLiteralValue() {
        return String.valueOf(value);
    }
    
    public void setValue(Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}
