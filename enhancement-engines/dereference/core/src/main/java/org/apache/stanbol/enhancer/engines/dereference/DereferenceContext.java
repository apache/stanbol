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

import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.DEREFERENCE_ENTITIES_LANGUAGES;
import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.NO_LANGUAGE_KEY;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DereferenceContext {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final static String INTERNAL_CONTENT_LANGUAGES = "internal.dereference.contentlanguages";
    protected final static String INTERNAL_ACCEPT_LANGUAGES = "internal.dereference.acceptlanguages";
    
    protected final EntityDereferenceEngine engine;
    
    protected final Map<String,Object> enhancementProps;
    /**
     * The {@link OfflineMode} status
     */
    private boolean offlineMode;
    /** 
     * Read-only set with literal languages defined in the context.
     */
    private Set<String> contextLanguages;
    /**
     * Read-only set with literal languages to be dereferenced. This is the
     * union over {@link #contextLanguages} and {@link #getConfig()}.
     * {@link DereferenceEngineConfig#getLanaguages() getLanaguages()}
     */
    private Set<String> languages;
    private List<String> fields;
    private String program;
    private HashSet<IRI> entityReferences;
    
    
    
    /**
     * Create a new DereferenceContext.
     * @param offlineMode the {@link OfflineMode} state
     * @param ep The enhancement properties
     * @throws DereferenceConfigurationException if the parsed enhancement
     * propertied contain an invalid configuration
     */
    @SuppressWarnings("unchecked")
    protected DereferenceContext(EntityDereferenceEngine engine, Map<String,Object> ep)
            throws DereferenceConfigurationException {
        this.engine = engine;
        this.enhancementProps = ep;
        parseLanguages(ep == null ? null : ep.get(DEREFERENCE_ENTITIES_LANGUAGES),
            ep == null ? null : (Collection<String>)ep.get(INTERNAL_CONTENT_LANGUAGES),
            ep == null ? null : (Collection<String>)ep.get(INTERNAL_ACCEPT_LANGUAGES));
        parseFields(ep == null ? null : ep.get(DereferenceConstants.DEREFERENCE_ENTITIES_FIELDS));
        parseLDPath(ep == null ? null : ep.get(DereferenceConstants.DEREFERENCE_ENTITIES_LDPATH));
        parseEntityReferences(ep == null ? null : ep.get(DereferenceConstants.ENTITY_REFERENCES));
        //call the initialisation callback
        initialise();
    }
    
    private void parseEntityReferences(Object value) throws DereferenceConfigurationException {
        Collection<String> entityRefProps;
        try{
            entityRefProps = EnhancementEngineHelper.parseConfigValues(value, String.class);
        } catch (IllegalStateException e){
            throw new DereferenceConfigurationException(e,
                engine.getDereferencer().getClass(), 
                DereferenceConstants.ENTITY_REFERENCES);
        }
        //start with the references present in the config
        this.entityReferences = new HashSet<IRI>(getConfig().getEntityReferences());
        if(entityRefProps != null && !entityRefProps.isEmpty()){
            NamespacePrefixService nps = engine.getConfig().getNsPrefixService();
            for(String prop : entityRefProps){
                if(!StringUtils.isBlank(prop)){
                    try {
                        entityReferences.add(new IRI(
                            NamespaceMappingUtils.getConfiguredUri(nps, prop)));
                    } catch(IllegalArgumentException e){
                        throw new DereferenceConfigurationException(e, 
                            engine.getDereferencer().getClass(), 
                            DereferenceConstants.ENTITY_REFERENCES);
                    }
                }
            }
        }
    }
    /**
     * Parses the {@link DereferenceConstants#DEREFERENCE_ENTITIES_LANGUAGES}
     * from the parsed value, merges content- and accept-languages and finally 
     * calls {@link #initLiteralLanguages(Set)} with the resulting set
     * @param value the value of the 
     * {@link DereferenceConstants#DEREFERENCE_ENTITIES_LANGUAGES} key
     * @param the content languages or <code>null</code> if 
     * {@link DereferenceConstants#FILTER_CONTENT_LANGUAGES} is deactivated
     * @param the accept languages or <code>null</code> if
     * {@link DereferenceConstants#FILTER_ACCEPT_LANGUAGES} is deactivated
     * @throws DereferenceConfigurationException if the parsed value is not a
     * valid configuration
     */
    private void parseLanguages(Object value, Collection<String> contentLanguages, 
            Collection<String> acceptLanguages) throws DereferenceConfigurationException {
        Set<String> languages;
        if(value == null){
            languages = null;
        } else {
            languages = new HashSet<String>();
            if(value instanceof String){
                String lang = (String) value;
                addLanguage(languages, lang);
            } else if(value instanceof Collection<?>){
                for(Object lang : (Collection<?>)value){
                    if(lang instanceof String){
                        addLanguage(languages, (String)lang);
                    }
                }           
            }
            if(languages.isEmpty()){
                languages = null;
            }
        }
        if(contentLanguages != null && !contentLanguages.isEmpty()){
            if(languages == null){
                languages = new HashSet<String>();
            }
            languages.addAll(contentLanguages);
        }
        if(acceptLanguages != null && !acceptLanguages.isEmpty()){
            if(languages == null){
                languages = new HashSet<String>();
            }
            languages.addAll(acceptLanguages);
        }
        //set the contextLanguages field
        if(languages == null){
            this.contextLanguages = Collections.emptySet();
        } else {
            this.contextLanguages = Collections.unmodifiableSet(languages);
        }
        //merge the languages with those of the config and set the languages field
        Set<String> merged;
        Collection<String> configLangs = getConfig().getLanaguages();
        if(languages == null && configLangs == null){
            merged = null;
        } else if(configLangs == null){
            merged = languages;
        } else {
            merged = new HashSet<String>(configLangs);
            if(languages != null){
                merged.addAll(languages);
            }
        }
        this.languages = merged;

    }
    /**
     * Parsed the language from the language string and adds it to the languages
     * set. This will convert languages to lower case and also converts empty
     * values as well as the {@link DereferenceConstants#NO_LANGUAGE_KEY} to 
     * <code>null</code> (indicating labels without any language) 
     * @param languages
     * @param lang
     */
    private void addLanguage(Set<String> languages, String lang) {
        if(StringUtils.isBlank(lang) || NO_LANGUAGE_KEY.equalsIgnoreCase(lang)){
            languages.add(null);
        } else {
            languages.add(lang.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Parsed the {@link DereferenceConstants#DEREFERENCE_ENTITIES_FIELDS}
     * from the parsed value and calls {@link #initDereferencedFields(List)} with 
     * the parsed value
     * @param value the value
     * @throws DereferenceConfigurationException if the parsed value is not a
     * valid configuration
     */
    private void parseFields(Object value) throws DereferenceConfigurationException{
        List<String> fields;
        if(value instanceof String && !StringUtils.isBlank((String)value)){
            fields = Collections.singletonList((String)value);
        } else if(value instanceof Collection<?>){
            fields = new ArrayList<String>(((Collection<?>)value).size());
            for(Object field : (Collection<?>)value){
                if(field != null && field instanceof String && 
                        !StringUtils.isBlank((String)field)){
                    fields.add((String)field);
                } // else ignore
            }           
        } else {
            fields = null;
        }
        this.fields  = fields;
    }
    
    /**
     * Parsed the {@link DereferenceConstants#DEREFERENCE_ENTITIES_LDPATH}
     * from the parsed value and calls {@link #initLdPathProgram(String)} with 
     * the parsed value
     * @param value the value
     * @throws DereferenceConfigurationException if the parsed value is not a
     * valid configuration
     */
    private void parseLDPath(Object value) throws DereferenceConfigurationException {
        String program;
        if(value instanceof String && !StringUtils.isBlank((String)value)){
            program = (String)value;
        } else if(value instanceof Collection<?>){
            StringBuilder sb = new StringBuilder();
            for(Object field : (Collection<?>)value){
                if(field != null && field instanceof String && 
                        !StringUtils.isBlank((String)field)){
                    sb.append((String)field).append('\n');
                } // else ignore
            }
            program = sb.length() > 0 ? sb.toString() : null;
        } else {
            program = null;
        }
        this.program = program;
    }
    
    /**
     * Allows to set the offline mode state
     */
    protected final void setOfflineMode(boolean state){
        this.offlineMode = state;
    }
    
    /**
     * If the {@link OfflineMode} is active. If active Dereferencers are not
     * allowed access remote resources for dereferencing Entities.
     * @return the offline mode status
     */
    public final boolean isOfflineMode() {
        return offlineMode;
    }
            
    /**
     * Context specific DerefernecedField configuration
     * @return the context specific DereferencedField configuration or
     * <code>null</code> if none
     */
    public final List<String> getFields() {
        return fields;
    }
    
    /**
     * Initialisation callback for the DereferenceContext. This is called by
     * the constructor after the {@link #enhancementProps} are set and
     * {@link #getLanguages()}, {@link #getFields()} and 
     * {@link #getLdPathProgram()} are initialised.<p>
     * The default implementation is empty.
     */
    protected void initialise(){
        
    }
    /**
     * Context specific LDPath program
     * @return the context specific LDPath program or <code>null</code> if none
     */
    public final String getLdPathProgram() {
        return program;
    }
    /**
     * The property URIs that may refer to Entities that need to be dereferenced.
     * This is the union view over properties parsed as EnhancementProperties
     * with properties configured with the engine
     * @return the entity reference properties
     * @see DereferenceEngineConfig#getEntityReferences()
     */
    public HashSet<IRI> getEntityReferences() {
        return entityReferences;
    }
    
    /**
     * Getter for the languages that should be dereferenced. If 
     * empty all languages should be included. This is the union over
     * Languages enabled in the context and 
     * {@link #getConfig()}.{@link DereferenceEngineConfig#getLanaguages()
     * getLanaguages()}
     * @return the languages for literals that should be dereferenced.
     */
    public final Set<String> getLanguages() {
        return languages;
    }
    
    /**
     * Set of languages enabled via the context. This does not include languages
     * enabled in the {@link DereferenceEngineConfig}
     * @return the set of languages enabled via the context. 
     */
    protected final Set<String> getContextLanguages(){
        return contextLanguages;
    }
    
    /**
     * Getter for the Enhancement Properties for this Context.
     * @return the Enhancement Properties
     */
    protected final Map<String,Object> getEnhancementProps() {
        return enhancementProps;
    }
    
    /**
     * Getter for the Dereference Engine Configuration
     * @return the dereference configuration
     */
    public final DereferenceEngineConfig getConfig() {
        return engine.getConfig();
    }
    
    /**
     * The EntityDereferencer this context is built for
     * @return the entity dereferencer
     */
    public final EntityDereferencer getDereferencer(){
        return engine.getDereferencer();
    }
    
}
