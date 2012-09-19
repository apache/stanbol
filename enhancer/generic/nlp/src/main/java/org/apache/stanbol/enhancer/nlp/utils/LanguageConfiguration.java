package org.apache.stanbol.enhancer.nlp.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    private static final Map<String,String> EMPTY_PARAMS = Collections.emptyMap();
    
    private final String property;
    private final Collection<String> defaultConfig;
    //Langauge configuration
    private Map<String,Map<String,String>> configuredLanguages = new HashMap<String,Map<String,String>>();
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
            String line = value.toString().trim();
            int sepIndex = line.indexOf(';');
            String lang = sepIndex < 0 ? line : line.substring(0, sepIndex).trim();
            lang = lang.toLowerCase();
            if(lang.charAt(0) == '!'){ //exclude
                lang = lang.substring(1);
                if(configuredLanguages.containsKey(lang)){
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
                if(sepIndex >= 0){
                    
                }
                configuredLanguages.put(lang,sepIndex >= 0 && sepIndex < line.length()-2 ? 
                        parseParameters(line.substring(sepIndex, line.length()).trim()) :
                            EMPTY_PARAMS);
            }
        }
    }
    /**
     * Parses optional parameters <code>{key}[={value}];{key2}[={value2}]</code>. Using
     * the same key multiple times will override the previouse value
     * @param paramString
     * @return
     * @throws ConfigurationException
     */
    private Map<String,String> parseParameters(String paramString) throws ConfigurationException {
        Map<String,String> params = new HashMap<String,String>();
        for(String param : paramString.split(";")){
            param = param.trim();
            int equalsPos = param.indexOf('=');
            if(equalsPos == 0){
                throw new ConfigurationException(property, 
                    "Parameter '"+param+"' has empty key!");
            }
            String key = equalsPos > 0 ? param.substring(0, equalsPos).trim() : param;
            String value;
            if(equalsPos > 0){
                if(equalsPos < param.length()-2) {
                    value = param.substring(equalsPos+1).trim(); 
                } else {
                    value = "";
                }
            } else {
                value = null;
            }
            params.put(key, value);
        }
        return params.isEmpty() ? EMPTY_PARAMS : Collections.unmodifiableMap(params);
    }

    /**
     * Checks if the parsed language is included in the configuration
     * @param language the language
     * @return the state
     */
    public boolean isLanguage(String language){
        return allowAll ? 
            (!excludedLanguages.contains(language)) : 
                configuredLanguages.containsKey(language);
    }
    /**
     * Returns parsed parameters if <code>{@link #isLanguage(String)} == true</code>
     * @param language the language
     * @return the parameters or <code>null</code> if none or the parsed language
     * is not active.
     */
    public Map<String,String> getParameters(String language){
        return isLanguage(language) ? configuredLanguages.get(language) : null;
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
    /**
     * Returns the value of the parameter for the language (if present and the
     * langage is active)
     * @param language the language
     * @param paramName the name of the param
     * @return the param or <code>null</code> if not present OR the language
     * is not active.
     */
    public String getParameter(String language, String paramName) {
        Map<String,String> params = getParameters(language);
        return params == null ? null : params.get(paramName);
    }
    
    
}
