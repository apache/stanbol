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

import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
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

    protected String field;
    protected boolean includeAll = false;
    protected Collection<String> included;
    protected Collection<String> exclude;
    //now represented by adding "" to included and exclude
    //boolean includeEmpty;

    private NamespacePrefixProvider nsPrefixProvider;
    
    public FieldValueFilter() {}
    
    /**
     * Only for unit testing
     */
    protected FieldValueFilter(NamespacePrefixProvider nsPrefixProvider, String field, Object filterConfig){
        this.nsPrefixProvider = nsPrefixProvider;
        this.field = getUri(field);
        parseFilterConfig(filterConfig);
    }
    
    @Override
    public Representation process(Representation source) {
        if(includeAll && exclude.isEmpty()){
            return source; //filter inactive
        }
        Iterator<Reference> refs = source.getReferences(field);
        if(!refs.hasNext()){ //no values and includeNull
            return (includeAll && !exclude.contains("")) || //include and empty not excluded
                    (!includeAll && included.contains("")) ? //empty is included
                            source : null;
        }
        while(refs.hasNext()){
            String value = refs.next().getReference();
            if((includeAll && !exclude.contains(value)) || //include and empty not excluded
                    (!includeAll && included.contains(value))){ //empty is included
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
        log.info("> configure {}",getClass().getSimpleName());
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        nsPrefixProvider = indexingConfig.getNamespacePrefixService();
        Object value = config.get(PARAM_FIELD);
        if(value == null || value.toString().isEmpty()){
            this.field = getUri(DEFAULT_FIELD);
        } else {
            this.field = getUri(value.toString());
        }
        log.info(" - field: {}",field);
        value = config.get(PARAM_VALUES);
        log.info(" - filters:");
        parseFilterConfig(value);
    }

    /**
     * @param value
     */
    @SuppressWarnings("unchecked")
	private void parseFilterConfig(Object value) {
        Collection<String> values; 
        if(value instanceof String){
            values = Arrays.asList(value.toString().split(";"));
        } else if (value instanceof String[]){
            values = Arrays.asList((String[])value);
        } else if(value == null){ // no values (accept all entities with any value)
            values = Collections.emptySet();
        } else if(value instanceof Collection<?>){
            values = (Collection<String>)value;
        } else {
            throw new IllegalArgumentException("Parameter '" + PARAM_VALUES 
                + "' must be of type String, String[] or Collection<String> (present: "
                + value.getClass()+")!");
        }
        if(values.isEmpty()){
            includeAll = true;
            this.included = values;
            this.exclude = Collections.emptySet();
        } else {
            this.included = new HashSet<String>();
            this.exclude = new HashSet<String>();
            for(String entry : values) {
                if(entry == null){ //NULL is a valid option, but we use "" instead
                    entry = "";
                }
                entry = entry.trim();
                if(entry.equalsIgnoreCase("null")){
                    entry = "";
                }
                if(!includeAll && entry.equals("*")){
                    log.info("    - includeAll");
                    includeAll = true;
                    continue;
                }
                boolean exclude = !entry.isEmpty() && entry.charAt(0) == '!';
                if(exclude){
                    entry = entry.substring(1);
                    if(entry.equalsIgnoreCase("null")){
                        entry = "";
                    }
                    if(entry.equals("*")){
                        throw new IllegalArgumentException("'!*' is not allowed in the config ("
                            + "it is the default if '*' is not present)!");
                    }
                }
                String uri = getUri(entry);
                if((exclude ? this.included : this.exclude).contains(uri)){
                    throw new IllegalArgumentException("'"+entry+"' both included and excluded by the"
                        + "parsed configuration!");
                }
                //if exclude add to this.exclude otherwise to this.values
                (exclude ? this.exclude : this.included).add(uri);
                log.info("    - {} {}",exclude ? "exclude" : "include", uri.isEmpty() ? "<empty>" : uri);
            }
        }
        //if only excludes are configured add the include all
        if(!includeAll && !exclude.isEmpty() && included.isEmpty()){
            log.info("    - includeAll (because only exclusions are configured");
            includeAll = true;
        }
    }

    /**
     * @param entry
     * @return
     */
    private String getUri(String entry) {
        String uri; 
        String nsPrefix = NamespaceMappingUtils.getPrefix(entry);
        if(nsPrefix != null){
            String ns = nsPrefixProvider.getNamespace(nsPrefix);
            if(ns == null){
                throw new IllegalArgumentException("Unable to resolve namesoace prefix used by '"
                        +entry+"' by using the NamespacePrefixService!");
            }
            uri = new StringBuilder(ns).append(entry,nsPrefix.length()+1, entry.length()).toString();
        } else {
            uri = entry;
        }
        return uri;
    }

}
