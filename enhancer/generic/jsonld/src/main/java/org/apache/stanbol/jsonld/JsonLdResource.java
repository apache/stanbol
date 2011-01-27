package org.apache.stanbol.jsonld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fabian Christ
 *
 */
public class JsonLdResource {

    private String subject;
    private List<String> types = new ArrayList<String>();
    private Map<String, Object> propertyMap = new HashMap<String, Object>();

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void addType(String type) {
        types.add(type);
    }

    public void addAllTypes(List<String> types) {
        this.types.addAll(types);
    }

    public List<String> getTypes() {
        return types;
    }

    public void putAllProperties(Map<String, Object> propertyMap) {
        this.propertyMap.putAll(propertyMap);
    }

    public void putProperty(String property, Object value) {
        propertyMap.put(property, value);
    }

    public Map<String, Object> getPropertyMap() {
        return propertyMap;
    }

}
