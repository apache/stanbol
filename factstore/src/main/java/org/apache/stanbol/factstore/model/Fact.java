package org.apache.stanbol.factstore.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdIRI;
import org.apache.stanbol.commons.jsonld.JsonLdResource;

public class Fact {

    private String factSchemaURN;
    
    private Map<String, List<String>> roleMap = new HashMap<String,List<String>>();
    
    public String getFactSchemaURN() {
        return factSchemaURN;
    }

    public void setFactSchemaURN(String factSchemaURN) {
        this.factSchemaURN = factSchemaURN;
    }

    public void addRole(String role, String type) {
        if (this.roleMap.get(role) == null) {
            this.roleMap.put(role, new ArrayList<String>());
        }
        List<String> types = this.roleMap.get(role);
        types.add(type);
    }
    
    public Set<String> getRoles() {
        return this.roleMap.keySet();
    }

    public List<String> getTypesOfRole(String role) {
        return this.roleMap.get(role);
    }

    public static Fact factFromJsonLd(JsonLd jsonLd) {
        Fact fact = null;
        
        if (jsonLd.getResourceSubjects().size() == 1) {
            String subject = jsonLd.getResourceSubjects().iterator().next();
            fact = resourceToFact(jsonLd, jsonLd.getResource(subject));
        }
        
        return fact;
    }
    
    public static Set<Fact> factsFromJsonLd(JsonLd jsonLd) {
        Set<Fact> facts = new HashSet<Fact>();
        
        if (jsonLd.getResourceSubjects().size() > 1) {
            for (String subject : jsonLd.getResourceSubjects()) {
                Fact fact = resourceToFact(jsonLd, jsonLd.getResource(subject));
                if (fact != null) {
                    facts.add(fact);
                }
            }
        }
        
        return facts;
    }
    
    private static Fact resourceToFact(JsonLd jsonLd, JsonLdResource resource) {
        Fact fact = null;
        String schemaURN = jsonLd.unCURIE(resource.getProfile());
        
        if (schemaURN != null && !schemaURN.isEmpty()) {
            Map<String, Object> propMap = resource.getPropertyMap();
            if (propMap.size() > 1) {
                fact = new Fact();
                fact.setFactSchemaURN(schemaURN);
                for (String role : propMap.keySet()) {
                    if (propMap.get(role) instanceof String) {
                        String strType = (String) propMap.get(role);
                        fact.addRole(role, strType);
                    }
                    else if (propMap.get(role) instanceof JsonLdIRI) {
                        JsonLdIRI iriType = (JsonLdIRI) propMap.get(role);
                        fact.addRole(role, jsonLd.unCURIE(iriType.getIRI()));
                    }
                    else {
                        fact.addRole(role, propMap.get(role).toString());
                    }
                }
            }
        }
        
        return fact;
    }
}
