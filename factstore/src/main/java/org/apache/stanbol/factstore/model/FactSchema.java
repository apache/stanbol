package org.apache.stanbol.factstore.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.commons.jsonld.JsonLdProfile;

public class FactSchema {

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
    
    public JsonLdProfile toJsonLdProfile() {
        JsonLdProfile profile = new JsonLdProfile();
        
        for (String role : this.roleMap.keySet()) {
            profile.addTypes(role, this.roleMap.get(role));
        }
        
        return profile;
    }
    
    public static FactSchema fromJsonLdProfile(String factSchemaURN, JsonLdProfile jsonLd) {
        if (factSchemaURN == null || factSchemaURN.isEmpty()) {
            throw new IllegalArgumentException("Fact schema URN must not be empty");
        }

        FactSchema factSchema = new FactSchema();
        factSchema.setFactSchemaURN(factSchemaURN);
        for (String role : jsonLd.getTypes()) {
            // we have to unCURIE the types here to be sure it is not stored as a CURIE
            List<String> types = jsonLd.getTypes(role);
            for (String type : types) {
                type = jsonLd.unCURIE(type);
                factSchema.addRole(role, type);
            }
            
        }

        return factSchema;
    }
    
}
