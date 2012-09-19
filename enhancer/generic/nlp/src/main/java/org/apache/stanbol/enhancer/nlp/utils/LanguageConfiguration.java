package org.apache.stanbol.enhancer.nlp.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.cm.ConfigurationException;

/**
 * Utility that supports the configuration of languages in the form of
 * <pre>
 *     de,en
 * </pre>
 * of
 * <pre>
 *     !fr,!cn,*
 * </pre>
 * <p>
 * Instead of comma separated Strings also <code>String[]</code> and
 * {@link Collection} are supported.
 *
 * @author Rupert Westenthaler
 *
 */
public class LanguageConfiguration {

    private final String property;
    private final Collection<String> defaultConfig;
    //Langauge configuration
    private Set<String> configuredLanguages = new HashSet<String>();
    private Set<String> excludedLanguages = new HashSet<String>();
    private boolean allowAll;
    
    @SuppressWarnings("unchecked")
    public LanguageConfiguration(String property, String[] defaultConfig){
        if(property == null || property.isEmpty()){
            throw new IllegalArgumentException("The parsed property MUST NOT be NULL nor empty!");
        }
        this.property = property;
        this.defaultConfig = defaultConfig != null ? Arrays.asList(defaultConfig) : 
            Collections.EMPTY_LIST;
        try {
            parseConfiguration(this.defaultConfig);
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException("Inalied default configuration "
                    + e.getMessage());
        }
    }
    
    public String getProperty() {
        return property;
    }
    
    /**
     * Reads the config for the configured {@link #getProperty() property}
     * from the parsed configuration. This supports <code>String[]</code>,
     * <code>Collection</code>, and comma separated Strings
     * @param configuration
     */
    public void setConfiguration(Dictionary<?,?> configuration) throws ConfigurationException {
        Object value = configuration.get(property);
        Collection<?> config;
        if(value == null){
            config = defaultConfig;
        } else if (value instanceof String[]){
            config = Arrays.asList((String[]) value);
        } else if (value instanceof Collection<?>){
            config = (Collection<?>)value;
        } else if (value instanceof String){
            config = Arrays.asList(value.toString().split(","));
        } else {
            throw new ConfigurationException(property, "Values of type '"
                + value.getClass() +"' are not supported (supported are "
                + "String[], Collection<?>, comma separated String and "
                + "NULL to reset to the default configuration)!");
        }
        parseConfiguration(config);
    }
    
    private void parseConfiguration(Collection<?> config) throws ConfigurationException {
        if(config == null){
            config = defaultConfig;
        }
        for(Object value : config) {
            if(value == null){
                continue; //ignore null values
            }
            String lang = value.toString().trim().toLowerCase();
            if(lang.charAt(0) == '!'){ //exclude
                lang = lang.substring(1);
                if(configuredLanguages.contains(lang)){
                    throw new ConfigurationException(property, 
                        "Langauge '"+lang+"' is both included and excluded (config: "
                        + config+")");
                }
                excludedLanguages.add(lang);
            } else if("*".equals(lang)){
                allowAll = true;
            } else if(!lang.isEmpty()){
                if(excludedLanguages.contains(lang)){
                    throw new ConfigurationException(property, 
                        "Langauge '"+lang+"' is both included and excluded (config: "
                        + config+")");
                }
                configuredLanguages.add(lang);
            }
        }
    }
    /**
     * Checks if the parsed language is included in the configuration
     * @param language the language
     * @return the state
     */
    public boolean isLanguage(String language){
        return allowAll ? 
            (!excludedLanguages.contains(language)) : 
                configuredLanguages.contains(language);
    }
    /**
     * Resets the configuration to the default (as parsed in the constructor)
     */
    public void setDefault() {
        try {
            parseConfiguration(defaultConfig);
        }catch (ConfigurationException e) {
            // can not happen else the default config is already validated
            // within the constructor
        }
    }
    
    
}
