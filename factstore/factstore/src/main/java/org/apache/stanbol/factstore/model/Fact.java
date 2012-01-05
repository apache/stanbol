/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.factstore.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdCommon;
import org.apache.stanbol.commons.jsonld.JsonLdProperty;
import org.apache.stanbol.commons.jsonld.JsonLdPropertyValue;
import org.apache.stanbol.commons.jsonld.JsonLdResource;

public class Fact {

    private String factSchemaURN;
    private Map<String, String> roleMap = new HashMap<String,String>();
    private FactContext context;
    
    public String getFactSchemaURN() {
        return factSchemaURN;
    }

    public void setFactSchemaURN(String factSchemaURN) {
        this.factSchemaURN = factSchemaURN;
    }

    public void addRole(String role, String value) {
    	this.roleMap.put(role, value);
    }
    
    public Set<String> getRoles() {
        return this.roleMap.keySet();
    }

    public String getValueOfRole(String role) {
        return this.roleMap.get(role);
    }
    
    public FactContext getContext() {
		return context;
	}

	public void setContext(FactContext context) {
		this.context = context;
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
            Map<String, JsonLdProperty> propMap = resource.getPropertyMap();
            if (propMap.size() > 1) {
                fact = new Fact();
                fact.setFactSchemaURN(schemaURN);
                for (String role : propMap.keySet()) {
                    JsonLdProperty jldProperty = propMap.get(role);
                    if (jldProperty.isSingleValued()) {
                        JsonLdPropertyValue jldValue = jldProperty.getValues().get(0);
                        if (jldValue.getType() != null && jldValue.getType().equals(JsonLdCommon.IRI)) {
                            fact.addRole(role, jsonLd.unCURIE(jldValue.getLiteralValue()));
                        } else {
                            fact.addRole(role, jldValue.getValue().toString());
                        }
                    }
                    else {
                        // TODO Implement multi-valued properties when converting JSON-LD to fact
                        throw new RuntimeException("Sorry, this is not implemented yet!");
                    }
                }
            }
        }
        
        return fact;
    }

    public JsonLd factToJsonLd() {
        JsonLd jsonLd = new JsonLd();
        
        JsonLdResource subject = new JsonLdResource();
        subject.setProfile(this.factSchemaURN);
        for (String role : this.roleMap.keySet()) {
            subject.putProperty(role, this.roleMap.get(role));
        }
        jsonLd.put(subject);
        
        return jsonLd;
    }
    
}
