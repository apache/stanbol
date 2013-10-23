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
package org.apache.stanbol.commons.jsonld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JsonLd class provides an API to create a JSON-LD object structure and to serialize this structure.
 * 
 * <p>
 * This implementation is based on the JSON-LD 1.0 specification editor's draft from
 * January 5, 2013. Available online at <a href="http://json-ld.org/spec">http://json-ld.org/spec</a>.
 */
public class JsonLd extends JsonLdCommon {
    
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(JsonLd.class);

    /**
     *  Map Subject -> Resource
     */
    private Map<String,JsonLdResource> resourceMap = new TreeMap<String,JsonLdResource>(new JsonComparator());

    /**
     * Flag to control whether the serialized JSON-LD output will use joint or disjoint graphs for subjects
     * and namespaces. Default value is <code>true</code>.
     */
    private boolean useJointGraphs = true;

    /**
     * Flag to control whether type coercion should be applied on serialization. Default value is
     * <code>true</code>.
     */
    private boolean useTypeCoercion = true;

    /**
     * Adds the given resource to this JsonLd object using the resource's subject as key. If the key is NULL
     * and there does not exist a resource with an empty String as key the resource will be added using
     * an empty String ("") as key. Otherwise an @IllegalArgumentException is thrown.
     * 
     * @param resource
     */
    public void put(JsonLdResource resource) {
        if (resource.getSubject() != null) {
            this.resourceMap.put(resource.getSubject(), resource);
        }
        else if (!this.resourceMap.containsKey("")) {
            this.resourceMap.put("", resource);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Add the given resource to this JsonLd object using the resourceId as key.
     * 
     * @param resourceId
     * @param resource
     */
    public void put(String resourceId, JsonLdResource resource) {
        this.resourceMap.put(resourceId, resource);
    }

    /**
     * Serializes the JSON-LD object structures to a String.
     * 
     * <p>
     * If you want to have a formatted output with indentation, use the toString(int indent) variant.
     * 
     * @return JSON-LD as unformatted String.
     */
    @Override
    public String toString() {
        if (useJointGraphs) {
            Map<String,Object> json = createJsonMap();

            return JsonSerializer.toString(json);
        } else {
            List<Object> json = createJsonList();

            return JsonSerializer.toString(json);
        }
    }

    /**
     * Serializes the JSON-LD object structure to a beautified String using indentation. The output is
     * formatted using the specified indentation size.
     * 
     * @param indent
     *            Number of whitespace chars used for indentation.
     * @return JSON-LD as formatted String.
     */
    public String toString(int indent) {
        if (useJointGraphs) {
            Map<String,Object> json = createJsonMap();

            return JsonSerializer.toString(json, indent);
        } else {
            List<Object> json = createJsonList();

            return JsonSerializer.toString(json, indent);
        }
    }
    
    private Map<String,Object> createJsonMap() {
        Map<String,Object> json = null;
        try {
            json = createJointGraph();
        } catch (ShorteningException e) {
            // problems while using the shortening algorithm
            this.setUseCuries(true);
            this.usedNamespaces.clear();
            try {
                json = createJointGraph();
            } catch (ShorteningException e1) {
                // ignore this
            }
        }
        return json;
    }
    
    private List<Object> createJsonList() {
        List<Object> json = null;
        try {
            json = createDisjointGraph();
        } catch (ShorteningException e) {
            // problems while using the shortening algorithm
            this.setUseCuries(true);
            this.usedNamespaces.clear();
            try {
                json = createDisjointGraph();
            } catch (ShorteningException e1) {
                // ignore this
            }
        }
        return json;
    }

    private List<Object> createDisjointGraph() throws ShorteningException {
        List<Object> json = new ArrayList<Object>();
        if (!resourceMap.isEmpty()) {

            for (String subject : resourceMap.keySet()) {
                Map<String,Object> subjectObject = new TreeMap<String,Object>(new JsonComparator());
                JsonLdResource resource = resourceMap.get(subject);

                // put subject
                if (resource.getSubject() != null && !resource.getSubject().isEmpty()) {
                    subjectObject.put(ID, shortenURI(resource.getSubject()));
                }

                // put profile
                if (resource.getProfile() != null && !resource.getProfile().isEmpty()) {
                    subjectObject.put(PROFILE, shortenURI(resource.getProfile()));
                }

                // put types
                putTypes(subjectObject, resource);

                // put properties = objects
                putProperties(subjectObject, resource);

                // add to list of subjects
                json.add(subjectObject);
                
                // put the used namespaces
                if (!this.usedNamespaces.isEmpty() || this.useTypeCoercion) {
                    Map<String,Object> nsObject = new TreeMap<String,Object>(new JsonComparator());
                    
                    for (String ns : this.usedNamespaces.keySet()) {
                        nsObject.put(this.usedNamespaces.get(ns), ns);
                    }
                    this.usedNamespaces.clear();
                    
                    subjectObject.put(CONTEXT, nsObject);
                }
            }
        }

        return json;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> createJointGraph() throws ShorteningException {
        Map<String,Object> context = new TreeMap<String,Object>(new JsonComparator());
        
        Map<String,Object> json = new TreeMap<String,Object>(new JsonComparator());
        Map<String,String> coercionMap = new TreeMap<String,String>(new JsonComparator());

        if (!resourceMap.isEmpty()) {
            List<Object> subjects = new ArrayList<Object>();

            for (String subject : resourceMap.keySet()) {
                // put subject
                Map<String,Object> subjectObject = new TreeMap<String,Object>(new JsonComparator());

                JsonLdResource resource = resourceMap.get(subject);

                // put subject
                if (resource.getSubject() != null && !resource.getSubject().isEmpty()) {
                    subjectObject.put(ID, shortenURI(resource.getSubject()));
                }
                
                // put profile
                if (resource.getProfile() != null && !resource.getProfile().isEmpty()) {
                    subjectObject.put(PROFILE, shortenURI(resource.getProfile()));
                }

                // put types
                putTypes(subjectObject, resource);

                // put properties = objects
                putProperties(subjectObject, resource);

                if (this.useTypeCoercion) {
                    coercionMap.putAll(resource.getCoerceMap());
                    
                    for (String propertyName : resource.getPropertyMap().keySet()) {
                        JsonLdProperty property = resource.getProperty(propertyName);
                        if (property.isTyped()) {
                            Map<String,Object> propObject = new TreeMap<String,Object>(new JsonComparator());
                            String finalPropName = propertyName;
                            if (this.useCuries) {
                                propObject.put(TYPE, this.shortenURI(property.getType()));
                                finalPropName = this.shortenURI(propertyName);
                            }
                            else {
                                propObject.put(TYPE, this.unCURIE(property.getType()));
                                finalPropName = this.unCURIE(propertyName);
                            }
                            
                            // Check if there is a namespace defined for this property
                            String removeNs = null;
                            for (String nsKey : this.usedNamespaces.keySet()) {
                                if (finalPropName.equals(nsKey)) {
                                    finalPropName = this.usedNamespaces.get(nsKey);
                                    propObject.put(ID, nsKey);
                                    removeNs = nsKey;
                                    break;
                                }
                            }
                            if (removeNs != null) {
                                this.usedNamespaces.remove(removeNs);
                            }
                            context.put(finalPropName, propObject);
                        }
                    }
                }

                // add to list of subjects
                subjects.add(subjectObject);
            }

            // put subjects
            if (!subjects.isEmpty()) {
                if (subjects.size() == 1) {
                    json = (Map<String,Object>) subjects.get(0);
                } else {
                    json.put(GRAPH, subjects);
                }
            }
        }

        // put the namespaces
        if (!this.usedNamespaces.isEmpty() || (!coercionMap.isEmpty() && this.useTypeCoercion)) {            
            for (String ns : usedNamespaces.keySet()) {
                context.put(usedNamespaces.get(ns), ns);
            }

            json.put(CONTEXT, context);
        }

        return json;
    }

    private void putTypes(Map<String,Object> subjectObject, JsonLdResource resource) throws ShorteningException {
        if (!resource.getTypes().isEmpty()) {
            List<String> types = new ArrayList<String>();
            for (String type : resource.getTypes()) {
                types.add(shortenURI(type));
            }
            if (types.size() == 1) {
                subjectObject.put(TYPE, types.get(0));
            } else {
                Collections.sort(types, new Comparator<String>() {

                    @Override
                    public int compare(String arg0, String arg1) {
                        return arg0.compareTo(arg1);
                    }

                });
                subjectObject.put(TYPE, types);
            }
        }
    }

    private void putProperties(Map<String,Object> jsonObject, JsonLdResource resource) throws ShorteningException {
        for (String property : resource.getPropertyMap().keySet()) {
            JsonLdProperty jldProperty = resource.getPropertyMap().get(property);

            if (jldProperty.isSingleValued()) {
                putSingleValuedProperty(jsonObject, resource, property, jldProperty);
            }
            else {
                putMultiValuedProperty(jsonObject, resource, property, jldProperty);
            }
        }
    }

    private void putSingleValuedProperty(Map<String,Object> jsonObject,
                                         JsonLdResource resource,
                                         String property,
                                         JsonLdProperty jldProperty) throws ShorteningException {
        // This is a single value property but it may have
        // a datatype and a language.
        Object value = jldProperty.getValues().get(0);
        Map<String,Object> valueObject = new TreeMap<String,Object>();
        putProperty(valueObject, resource.getCoerceMap(), property, value);
        
        if (valueObject.containsKey(TYPE)) {
            putTypedValue(jsonObject, resource, property, valueObject);
        }
        else if (valueObject.size() == 1 && valueObject.containsKey(VALUE)) {
            // If the returned value object contains only one @value value,
            // we can simplify the value by admitting the @value.
            putSimplifiedValue(jsonObject, property, valueObject, resource.getCoerceMap());
        }
        else {
            // Value object has no type but a language
            jsonObject.put(shortenURI(property),valueObject);
        }
    }
    
    private void putSimplifiedValue(Map<String,Object> jsonObject,
                                    String property,
                                    Map<String,Object> valueObject,
                                    Map<String,String> coercionMap) throws ShorteningException {
        Object convertedValue = convertValueType(valueObject.get(VALUE));
        if (convertedValue instanceof String) {
            String strValue = (String) convertedValue;
            String type = coercionMap.get(property);
            if (type != null) {
                if (this.useTypeCoercion) {
                    strValue = (String)doCoerce(strValue, type);
                    jsonObject.put(shortenURI(property), shortenURI(strValue));
                } else {
                    Object objValue = unCoerce(strValue, type);
                    jsonObject.put(shortenURI(property), objValue);
                }
            }
            else {
                jsonObject.put(shortenURI(property), shortenURI(strValue));
            }
        }
        else {
            jsonObject.put(shortenURI(property), convertedValue);
        }
    }

    private void putTypedValue(Map<String,Object> jsonObject,
                               JsonLdResource resource,
                               String property,
                               Map<String,Object> valueObject) throws ShorteningException {
        if (this.useTypeCoercion) {
            // In case of type coercion we just add the value. The datatype is already
            // set through the coercion map.
            if (resource.getCoerceMap().get(property) != null) {
                putSimplifiedValue(jsonObject, property, valueObject, resource.getCoerceMap());
            }
            else {
                // No type defined through coercion - use the value as it is
                simplifyIRI(valueObject);
                jsonObject.put(shortenURI(property),valueObject);
            }
        }
        else {
            // No type coercion but value has type - so we add value object
            simplifyIRI(valueObject);
            jsonObject.put(shortenURI(property),valueObject);
        }
    }
    
    private void putMultiValuedProperty(Map<String,Object> jsonObject,
                                        JsonLdResource resource,
                                        String property,
                                        JsonLdProperty jldProperty) throws ShorteningException {
        List<Object> valueList = new ArrayList<Object>();
        
        for (JsonLdPropertyValue value : jldProperty.getValues()) {
            Map<String,Object> valueObject = new TreeMap<String,Object>();
            putProperty(valueObject, resource.getCoerceMap(), property, value);
            
            if (valueObject.containsKey(TYPE)) {
                if (this.useTypeCoercion) {
                    if (resource.getCoerceMap().get(property) != null) {
                        // Type already defined by coercion. We add the simplified value.
                        addSimplifiedValue(valueList, valueObject);
                    }
                    else {
                        // No type defined by coercion - so we use the value object
                        simplifyIRI(valueObject);
                        valueList.add(valueObject);
                    }
                }
                else {
                    simplifyIRI(valueObject);
                    valueList.add(valueObject);
                }
            }
            else if (valueObject.size() == 1 && valueObject.containsKey(VALUE)) {
                // If the returned value object contains only one @value value,
                // we can simplify the value by admitting the @value.
                addSimplifiedValue(valueList, valueObject);
            }
            else {
                valueList.add(valueObject);
            }
            
        }
        
        jsonObject.put(shortenURI(property), valueList);
    }

    private void simplifyIRI(Map<String,Object> valueObject) throws ShorteningException {
        // No type coercion but value has type - maybe it's an IRI
        if (valueObject.get(TYPE).equals(ID)) {
            String iri = (String)valueObject.get(VALUE);
            valueObject.clear();
            valueObject.put(ID, shortenURI(iri));
        }
    }

    private void addSimplifiedValue(List<Object> valueList, Map<String,Object> valueObject) throws ShorteningException {
        Object convertedValue = convertValueType(valueObject.get(VALUE));
        if (convertedValue instanceof String) {
            String strValue = (String) convertedValue;
            valueList.add(shortenURI(strValue));
        }
        else {
            valueList.add(convertedValue);
        }
    }

    private void putProperty(Map<String,Object> jsonObject,
                               Map<String,String> coercionMap,
                               String property,
                               Object value) throws ShorteningException {
        if (value instanceof JsonLdIRI) {
            JsonLdIRI iriValue = (JsonLdIRI) value;
            Map<String,Object> iriObject = new TreeMap<String,Object>();
            iriObject.put(ID, shortenURI(iriValue.getIRI()));
            jsonObject.put(shortenURI(property), iriObject);
        } else if (value instanceof JsonLdPropertyValue) {
            JsonLdPropertyValue jldPropertyValue = (JsonLdPropertyValue)value;
            jsonObject.put(VALUE, jldPropertyValue.getLiteralValue());
            String type = coercionMap.get(property);
            if (type != null) {
                jldPropertyValue.setType(type);
            }
            if (jldPropertyValue.getType() != null) {
                jsonObject.put(TYPE, shortenURIWithCuries(jldPropertyValue.getType()));
            }
            if (jldPropertyValue.getLanguage() != null) {
                jsonObject.put(LANGUAGE, jldPropertyValue.getLanguage());
            }
        }
    }

    /**
     * Returns a map specifying the literal form and the datatype.
     * 
     * @param strValue
     * @param type
     * @return
     * @throws ShorteningException 
     */
    private Map<String, Object> unCoerce(Object value, String type) throws ShorteningException {
        Map<String, Object> typeDef = new TreeMap<String,Object>(new JsonComparator());
        
        if (type.equals(ID)) {
            typeDef.put(VALUE, String.valueOf(value));
        }
        else {
            typeDef.put(VALUE, String.valueOf(value));
        }
        typeDef.put(TYPE, shortenURI(type));
        
        return typeDef;
    }

    /**
     * Removes the type from the value and handles conversion to Integer and Boolean.
     * 
     * @FIXME Use @literal and @datatype notation when parsing typed literals
     * 
     * @param strValue
     * @param type
     * @return
     */
    private Object doCoerce(Object value, String type) {
        if (value instanceof String) {
            String strValue = (String) value;
            String typeSuffix = "^^" + unCURIE((type));
            strValue = strValue.replace(typeSuffix, "");
            strValue = strValue.replaceAll("\"", "");
            return strValue;            
        }
        
        return value;
    }

    /**
     * Converts a given object to Integer or Boolean if the object is instance of one of those types.
     * 
     * @param strValue
     * @return
     */
    private Object convertValueType(Object value) {
        if (value instanceof String) {
            String strValue = (String) value;
            
            // check if value can be interpreted as long
            try {
                return Long.valueOf(strValue);
            }
            catch (Throwable t) {};
            
            // check if value can be interpreted as integer
            try {
                return Integer.valueOf(strValue);
            }
            catch (Throwable t) {};

            // check if it is a float double
            try {
                return Double.valueOf(strValue);
            }
            catch (Throwable t) {};
            
            // check if it is a float value
            try {
                return Float.valueOf(strValue);
            }
            catch (Throwable t) {};
            
            // check if value can be interpreted as boolean
            if (strValue.equalsIgnoreCase("true") || strValue.equalsIgnoreCase("false")) {
                return Boolean.valueOf(strValue);
            }
            
            return strValue;            
        }
        
        return value;
    }
    
    /**
     * Return the JSON-LD resource for the given subject.
     */
    public JsonLdResource getResource(String subject) {
        return this.resourceMap.get(subject);
    }
    
    public Set<String> getResourceSubjects() {
        return this.resourceMap.keySet();
    }

    /**
     * Determine whether currently joint or disjoint graphs are serialized with this JSON-LD instance.
     * 
     * @return <code>True</code> if joint graphs are used, <code>False</code>otherwise.
     */
    public boolean isUseJointGraphs() {
        return useJointGraphs;
    }

    /**
     * Set to <code>true</code> if you want to use joint graphs (default) or <code>false</code> otherwise.
     * 
     * @param useJointGraphs
     */
    public void setUseJointGraphs(boolean useJointGraphs) {
        this.useJointGraphs = useJointGraphs;
    }

    /**
     * Flag to control whether type coercion is applied or not.
     * 
     * @return <code>True</code> if type coercion is applied, <code>false</code> otherwise.
     */
    public boolean isUseTypeCoercion() {
        return useTypeCoercion;
    }

    /**
     * Control whether type coercion should be applied. Set this to <code>false</code> if you don't want to
     * use type coercion in the output.
     * 
     * @param useTypeCoercion
     */
    public void setUseTypeCoercion(boolean useTypeCoercion) {
        this.useTypeCoercion = useTypeCoercion;
    }

}
