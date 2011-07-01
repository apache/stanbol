package org.apache.stanbol.commons.jsonld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabian Christ
 * 
 */
public class JsonLdResource {

    private String subject;
    private String profile;
    private List<String> types = new ArrayList<String>();
    private Map<String,String> coercionMap = new HashMap<String,String>();
    private Map<String,Object> propertyMap = new HashMap<String,Object>();

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void addType(String type) {
        types.add(type);
    }

    public void addAllTypes(List<String> types) {
        this.types.addAll(types);
    }

    public void putCoercionType(String property, String type) {
        this.coercionMap.put(property, type);
    }

    public String getCoercionTypeOf(String property) {
        return this.coercionMap.get(property);
    }

    public Map<String,String> getCoercionMap() {
        return this.coercionMap;
    }

    public List<String> getTypes() {
        return types;
    }

    public void putAllProperties(Map<String,Object> propertyMap) {
        this.propertyMap.putAll(propertyMap);
    }

    public void putProperty(String property, Object value) {
        propertyMap.put(property, value);
    }

    public Object getPropertyValueIgnoreCase(String property) {
        for (String p : this.propertyMap.keySet()) {
            if (p.equalsIgnoreCase(property)) {
                return this.propertyMap.get(p);
            }
        }
        return null;
    }
    
    public Set<String> getProperties() {
        return this.propertyMap.keySet();
    }
   
    public Map<String,Object> getPropertyMap() {
        return this.propertyMap;
    }

    public boolean hasPropertyIgnorecase(String property) {
        for (String p : this.propertyMap.keySet()) {
            if (p.equalsIgnoreCase(property)) {
                return true;
            }
        }

        return false;
    }
}
