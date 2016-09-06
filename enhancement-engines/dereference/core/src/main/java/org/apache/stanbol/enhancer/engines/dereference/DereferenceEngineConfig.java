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

package org.apache.stanbol.enhancer.engines.dereference;

import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.NO_LANGUAGE_KEY;
import static org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils.getConfiguredUri;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getConfigValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;

public class DereferenceEngineConfig implements DereferenceConstants {

    private final NamespacePrefixService nsPrefixService;

    private final Dictionary<String,Object> config;
    private String ldpath;
    private List<String> dereferenced;
    private Set<String> languages;
    private Set<IRI> entityReferences;

    /**
     * Creates a DereferenceEngine configuration based on a Dictionary. Typically
     * the dictionary will contain keys as defined by {@link DereferenceConstants}
     * and {@link EnhancementEngine}
     * @param config the config - typically as parsed in the activate method of
     * an OSGI component.
     * @param nsPrefixService the {@link NamespacePrefixService} used to resolve
     * <code>{ns}:{local-name}</code> like configurations for the configured 
     * {@link #getEntityReferences()} properties. If <code>null</code> 
     * using qnames for the entity references will cause a 
     * {@link ConfigurationException}
     * @throws ConfigurationException if the parsed config is not valid
     */
    public DereferenceEngineConfig(Dictionary<String,Object> config, NamespacePrefixService nsPrefixService) throws ConfigurationException {
        this.nsPrefixService = nsPrefixService;
        this.config = config;
        validateRequired(config);
    }
    
    /**
     * Getter for the {@link NamespacePrefixService}
     * @return the service or <code>null</code> if not available
     * @since 0.12.1
     */
    public NamespacePrefixService getNsPrefixService() {
        return nsPrefixService;
    }
    /**
     * If filtering for non content language literals is active
     * @return the {@link DereferenceConstants#FILTER_CONTENT_LANGUAGES} state
     */
    public boolean isFilterContentLanguages(){
        Object value = config.get(FILTER_CONTENT_LANGUAGES);
        return value == null ? DEFAULT_FILTER_CONTENT_LANGUAGES : 
            Boolean.parseBoolean(value.toString());
    }
    
    /**
     * if filtering for non accept language literals is active
     * @return the {@link DereferenceConstants#FILTER_ACCEPT_LANGUAGES} state
     */
    public boolean isFilterAcceptLanguages(){
        Object value = config.get(FILTER_ACCEPT_LANGUAGES);
        return value == null ? DEFAULT_FILTER_ACCEPT_LANGUAGES : 
            Boolean.parseBoolean(value.toString());
    }
    /**
     * Validates the parsed configuration.
     * @param config the configuration to validate
     * @throws ConfigurationException for the first property with an invalid
     * configuration. (1) if no name is set for the engine (2) the configured
     * dereferenced fields are invalid (3) the configured LDPath program is
     * invalid (4) the configured entity references are invalid
     */
    private void validateRequired(Dictionary<String,Object> config) throws ConfigurationException {
        Object value = config.get(EnhancementEngine.PROPERTY_NAME);
        if(value == null || StringUtils.isBlank(value.toString())){
            throw new ConfigurationException(EnhancementEngine.PROPERTY_NAME, 
                "The EnhancementEngine name MUST NOT be NULL nor empty!");
        }
        this.dereferenced = parseDereferenceFields();
        this.ldpath = parseLdPathProgram();
        this.languages = parseLanguages();
        //STANBOL-1334
        this.entityReferences = parseEntityReferences();
    }

    /**
     * Getter for the name of the EnhancementEngine
     * @return the configured {@link EnhancementEngine#PROPERTY_NAME}
     */
    public String getEngineName(){
        Object value = config.get(EnhancementEngine.PROPERTY_NAME);
        return value == null ? null : value.toString();
    }
    /**
     * The Integer service ranking for the engine
     * @return the configured {@link Constants#SERVICE_RANKING}
     */
    public Integer getServiceRanking(){
        Object value = config.get(Constants.SERVICE_RANKING);
        return value instanceof Integer ? (Integer) value : 
            value instanceof Number ? ((Number)value).intValue() :
                value != null ? Integer.parseInt(value.toString()) : 
                    null;
        
    }
    /**
     * Getter for the list of properties used to refer Entities as configured
     * by the {@link DereferenceConstants#ENTITY_REFERENCES} property. <p>
     * @return the configured entity references or a singleton list containing
     * {@link Properties#ENHANCER_ENTITY_REFERENCE} if no custom configuration
     * is present
     * @since 0.12.1 (<a href="https://issues.apache.org/jira/browse/STANBOL-1334">STANBOL-1334</a>)
     */
    public Set<IRI> getEntityReferences() {
        return entityReferences;
    }

