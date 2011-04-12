package org.apache.stanbol.entityhub.core.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping.MappingState;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol.SymbolState;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link SiteConfiguration} interface with
 * getter and setters.<p>
 * Site configurations are read only and usually set during the activation and
 * through accessible via the {@link ReferencedSite} interface they MUST NOT
 * be changed by other components. Because of this there is the possibility to
 * create readonly instances of this class. In this case all setter methods will
 * throw {@link UnsupportedOperationException}s.
 * 
 * @author Rupert Westenthaler
 *
 */
public class DefaultSiteConfiguration implements SiteConfiguration {
    /**
     * The logger
     */
    private final static Logger log = LoggerFactory.getLogger(DefaultSiteConfiguration.class);

    /**
     * Internally used to store the configuration.
     */
    private Map<String,Object> config;
    /**
     * The readonly switch (only set by the constructor
     */
    private final boolean readonly;
    
    private DefaultSiteConfiguration(boolean readonly){
        this.readonly = readonly;
        this.config = new HashMap<String,Object>();
    }
    /**
     * Creates a configuration based on the parsed parameter. The parsed 
     * configuration is validated.<p>
     * Changes to the parsed configuration do have no affect on the state of 
     * the created instance.
     * @param readonly if <code>true</code> the configuration can not be
     * be modified after creation. Calling any setter method will result in
     * {@link UnsupportedOperationException}s to be thrown
     * @param parsed the configuration used for the initialisation.
     * @throws ConfigurationException if the parsed properties are not valid
     */
    public DefaultSiteConfiguration(boolean readonly,Map<String,Object> parsed) throws ConfigurationException {
        this(readonly);
        //now add the parsed configuration
        if(parsed != null){
            //use local variable because the field might be a read only wrapper!
            config.putAll(parsed);
        }
        //validate the configuration and transform values to the preferred
        //type (e.g. strings to enums)
        validateConfiguration();
        //if readonly replace with an unmodifiable map
        if(readonly){
            this.config = Collections.unmodifiableMap(config);
        }
    }
    /**
     * Constructs an empty read/write configuration
     */
    public DefaultSiteConfiguration(){
       this(false);
    }
    /**
     * Constructs an readonly instance based on the parsed configuration. The parsed 
     * configuration is validated.<p>
     * Changes to the parsed configuration do have no affect on the state of 
     * the created instance.
     * @param config the configuration to use
     * @throws ConfigurationException if the parsed properties are not valid
     */
    public DefaultSiteConfiguration(Map<String,Object> config) throws ConfigurationException {
        this(true,config);
    }
    /**
     * Validates if the current configuration is valid and also perform type
     * transformations on values (e.g. converting string values to the enumerated 
     * types).<p>
     * This Method requires write access to the configuration and will therefore
     * fail in case this instance is readonly. Constructors that do have an
     * config as parameter call this method to validate the configuration.
     * @throws ConfigurationException if the validation of an property fails 
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     */
    public void validateConfiguration() throws ConfigurationException, UnsupportedOperationException {
        if(getId() == null || getId().isEmpty()){
            throw new ConfigurationException(ID, "The id of a ReferencedSite configuration MUST NOT be NULL nor empty!");
        }
        //check if all the Enumerated values are valid strings and convert them
        //to enumeration instances
        try {
            setDefaultMappedEntityState(getDefaultMappedEntityState());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(DEFAULT_MAPPING_STATE,
                String.format("Unknown default MappingState (%s=%s) for Site %s! Valid values are %s ",
                    DEFAULT_MAPPING_STATE,config.get(DEFAULT_MAPPING_STATE),getId(),
                        Arrays.toString(MappingState.values())),e);
        }
        try {
            setDefaultSymbolState(getDefaultSymbolState());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(DEFAULT_SYMBOL_STATE, 
                String.format("Unknown default SymbolState (%s=%s) for Site %s! Valid values are %s ",
                    DEFAULT_SYMBOL_STATE,config.get(DEFAULT_SYMBOL_STATE),getId(),
                        Arrays.toString(SymbolState.values()),e));
        }
        try {
            setCacheStrategy(getCacheStrategy());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(CACHE_STRATEGY, 
                String.format("Unknown CachStrategy (%s=%s) for Site %s! Valid values are %s ",
                    CACHE_STRATEGY,config.get(CACHE_STRATEGY),getId(),
                        Arrays.toString(CacheStrategy.values()),e));
        }
        //check if the default expire duration is a number
        try {
            setDefaultExpireDuration(getDefaultExpireDuration());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(DEFAULT_EXPIRE_DURATION, 
                String.format("Unable to parse Number for %s=%s. Will return -1 (no expire duration)",
                    DEFAULT_EXPIRE_DURATION,config.get(DEFAULT_EXPIRE_DURATION)),e);
        }
        //check if the prefixes can be converted to an String[]
        try {
            setEntityPrefixes(getEntityPrefixes());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(ENTITY_PREFIX, e.getMessage(),e);
        }
        //check if the fieldMappings can be converted to an String[]
        try {
            setFieldMappings(getFieldMappings());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(SITE_FIELD_MAPPINGS, e.getMessage(),e);
        }
        //check that a cacheId is set if the CacheStrategy != none
        if(CacheStrategy.none != getCacheStrategy() && getCacheId() == null){
            throw new ConfigurationException(CACHE_ID, 
                String.format("The CacheID (%s) MUST NOT be NULL nor empty if the the CacheStrategy != %s",
                    CACHE_ID,CacheStrategy.none));
        }
        //check that a accessUri and an entity dereferencer is set if the 
        //cacheStrategy != CacheStrategy.all
        if(CacheStrategy.all != getCacheStrategy()){
            if(getAccessUri() == null){
                throw new ConfigurationException(ACCESS_URI, 
                    String.format("An AccessUri (%s) MUST be configured if the CacheStrategy != %s",
                        ACCESS_URI,CacheStrategy.all));
            }
            if(getEntityDereferencerType() == null){
                throw new ConfigurationException(ENTITY_DEREFERENCER_TYPE, 
                    String.format("An EntityDereferencer (%s) MUST be configured if the CacheStrategy != %s",
                        ENTITY_DEREFERENCER_TYPE,CacheStrategy.all));
            }
        }
    }
    /**
     * Getter for the readonly state.
     * @return if <code>true</code> this configuration is readonly and all
     * setter methods will throw {@link UnsupportedOperationException}s.
     */
    public final boolean isReadonly() {
        return readonly;
    }
    
