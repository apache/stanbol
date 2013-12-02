package org.apache.stanbol.enhancer.engines.dereference;

import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.DEREFERENCE_ENTITIES_FIELDS;
import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.DEREFERENCE_ENTITIES_LDPATH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;

public class DereferenceEngineConfig implements DereferenceConstants {

    
    private final Dictionary<String,Object> config;
    
    /**
     * Creates a DereferenceEngine configuration based on a Dictionary. Typically
     * the dictionary will contain keys as defined by {@link DereferenceConstants}
     * and {@link EnhancementEngine}
     * @param config the config - typically as parsed in the activate method of
     * an OSGI component.
     */
    public DereferenceEngineConfig(Dictionary<String,Object> config) throws ConfigurationException {
        this.config = config;
        validateRequired(config);
    }
    /**
     * Constructor that parses the config (ATM only used by unit tests)
     * @param name
     * @param filterContentLang
     * @param filterAcceptLang
     * @throws ConfigurationException
     */
    protected DereferenceEngineConfig(String name, boolean filterContentLang, boolean filterAcceptLang) 
            throws ConfigurationException {
        config = new Hashtable<String,Object>();
        config.put(EnhancementEngine.PROPERTY_NAME, name);
        config.put(FILTER_CONTENT_LANGUAGES, filterContentLang);
        config.put(FILTER_ACCEPT_LANGUAGES, filterAcceptLang);
        validateRequired(config);
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
    
    private void validateRequired(Dictionary<String,Object> config) throws ConfigurationException {
        Object value = config.get(EnhancementEngine.PROPERTY_NAME);
        if(value == null || StringUtils.isBlank(value.toString())){
            throw new ConfigurationException(EnhancementEngine.PROPERTY_NAME, 
                "The EnhancementEngine name MUST NOT be NULL nor empty!");
        }
        value = config.get(DEREFERENCE_ENTITIES_FIELDS);
        if(value != null && !(value instanceof String || value instanceof String[] ||
                value instanceof Collection<?>)){
            throw new ConfigurationException(DEREFERENCE_ENTITIES_FIELDS, 
                "Dereference Entities Fields MUST BE parsed as String[], Collection<String> or "
                + "String (single value). The actual value '"+value+"'(type: '"+value.getClass() 
                + "') is NOT supported");
        }
        value = config.get(DEREFERENCE_ENTITIES_LDPATH);
        if(value != null && !(value instanceof String || value instanceof String[] ||
                value instanceof Collection<?>)){
            throw new ConfigurationException(DEREFERENCE_ENTITIES_LDPATH, 
                "Dereference LDPath statements MUST BE parsed as String, String[] or "
                + "Collection<String>. The actual value '"+value+"'(type: '"+value.getClass() 
                + "') is NOT supported");            
        }        
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
    public List<String> getDereferenceFields(){
        Object value = config.get(DEREFERENCE_ENTITIES_FIELDS);
        final List<String> fields;
        if(value instanceof String[]){
            fields = Arrays.asList((String[])value);
        } else if(value instanceof Collection<?>){
            fields = new ArrayList<String>(((Collection<?>)value).size());
            for(Object field : (Collection<?>)value){
                if(field == null){
                    fields.add(null);
                } else {
                    fields.add(field.toString());
                }
            }
        } else if(value instanceof String){
            fields = Collections.singletonList((String)value);
        } else { //value == null or of unsupported type 
            fields = Collections.emptyList();
        }
        return fields;
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
    public String getLdPathProgram(){
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
        } //else unsupported type - ignore
        //if first == false we we have not found any non blank line -> return null!
        return !first ? sb.toString() : null;
    }
    /**
     * The dictionary holding the config
     * @return the dictionary holding the config
     */
    public Dictionary<String,Object> getDict(){
        return config;
    }
    
}