    /**
     * Getter for the list of dereferenced fields as configured by the 
     * {@link DereferenceConstants#DEREFERENCE_ENTITIES_FIELDS} property.<p>
     * <i>NOTE</i>: this is the list of configured values. This may still include 
     * '<code>{ns}:{local-name}</code>' like values or special syntax supported
     * by the actual {@link EntityDereferencer} implementation.
     * @return the list of configured dereferenced fields. 
     */
    public List<String> getDereferenceFields(){
        return dereferenced;
    }

    /**
     * Parsed the {@link DereferenceConstants#DEREFERENCE_ENTITIES_FIELDS}
     * config from the parsed Dictionary regardless if it is defined as 
     * <code>String[]</code>, <code>Collection&lt;String&gt;</code> or
     * <code>String</code> (single value).<p>
     * This returns the fields as parsed by the configuration.<p>
     * <b>NOTE:</b> This does not check/convert <code>{prefix}:{localname}</code>
     * configurations to URIs. The receiver of the list is responsible for
     * that 
     * @return the {@link List} with the unprocessed dereference fields as list
     */
    private List<String> parseDereferenceFields() throws ConfigurationException {
        List<String> fields = new ArrayList<String>();
        getConfigValues(config, DEREFERENCE_ENTITIES_FIELDS, String.class, fields);
        return fields;
    }
    /**
     * Parses the URIs for the {@link DereferenceConstants#ENTITY_REFERENCE_PROPERTIES}
     * @return
     * @throws ConfigurationException
     */
    private Set<IRI> parseEntityReferences() throws ConfigurationException {
        Set<IRI> entityRefPropUris;
        Collection<String> entityProps = EnhancementEngineHelper.getConfigValues(
            config, ENTITY_REFERENCES, String.class);
        if(entityProps == null || entityProps.isEmpty()){
            entityRefPropUris = DEFAULT_ENTITY_REFERENCES;
        } else {
            entityRefPropUris = new HashSet<IRI>(entityProps.size());
            for(String prop : entityProps){
                if(!StringUtils.isBlank(prop)){
                    entityRefPropUris.add(new IRI(getConfiguredUri(nsPrefixService, 
                        ENTITY_REFERENCES, prop.trim())));
                }
            }
        }
        return entityRefPropUris;
    }
    /**
     * Getter for the LDPath program as configured by the 
     * {@link DereferenceConstants#DEREFERENCE_ENTITIES_LDPATH} property.
     * @return the LDPath property
     */
    public String getLdPathProgram(){
        return ldpath;
    }
    /**
     * Parses the LdPath program from the value of the 
     * {@link DereferenceConstants#DEREFERENCE_ENTITIES_LDPATH} property. <p>
     * This supports <code>String</code> (the program as a single String), 
     * <code>String[]</code> and <code>Collection&lt;String&gt;</code> (one
     * statement per line).<p>
     * <b>NOTE:</b> This does not parse the LDPath program as this can only be
     * done by the LdPath repository used by the dereferencer.
     * @return the unparsed LDPath program as String 
     */
    private String parseLdPathProgram() throws ConfigurationException {
        Object value = config.get(DEREFERENCE_ENTITIES_LDPATH);
        if(value == null){
            return null;
        } else if(value instanceof String){
            return StringUtils.isBlank((String) value) ? null : (String) value;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if(value instanceof Collection<?>){
            for(Object line : (Collection<?>)value){
                if(line != null && !StringUtils.isBlank(line.toString())){
                    if(first){
                        first = false;
                    } else {
                        sb.append('\n');
                    }
                    sb.append(line.toString());
                }
            }
        } else if(value instanceof String[]){
            for(String line : (String[])value){
                if(line != null && !StringUtils.isBlank(line)){
                    if(first){
                        first = false;
                    } else {
                        sb.append('\n');
                    }
                    sb.append(line);
                }
            }
        } else if(value != null) {//unsupported type
            throw new ConfigurationException(DEREFERENCE_ENTITIES_LDPATH, 
                "Dereference LDPath statements MUST BE parsed as String, String[] or "
                + "Collection<String>. The actual value '"+value+"'(type: '"+value.getClass() 
                + "') is NOT supported");            
        }
        //if first == false we we have not found any non blank line -> return null!
        return !first ? sb.toString() : null;
    }
    /**
     * The dictionary parsed to this configuration object. NOTE: that changes
     * to this dictionary will not be reflected by the getters of this class.
     * @return the dictionary holding the parsed configuration (expected to 
     * be used as read-only).
     */
    public Dictionary<String,Object> getDict(){
        return config;
    }
    
    /**
     * If the {@link DereferenceConstants#FALLBACK_MODE} is active or inactive
     * @return the fallback mode state
     */
    public boolean isFallbackMode(){
    	Object value = config.get(FALLBACK_MODE);
    	return value == null ? DereferenceConstants.DEFAULT_FALLBACK_MODE :
    		Boolean.parseBoolean(value.toString());
    }
    
    /**
     * The configured {@link DereferenceConstants#URI_PATTERN}
     * @return the URI patterns. An empty List if none
     */
    public List<String> getUriPatterns(){
    	Object value = config.get(DereferenceConstants.URI_PATTERN);
    	return getStrValues(value);
    }
    /**
     * The configured {@link DereferenceConstants#URI_PREFIX}
     * @return the URI prefixes. An empty List if none
     */
    public List<String> getUriPrefixes(){
    	Object value = config.get(DereferenceConstants.URI_PREFIX);
    	return getStrValues(value);
    }
	/**
	 * Extracts String values from the parsed value.
	 * @param value the value (String, String[] or Collection<?>
	 * @return the values as List in the parsed order
	 */
	private List<String> getStrValues(Object value) {
		final List<String> values;
    	if(value instanceof String){
    		values = StringUtils.isBlank(((String)value)) ? 
    				Collections.<String>emptyList() : 
    					Collections.singletonList((String)value);
    	} else if(value instanceof String[]){
    		values = new ArrayList<String>();
    		for(String pattern : (String[])value){
    			if(!StringUtils.isBlank(pattern)){
    				values.add(pattern);
    			}
    		}
    	} else if(value instanceof Collection<?>){
    		values = new ArrayList<String>();
    		for(Object pattern : (Collection<?>)value){
    			if(pattern != null && StringUtils.isBlank(pattern.toString())){
    				values.add(pattern.toString());
    			}
    		}    		
    	} else {
    		values = Collections.emptyList();
    	}
    	return values;
	}
	/**
	 * Getter for Literal languages that should be dereferenced.
	 * @return
	 */
	public Collection<String> getLanaguages(){
	    return languages;
	}
	/**
	 * parses the {@link DereferenceConstants#DEREFERENCE_ENTITIES_LANGUAGES} property
	 * @return
	 */
	private Set<String> parseLanguages() throws ConfigurationException {
	    Collection<String> values = EnhancementEngineHelper.getConfigValues(
	        config, DEREFERENCE_ENTITIES_LANGUAGES, String.class);
	    if(values == null){
	        return null;
	    } else {
            Set<String> languages = new HashSet<String>(values.size());
	        for(String value : values){
	            addLanguage(languages, value);
	        }
	        return languages;
	    }
	}
	
    /**
     * Parses a language from the parsed languate string and adds it to the
     * parsed languages set. This converts languages to lower case and also
     * checks for the {@link DereferenceConstants#NO_LANGUAGE_KEY} value.
     * @param languages the set holding the languages
     * @param lang the language
     */
    private void addLanguage(Set<String> languages, String lang) {
        if(!StringUtils.isBlank(lang)){
            if(NO_LANGUAGE_KEY.equalsIgnoreCase(lang)){
                languages.add(null);
            } else {
                languages.add(lang.toLowerCase(Locale.ROOT));
            }
        }
    }
}