    @Override
    public final String getAccessUri() {
        Object accessUri = config.get(ACCESS_URI);
        return accessUri == null?null:accessUri.toString();
    }
    /**
     * 
     * @param uri
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getAccessUri()
     */
    public final void setAccessUri(String uri) throws UnsupportedOperationException{
        if(uri == null){
            config.remove(ACCESS_URI);
        } else {
            config.put(ACCESS_URI, uri);
        }
    }
    
    @Override
    public final String getAttribution() {
        Object attribution = config.get(SITE_ATTRIBUTION);
        return attribution == null || attribution.toString().isEmpty() ?
                null : attribution.toString();
    }
    /**
     * 
     * @param attribution
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getAttribution()
     */
    public final void setAttribution(String attribution) throws UnsupportedOperationException {
        if(attribution == null){
            config.remove(SITE_ATTRIBUTION);
        } else {
            config.put(SITE_ATTRIBUTION, attribution);
        }
    }

    @Override
    public final String getCacheId() {
        Object id = config.get(CACHE_ID);
        return id == null || id.toString().isEmpty() ? 
                null : id.toString();
    }
    /**
     * 
     * @param id
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getCacheId()
     */
    public final void setCacheId(String id) throws UnsupportedOperationException {
        if(id == null || id.isEmpty()){
            config.remove(CACHE_ID);
        } else {
            config.put(CACHE_ID, id);
        }
    }

    @Override
    public final CacheStrategy getCacheStrategy() {
        Object cacheStrategy = config.get(CACHE_STRATEGY);
        if(cacheStrategy == null){
            return null;
        } else if(cacheStrategy instanceof CacheStrategy){
            return (CacheStrategy)cacheStrategy;
        } else {
            return CacheStrategy.valueOf(cacheStrategy.toString());
        }
    }
    /**
     * 
     * @param strategy
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getCacheStrategy()
     */
    public final void setCacheStrategy(CacheStrategy strategy) throws UnsupportedOperationException {
        if(strategy == null){
            config.remove(CACHE_STRATEGY);
        } else {
            config.put(CACHE_STRATEGY, strategy);
        }
    }
    
    @Override
    public final long getDefaultExpireDuration() {
        Object duration = config.get(DEFAULT_EXPIRE_DURATION);
        if(duration == null){
            return 0;
        } else if(duration instanceof Long){
            return (Long) duration;
        } else {
            return Long.valueOf(duration.toString());
        }
    }
    /**
     * 
     * @param duration
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDefaultExpireDuration()
     */
    public final void setDefaultExpireDuration(long duration) throws UnsupportedOperationException {
        if(duration <= 0){
            config.remove(DEFAULT_EXPIRE_DURATION);
        } else {
            config.put(DEFAULT_EXPIRE_DURATION, duration);
        }
    }
    
