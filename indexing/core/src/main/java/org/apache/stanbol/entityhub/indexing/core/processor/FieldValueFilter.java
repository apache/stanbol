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
package org.apache.stanbol.entityhub.indexing.core.processor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A simple Processor that allows to filter {@link Representation} based on
 * {@link Reference#getReference()} values of a configured Field.<p>
 * Typically used to filter Representations based on the type (rdf:type)<p>
 * Parsing '*' as value for the field deactivates filtering. A missing
 * field configuration is assumed as Error and will cause an 
 * {@link IllegalArgumentException} during {@link #setConfiguration(Map)}
 * @author Rupert Westenthaler
 *
 */
public class FieldValueFilter implements EntityProcessor{

    private final Logger log = LoggerFactory.getLogger(FieldValueFilter.class);
    
    public static final String PARAM_FIELD = "field";
    public static final String PARAM_VALUES = "values";
    
    public static final String DEFAULT_FIELD = "rdf:type";

    public String field;
    public Collection<String> values;
    /**
     * Parsing 'null' or '' as value can be used to include entities that do not
     * define any values for the configured {@link #field}
     */
    boolean includeEmpty;
    
    @Override
    public Representation process(Representation source) {
        if(!includeEmpty && values.isEmpty()){ //no filter set
            return source;
        }
        Iterator<Reference> refs = source.getReferences(field);
        if(includeEmpty && !refs.hasNext()){ //no values and includeNull
            return source;
        }
        while(refs.hasNext()){
            if(values.contains(refs.next().getReference())){
                return source;
            }
        }
        //not found -> filter
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public void initialise() {
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        Object value = config.get(PARAM_FIELD);
        if(value == null || value.toString().isEmpty()){
            this.field = NamespaceEnum.getFullName(DEFAULT_FIELD);
            log.info("Using default Field %s",field);
        } else {
            this.field = NamespaceEnum.getFullName(DEFAULT_FIELD);
            log.info("configured Field: %s",field);
        }
        value = config.get(PARAM_VALUES);
        if(value == null || value.toString().isEmpty()){
            throw new IllegalArgumentException("Missing required Parameter "+PARAM_VALUES+". Set to '*' to deactivate Filtering");
        } else if(value instanceof String){
            String stringValue = value.toString().trim();
            if(stringValue.equals("*")){ // * -> deactivate Filtering
                this.values = Collections.emptySet();
            } else {
                Set<String> values = new HashSet<String>();
                for(String fieldValue : stringValue.split(";")){
                    if(fieldValue != null){
                        if(fieldValue.isEmpty() || fieldValue.equalsIgnoreCase("null")){
                            this.includeEmpty = true;
                        } else {
                            values.add(NamespaceEnum.getFullName(fieldValue));
                        }
                    } 
                }
                if(values.isEmpty() && !includeEmpty){
                    throw new IllegalArgumentException("Parameter "+PARAM_VALUES+'='+value+" does not contain a valid field value!");
                } else {
                    this.values = values;
                }
            }
        } else if (value instanceof String[]){
            String[] typeArray = (String[])value;
            if(typeArray.length == 0 || //if an empty array or
                    typeArray.length == 1 && typeArray[0].equals("*")){ //only a * is parsed
                this.values = Collections.emptySet(); // than deactivate filtering
            } else {
                Set<String> values = new HashSet<String>();
                for(String filterString : typeArray){
                    if(filterString != null){
                        if(filterString.isEmpty() || filterString.equalsIgnoreCase("null")){
                            this.includeEmpty = true;
                        } else {
                            values.add(NamespaceEnum.getFullName(filterString));
                        }
                    }
                }
                if(values.isEmpty() && !this.includeEmpty){
                    throw new IllegalArgumentException("Parameter "+PARAM_VALUES+'='+value+" does not contain a valid field value!");
                } else {
                    this.values = values;
                }
            }
        } else {
            throw new IllegalArgumentException("Type of parameter "+PARAM_VALUES+'='+value+
                "(type:"+value.getClass()+") is not supported MUST be String or String[]!");
        }
    }

}
