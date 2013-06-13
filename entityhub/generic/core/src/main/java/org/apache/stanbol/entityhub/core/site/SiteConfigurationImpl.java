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
package org.apache.stanbol.entityhub.core.site;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.site.License;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link SiteConfiguration} interface.<p>
 * While this implementation also provides setter methods when used within an
 * OSGI environment configurations are typically read only as the configuration
 * is provided as parameter to the component activation method.
 * 
 * @author Rupert Westenthaler
 *
 */
public class SiteConfigurationImpl implements SiteConfiguration {
    /**
     * The logger
     */
    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(SiteConfigurationImpl.class);

    /**
     * Internally used to store the configuration.
     */
    protected final Dictionary<String,Object> config;
    
    /**
     * Creates a configuration based on the parsed parameter. The parsed 
     * configuration is validated.<p>
     * Changes to the parsed configuration do have no affect on the state of 
     * the created instance.<p>
     * OSGI specific metadata are removed from the parsed configuration.
     * @param parsed the configuration used for the initialisation.
     * @throws ConfigurationException if the parsed properties are not valid
     */
    protected SiteConfigurationImpl(Dictionary<String,Object> parsed) throws ConfigurationException {
        this();
        //now add the parsed configuration
        if(parsed != null){
            //copy over the elements
            for(Enumeration<String> it = parsed.keys();it.hasMoreElements();) {
                String key = it.nextElement();
                config.put(key,parsed.get(key));
            }
            //Remove OSGI specific metadata
            config.remove(Constants.SERVICE_ID);
            config.remove(Constants.SERVICE_PID);
            config.remove(Constants.OBJECTCLASS);
            config.remove(Constants.SYSTEM_BUNDLE_LOCATION);
            config.remove(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
        }
        validateConfiguration();
    }
    /**
     * Constructs an empty configuration
     */
    protected SiteConfigurationImpl(){
       this.config = new Hashtable<String,Object>();
    }
    /**
     * Validates if the current configuration is valid and also perform type
     * transformations on values (e.g. converting string values to the enumerated 
     * types). if the validation fails an Exception MUST BE thrown.<p>
     * @throws ConfigurationException if the validation fails 
     */
    protected void validateConfiguration() throws ConfigurationException {
        if(getId() == null || getId().isEmpty()){
            throw new ConfigurationException(ID, "The id of a ReferencedSite configuration MUST NOT be NULL nor empty!");
        }
        //check if the prefixes can be converted to an String[]
        try {
            setEntityPrefixes(getEntityPrefixes());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(ENTITY_PREFIX, e.getMessage(),e);
        }
        //check the configured licenses and create the License array
        setLicenses(getLicenses());
        //check if the fieldMappings can be converted to an String[]
        try {
            setFieldMappings(getFieldMappings());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(SITE_FIELD_MAPPINGS, e.getMessage(),e);
        }
        try {
            setDefaultMappedEntityState(getDefaultMappedEntityState());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(DEFAULT_MAPPING_STATE,
                String.format("Unknown default MappingState (%s=%s) for Site %s! Valid values are %s ",
                    DEFAULT_MAPPING_STATE,config.get(DEFAULT_MAPPING_STATE),getId(),
                        Arrays.toString(MappingState.values())),e);
        }
        try {
            setDefaultManagedEntityState(getDefaultManagedEntityState());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(DEFAULT_SYMBOL_STATE, 
                String.format("Unknown default SymbolState (%s=%s) for Site %s! Valid values are %s ",
                    DEFAULT_SYMBOL_STATE,config.get(DEFAULT_SYMBOL_STATE),getId(),
                        Arrays.toString(ManagedEntityState.values()),e));
        }
        //check if the default expire duration is a number
        try {
            setDefaultExpireDuration(getDefaultExpireDuration());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(DEFAULT_EXPIRE_DURATION, 
                String.format("Unable to parse Number for %s=%s. Will return -1 (no expire duration)",
                    DEFAULT_EXPIRE_DURATION,config.get(DEFAULT_EXPIRE_DURATION)),e);
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
     * Setter for the default state of Mappings created between Entities of this
     * Site and Entities managed by the Entityhub. If this configuration is not
     * present created mappings will have the default state as configured by the
     * Entityhub.
     * @param state the default state for new Entity mappings.
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
    public final ManagedEntityState getDefaultManagedEntityState() {
        Object defaultSymbolState = config.get(DEFAULT_SYMBOL_STATE);
        if(defaultSymbolState == null){
            return null;
        } else if(defaultSymbolState instanceof ManagedEntityState){
            return (ManagedEntityState)defaultSymbolState;
        } else {
            return ManagedEntityState.valueOf(defaultSymbolState.toString());
        }
    }
    /**
     * Setter for the default state of Entities after importing them into the
     * Entityhub. If this configuration is not present Entities will have the
     * default state set for the Entityhub.
     * @param state the state or <code>null</code> to remove this configuration
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDefaultManagedEntityState()
     */
    public final void setDefaultManagedEntityState(ManagedEntityState state) throws UnsupportedOperationException {
        if(state == null){
            config.remove(DEFAULT_SYMBOL_STATE);
        } else {
            config.put(DEFAULT_SYMBOL_STATE, state);
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
        if(attribution == null  || attribution.isEmpty()){
            config.remove(SITE_ATTRIBUTION);
        } else {
            config.put(SITE_ATTRIBUTION, attribution);
        }
    }
    @Override
    public String getAttributionUrl() {
        Object attribution = config.get(SITE_ATTRIBUTION_URL);
        return attribution == null || attribution.toString().isEmpty() ?
                null : attribution.toString();
    }
    /**
     * 
     * @param attribution
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getAttribution()
     */
    public final void setAttributionUrl(String attributionUrl) throws UnsupportedOperationException {
        if(attributionUrl == null || attributionUrl.isEmpty()){
            config.remove(SITE_ATTRIBUTION_URL);
        } else {
            config.put(SITE_ATTRIBUTION_URL, attributionUrl);
        }
    }


    
    @Override
    public final String getDescription() {
        Object description = config.get(DESCRIPTION);
        return description == null || description.toString().isEmpty() ? 
                null : description.toString();
    }
    /**
     * Setter for the description of the {@link Site}. If set to
     * <code>null</code> or an empty string this configuration will be removed.
     * @param description the description
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDescription()
     */
    public final void setDescription(String description) throws UnsupportedOperationException {
        if(description == null || description.isEmpty()){
            config.remove(DESCRIPTION);
        } else {
            config.put(DESCRIPTION, description);
        }
    }
    
    @Override
    public final String[] getEntityPrefixes() {
        String[] prefixes = getStringValues(ENTITY_PREFIX);
        return prefixes == null ? new String[]{} : prefixes;
    }
    /**
     * Setter for the Entity prefixes (typically the namespace or the host name)
     * of the entities provided by this site. Because Sites might provide Entities
     * with different namespaces this site allows to parse an array. Setting the
     * prefixes to <code>null</code> or an empty array will cause that this site
     * is ask for all requested entities.
     * @param prefixes The array with the prefixes or <code>null</code> to 
     * remove any configured prefixes
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getEntityPrefixes()
     */
    public final void setEntityPrefixes(String[] prefixes) throws UnsupportedOperationException {
        if(prefixes == null || prefixes.length < 1){
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
     * Setter for the id of the referenced site
     * @param id the id
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @throws IllegalArgumentException in case the parsed ID is <code>null</code> or an empty String
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
    public final License[] getLicenses() {
        //get Licenses based on related keys
        int elements = 0;
        String[] names = getLicenseName();
        if(names == null){
            names = new String[]{};
        } else {
            elements = Math.max(elements, names.length);
        }
        String[] texts = getLicenseText();
        if(texts == null){
            texts = new String[]{};
        } else {
            elements = Math.max(elements, texts.length);
        }
        String[] urls = getLicenseUrl();
        if(urls == null){
            urls = new String[]{};
        } else {
            elements = Math.max(elements, urls.length);
        }
        Collection<License> licenseList = new ArrayList<License>();
        for(int i=0;i<elements;i++){
            try {
                licenseList.add(new License(
                    names.length>i?names[i]:null,
                            urls.length>i?urls[i]:null,
                                    texts.length>i?texts[i]:null));
            } catch(IllegalArgumentException e){
                //ignore if name, text and url == null and/or empty
            }
        }
        return licenseList.isEmpty() ? new License[]{} : 
            licenseList.toArray(new License[licenseList.size()]);
    }
    /**
     * Setter for the {@link License} information. This method stores the name,
     * text and url of the license as strings in the according fields of the
     * configuration.
     * @param licenses the licenses to store. <code>null</code> or an empty
     * array to remove existing values
     * @throws IllegalArgumentException if the parsed array contains a <code>null</code>
     * element
     * @throws UnsupportedOperationException if the configuration is read-only
     */
    public final void setLicenses(License[] licenses) throws IllegalArgumentException, UnsupportedOperationException {
        if(licenses == null || licenses.length < 1){
            config.remove(SITE_LICENCE_NAME);
            config.remove(SITE_LICENCE_TEXT);
            config.remove(SITE_LICENCE_URL);
        } else {
            String[] names = new String[licenses.length];
            String[] texts = new String[licenses.length];
            String[] urls = new String[licenses.length];
            for(int i=0;i<licenses.length;i++){
                if(licenses[i] != null){
                    names[i] = licenses[i].getName();
                    texts[i] = licenses[i].getText();
                    urls[i] = licenses[i].getUrl();
                } else {
                    throw new IllegalArgumentException("The parsed License array" +
                    		"MUST NOT contain a NULL element! (parsed: "+
                    		Arrays.toString(licenses)+")");
                }
            }
            config.put(SITE_LICENCE_NAME, names);
            config.put(SITE_LICENCE_TEXT, texts);
            config.put(SITE_LICENCE_URL, urls);
        }
    }
    
    /**
     * Internally used to get the names of the licenses
     * @return
     */
    private String[] getLicenseName() {
        return getStringValues(SITE_LICENCE_NAME);
    }
    /**
     * Internally used to get the texts of the licenes
     * @return
     */
    private String[] getLicenseText() {
        return getStringValues(SITE_LICENCE_TEXT);
    }
    /**
     * Internally used to get the urls of the page describing the license
     * @return
     */
    private String[] getLicenseUrl() {
        return getStringValues(SITE_LICENCE_URL);
    }

    @Override
    public String getName() {
        Object name = config.get(NAME);
        //use ID as fallback!
        return name == null || name.toString().isEmpty() ? getId() : name.toString();
    }
    /**
     * Setter for the name of the Referenced Site. Note that if the name is not
     * present the {@link #getId() id} will be used as name.
     * @param name the name of the site or <code>null</code> to remove it (and
     * use the {@link #getId() id} also as name
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getName()
     */
    public final void setName(String name) throws UnsupportedOperationException {
        if(name == null || name.isEmpty()){
            config.remove(NAME);
        } else {
            config.put(NAME, name);
        }
    }

    /**
     * Provides direct access to the internal configuration
     * @return the configuration wrapped by this class
     */
    public final Dictionary<String,Object> getConfiguration(){
        return config;
    }
    
    @Override
    public String[] getFieldMappings() {
        return getStringValues(SITE_FIELD_MAPPINGS);
    }
    /**
     * Setter for the mappings of a site. This mappings are used in case an 
     * Entity of this site is imported to the Entityhub. Parsing <code>null</code>
     * or an empty array will cause all existing mappings to be removed.
     * @param mappings the mappings
     * @throws UnsupportedOperationException
     */
    public final void setFieldMappings(String[] mappings) throws UnsupportedOperationException {
        if(mappings == null || mappings.length < 1){
            config.remove(SITE_FIELD_MAPPINGS);
        } else {
            config.put(SITE_FIELD_MAPPINGS, mappings);
        }
    }
    
    /**
     * Internally used to parse String[] based on key values. This method
     * supports Stirng, Stirng[] and Iterables&lt;?&gt;. For Iterables&lt;?&gt;
     * the {@link Object#toString()} is used and <code>null</code> elementes are
     * kept.
     * @return
     */
    protected final String[] getStringValues(String key) {
        Object values = config.get(key);
        if(values == null){
            return null;
        } else if (values instanceof String[]){
            if(((String[])values).length<1){ //return null if empty
                return null;
            } else {
                return (String[]) values;
            }
        } else if (values instanceof Iterable<?>){
            Collection<String> prefixes = new ArrayList<String>();
            for(Object value : (Iterable<?>)values){
                prefixes.add(value==null ? null : value.toString());
            }
            return prefixes.toArray(new String[prefixes.size()]);
        } else if(values instanceof String) {
            return new String[]{values.toString()};
        } else {
            throw new IllegalArgumentException(
                String.format("Unable to parse Sting[] for field %s form type %s (supported are String, String[] and Iterables)",
                    key,values.getClass()));
        }
    }
}