    @Override
    public final MappingState getDefaultMappedEntityState() {
        Object defaultMappingState = config.get(DEFAULT_MAPPING_STATE);
        if(defaultMappingState == null){
            return null;
        } else if(defaultMappingState instanceof MappingState){
            return (MappingState)defaultMappingState;
        } else {
            return MappingState.valueOf(defaultMappingState.toString());
        }
    }
    /**
     * 
     * @param state
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDefaultMappedEntityState()
     */
    public final void setDefaultMappedEntityState(MappingState state) throws UnsupportedOperationException {
        if(state == null){
            config.remove(DEFAULT_MAPPING_STATE);
        } else {
            config.put(DEFAULT_MAPPING_STATE, state);
        }
    }

    @Override
    public final SymbolState getDefaultSymbolState() {
        Object defaultSymbolState = config.get(DEFAULT_SYMBOL_STATE);
        if(defaultSymbolState == null){
            return null;
        } else if(defaultSymbolState instanceof SymbolState){
            return (SymbolState)defaultSymbolState;
        } else {
            return SymbolState.valueOf(defaultSymbolState.toString());
        }
    }
    /**
     * 
     * @param state
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDefaultSymbolState()
     */
    public final void setDefaultSymbolState(SymbolState state) throws UnsupportedOperationException {
        if(state == null){
            config.remove(DEFAULT_SYMBOL_STATE);
        } else {
            config.put(DEFAULT_SYMBOL_STATE, state);
        }
    }
    @Override
    public final String getEntityDereferencerType() {
        Object dereferencer = config.get(ENTITY_DEREFERENCER_TYPE);
        return dereferencer == null ||dereferencer.toString().isEmpty() ?
                null : dereferencer.toString();
    }
    /**
     * 
     * @param entityDereferencerType
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getEntityDereferencerType()
     */
    public final void setEntityDereferencerType(String entityDereferencerType) throws UnsupportedOperationException {
        if(entityDereferencerType == null){
            config.remove(entityDereferencerType);
        } else {
            config.put(ENTITY_DEREFERENCER_TYPE, entityDereferencerType);
        }
    }
    
    @Override
    public final String getDescription() {
        Object description = config.get(DESCRIPTION);
        return description == null || description.toString().isEmpty() ? 
                null : description.toString();
    }
    /**
     * 
     * @param description
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDescription()
     */
    public final void setDescription(String description) throws UnsupportedOperationException {
        if(description == null){
            config.remove(DESCRIPTION);
        } else {
            config.put(DESCRIPTION, description);
        }
    }
    @Override
    public String[] getFieldMappings() {
        Object fieldMappings = config.get(SITE_FIELD_MAPPINGS);
        if(fieldMappings == null){
            return null;
        } else if (fieldMappings instanceof String[]){
            return (String[]) fieldMappings;
        } else if (fieldMappings instanceof Iterable<?>){
            Collection<String> prefixes = new ArrayList<String>();
            for(Object value : (Iterable<?>)fieldMappings){
                if(value != null && value.toString().isEmpty()){
                    prefixes.add(value.toString());
                }
            }
            return prefixes.toArray(new String[prefixes.size()]);
        } else if(fieldMappings instanceof String) {
            return new String[]{fieldMappings.toString()};
        } else {
            throw new IllegalArgumentException(
                String.format("Unable to parse FieldMappings form class %s (supported are String, String[] and Iterables)",
                    fieldMappings.getClass()));
        }
    }
    public final void setFieldMappings(String[] mappings) throws UnsupportedOperationException {
        if(mappings == null){
            config.remove(SITE_FIELD_MAPPINGS);
        } else {
            config.put(SITE_FIELD_MAPPINGS, mappings);
        }
    }
    
    @Override
    public final String[] getEntityPrefixes() {
        Object entityPrefixes = config.get(ENTITY_PREFIX);
        if(entityPrefixes == null){
            return null;
        } else if (entityPrefixes instanceof String[]){
            return (String[]) entityPrefixes;
        } else if (entityPrefixes instanceof Iterable<?>){
            Collection<String> prefixes = new ArrayList<String>();
            for(Object value : (Iterable<?>)entityPrefixes){
                if(value != null){
                    prefixes.add(value.toString());
                }
            }
            return prefixes.toArray(new String[prefixes.size()]);
        } else if(entityPrefixes instanceof String) {
            return new String[]{entityPrefixes.toString()};
        } else {
            throw new IllegalArgumentException(
                String.format("Unable to parse EnityPrefixes form class %s (supported are String String[] and Iterables)",
                    entityPrefixes.getClass()));
        }
    }
    /**
     * 
     * @param prefixes
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getEntityPrefixes()
     */
    public final void setEntityPrefixes(String[] prefixes) throws UnsupportedOperationException {
        if(prefixes == null){
            config.remove(ENTITY_PREFIX);
        } else {
            config.put(ENTITY_PREFIX, prefixes);
        }
    }
    
    @Override
    public final String getId() {
        Object id = config.get(ID);
        return id == null ? null : id.toString();
    }
    /**
     * 
     * @param id
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @throws IllegalArgumentException in case the parsed ID is NULL or an empty String
     * @see #getId()
     */
    public final void setId(String id) throws UnsupportedOperationException, IllegalArgumentException {
        if(id == null){
            throw new IllegalArgumentException("The ID of the Site MUST NOT be set to NULL!");
        } else if (id.isEmpty()){
            throw new IllegalArgumentException("The ID of the Site MIST NOT be set to an empty String!");
        } else {
            config.put(ID, id);
        }
    }
    
    @Override
    public final String getLicenseName() {
        Object name = config.get(SITE_LICENCE_NAME);
        return name == null || name.toString().isEmpty() ? null : name.toString();
    }
    /**
     * 
     * @param name
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getLicenseName()
     */
    public final void setLicenseName(String name) throws UnsupportedOperationException {
        if(name == null){
            config.remove(SITE_LICENCE_NAME);
        } else {
            config.put(SITE_LICENCE_NAME, name);
        }
    }

    @Override
    public String getLicenseText() {
        Object text = config.get(SITE_LICENCE_TEXT);
        return text == null || text.toString().isEmpty() ? null : text.toString();
    }
    /**
     * 
     * @param licenseText
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getLicenseText()
     */
    public void setLicenseText(String licenseText) throws UnsupportedOperationException {
        if(licenseText == null){
            config.remove(SITE_LICENCE_TEXT);
        } else {
            config.put(SITE_LICENCE_TEXT, licenseText);
        }
    }

    @Override
    public String getLicenseUrl() {
        Object url = config.get(SITE_LICENCE_URL);
        return url == null || url.toString().isEmpty() ? null : url.toString();
    }
    /**
     * 
     * @param licenseUrl
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getLicenseUrl()
     */
    public final void setLicenseUrl(String licenseUrl) throws UnsupportedOperationException {
        if(licenseUrl == null){
            config.remove(SITE_LICENCE_URL);
        } else {
            config.put(SITE_LICENCE_URL, licenseUrl);
        }
    }

    @Override
    public String getName() {
        Object name = config.get(NAME);
        //use ID as fallback!
        return name == null || name.toString().isEmpty() ? getId() : name.toString();
    }
    /**
     * 
     * @param name
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getName()
     */
    public final void setName(String name) throws UnsupportedOperationException {
        if(name == null){
            config.remove(NAME);
        } else {
            config.put(NAME, name);
        }
    }

    @Override
    public String getEntitySearcherType() {
        Object type = config.get(ENTITY_SEARCHER_TYPE);
        return type == null || type.toString().isEmpty() ? null : type.toString();
    }
    /**
     * 
     * @param entitySearcherType
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getEntitySearcherType()
     */
    public final void setEntitySearcherType(String entitySearcherType) throws UnsupportedOperationException {
        if(entitySearcherType == null){
            config.remove(ENTITY_SEARCHER_TYPE);
        } else {
            config.put(ENTITY_SEARCHER_TYPE, entitySearcherType);
        }
    }
    @Override
    public String getQueryUri() {
        Object uri = config.get(QUERY_URI);
        return uri == null || uri.toString().isEmpty() ? null : uri.toString();
    }
    /**
     * 
     * @param queryUri
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getQueryUri()
     */
    public final void setQueryUri(String queryUri) throws UnsupportedOperationException {
        if(queryUri == null){
            config.remove(QUERY_URI);
        } else {
            config.put(QUERY_URI, queryUri);
        }
    }
    /**
     * Provides direct access to the internal configuration
     * @return the configuration wrapped by this class
     */
    protected final Map<String,Object> getConfiguration(){
        return config;
    }

}
