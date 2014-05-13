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
package org.apache.stanbol.enhancer.servicesapi.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility Class with methods allowing to parse line based configurations formatted
 * like:
 * <code><pre>
 *   &lt;key1&gt;;&lt;parm1&gt;=&lt;value1&gt;,&lt;value2&gt;;&lt;parm2&gt;=&lt;value1&gt;...
 *   &lt;key2&gt;;&lt;state&gt
 *   &lt;key3&gt;;&lt;state&gt=true
 *   &lt;key4&gt;;&lt;parm1&gt;=&lt;value1&gt;;&lt;parm1&gt;=&lt;value2&gt;
 * </pre></code>
 * Rules:<ul>
 * <li> keys MUST NOT be contained multiple times
 * <li> lines with empty keys are ignored (this includes empty lines or 
 * lines that are <code>null</code>)
 * <li> If parameter appear multiple time values are appended
 * <li> empty parameter names are ignored
 * <li> parameters MIST NOT start with '='
 * </ul>
 * <p>
 * This is intended to be used to parse richer configurations form OSGI
 * configuration files.
 * 
 * @author Rupert Westenthaler
 *
 */
public final class ConfigUtils {
    
    private ConfigUtils(){/* Do not create instances of utility classes */}
    
    private static Map<String,String> rdfExt2Formats;
    static {
        Map<String,String> m = new HashMap<String,String>();
        m.put(null, SupportedFormat.RDF_XML); //default to rdf+xml
        m.put("", SupportedFormat.RDF_XML); //no extension is also mapped to rdf+xml
        m.put("rdf", SupportedFormat.RDF_XML);
        m.put("xml", SupportedFormat.RDF_XML);
        m.put("owl", SupportedFormat.RDF_XML);
        m.put("rdfs", SupportedFormat.RDF_XML);
        m.put("json", SupportedFormat.RDF_JSON);
        m.put("nt", SupportedFormat.N_TRIPLE);
        m.put("n3", SupportedFormat.N3);
        m.put("ttl", SupportedFormat.TURTLE);
        rdfExt2Formats = Collections.unmodifiableMap(m);
    }
    /**
     * Guesses the RDF format based on the provided file extension. Parsing
     * <code>null</code> will return the default format.
     * @param extension the extension or <code>null</code> to ask for the default
     * @return the mime type or <code>null</code> if the parsed extension is not
     * known.
     */
    public static String guessRdfFormat(String extension){
        return rdfExt2Formats.get(extension);
    }
    
    private static final Logger log = LoggerFactory.getLogger(ConfigUtils.class);
    
    /**
     * Parses configurations formatted like
     * <code><pre>
     *   &lt;key1&gt;;&lt;parm1&gt;=&lt;value1&gt;,&lt;value2&gt;;&lt;parm2&gt;=&lt;value1&gt;...
     *   &lt;key2&gt;;&lt;state&gt
     *   &lt;key3&gt;;&lt;state&gt=true
     *   &lt;key4&gt;;&lt;parm1&gt;=&lt;value1&gt;;&lt;parm1&gt;=&lt;value2&gt;
     * </pre></code>
     * Rules:<ul>
     * <li> keys MUST NOT be contained multiple times
     * <li> lines with empty keys are ignored (this includes empty lines or 
     * lines that are <code>null</code>)
     * <li> If parameter appear multiple time values are appended
     * <li> empty parameter names are ignored
     * <li> parameters MIST NOT start with '='
     * </ul>
     * @param configuration The configuration
     * @throws IllegalArgumentException on any syntax error in the parsed
     * configuration
     */
    public static Map<String,Map<String,List<String>>> parseConfig(Iterable<String> configuration) {
        Map<String,Map<String,List<String>>> config = new HashMap<String,Map<String,List<String>>>();
        for(String line : configuration){
            parseLine(config, line);
        }
        return config;
    }
    /**
     * Parses configurations formatted like
     * <code><pre>
     *   &lt;key1&gt;;&lt;parm1&gt;=&lt;value1&gt;,&lt;value2&gt;;&lt;parm2&gt;=&lt;value1&gt;...
     *   &lt;key2&gt;;&lt;state&gt
     *   &lt;key3&gt;;&lt;state&gt=true
     *   &lt;key4&gt;;&lt;parm1&gt;=&lt;value1&gt;;&lt;parm1&gt;=&lt;value2&gt;
     * </pre></code>
     * Rules:<ul>
     * <li> keys MUST NOT be contained multiple times
     * <li> lines with empty keys are ignored (this includes empty lines or 
     * lines that are <code>null</code>)
     * <li> If parameter appear multiple time values are appended
     * <li> empty parameter names are ignored
     * <li> parameters MIST NOT start with '='
     * </ul>
     * @param confIterator The Iterator over the lines of the configuration.
     * @throws IllegalArgumentException on any syntax error in the parsed
     * configuration
     */
    public static Map<String,Map<String,List<String>>> parseConfig(Iterator<String> confIterator) {
        Map<String,Map<String,List<String>>> config = new HashMap<String,Map<String,List<String>>>();
        while(confIterator.hasNext()){
            parseLine(config, confIterator.next());
        }
        return config;
    }
    /**
     * Returns the "key, parameter" entry parsed form the parsed configuration 
     * line.<p>
     * This method is useful if the caller need to preserve the oder of multi
     * line configurations and therefore can not use the parseConfig methods.
     * @param line a configuration line
     * @return the key, parameter entry
     */
    public static Entry<String,Map<String,List<String>>> parseConfigEntry(String line){
        String[] elements = line.split(";");
        return Collections.singletonMap(elements[0].trim(), getParameters(elements,1))
            .entrySet().iterator().next();
    }
    
    /**
     * Utility that extracts enhancement properties from the parsed parameter maps.
     * This will only consider keys starting with '<code>enhancer.</code>' as
     * defined by <a href="https://issues.apache.org/jira/browse/STANBOL-488">STANBOL-488</a>
     * @param parameters the paraemters (e.g. as returned as values by
     * {@link #parseConfig(Iterator)})
     * @return The enhancement properties extracted from the parsed parameters
     * @since 0.12.1
     */
    public static Map<String,Object> getEnhancementProperties(Map<String, List<String>> parameters){
        Map<String,Object> props = new HashMap<String,Object>();
        for(Entry<String,List<String>> entry : parameters.entrySet()){
            if(entry.getKey().startsWith("enhancer.")){
                Object value;
                if(entry.getValue().size() == 1){
                    value = entry.getValue().get(0);
                } else {
                    value = entry.getValue();
                }
                if(value != null){
                    props.put(entry.getKey(), value);
                }
            }
        }
        return props;
    }
    /**
     * Utility that extracts enhancement properties from a configuration line
     * using the syntax
     * <pre>
     *     {prop}={value-1},{value-2},..,{value-n}
     * </pre>
     * @param line the configuration of a single enhancement property
     * @return The enhancement properties extracted from the parsed parameters
     * @since 0.12.1
     */
    public static Map<String,Object> getEnhancementProperties(Collection<String> lines){
        if(lines == null || lines.isEmpty()){
            return null;
        } else {
            return getEnhancementProperties(getParameters(
                lines.toArray(new String[lines.size()]), 0));
        }
    }
    
    /**
     * Internally used to parse single lines of an parsed {@link Iterable} or
     * {@link Iterator}
     * @param config the map used to write the parsed values
     * @param line the line to parse
     */
    private static void parseLine(Map<String,Map<String,List<String>>> config, String line) {
        if(line == null || line.isEmpty() || line.charAt(0) == ';'){
            log.warn("Configuration entry ignored because of empty Engine name " +
                    "(entry: '"+line+"')!");
        }
        String[] elements = line.split(";");
        String key = elements[0].trim();
        if(!config.containsKey(key)){
            config.put(key, getParameters(elements,1));
        } else {
            throw new IllegalArgumentException("The configuration MUST NOT contain the same key '"+
                    elements[0]+"' multiple times!");
        }
    }

    /**
     * Parses the boolean value form the values for the parsed key.
     * The state is assumed as <code>true</code> if the key is present and the 
     * value is either an empty list of the first element of the list evaluates
     * to <code>{@link Boolean#parseBoolean(String)} == true</code>.
     * @param parameters the parameter
     * @param key the key
     * @return the boolean state of the requested key
     */
    public static boolean getState(Map<String,List<String>> parameters, String key){
        List<String> value = parameters.get(key);
        return value != null && (value.isEmpty() || Boolean.parseBoolean(
            value.get(0)));
    }
    /**
     * Getter for the first value of a given key
     * @param parameters the parameters
     * @param key the key
     * @return the first value or <code>null</code> if the key is not present
     * or the list is empty.
     */
    public static String getValue(Map<String,List<String>> parameters, String key) {
        List<String> values = parameters.get(key);
        return values == null || values.isEmpty() ? null : values.get(1);
    }
    /**
     * Utility that parses 'key=value,value2' parameters from the parsed array.
     * If key appear multiple time values are appended.
     * @param elements the elements
     * @param start the start position within the parsed array to start parsing
     * @return the parsed parameters. If no value is provided for a key the
     * value of entries will be NOT <code>null</code> but en empty list. The
     * returned Map provides read and write access.
     * @throws IllegalArgumentExeption if elements are illegal formatted (e.g.
     * if the start with an '='
     */
    public static Map<String,List<String>> getParameters(String[] elements, int start) {
        final Map<String,List<String>> parameters;
        if(elements.length > start){
            parameters = new HashMap<String,List<String>>(elements.length-start);
            for(int i = start;i<elements.length;i++){
                if(elements[i] != null && !elements[i].isEmpty()){
                    if(elements[i].charAt(0) == '='){
                        throw new IllegalArgumentException("Unable to " +
                                "parse parameters because element at index " +
                                i+" MUST NOT start with '=' (element: '"+
                                elements[i]+"')!");
                    }
                    int sepIndex = elements[i].indexOf('=');
                    String param = sepIndex > 0 ? elements[i].substring(0, sepIndex).trim() : 
                        elements[i].trim();
                    String value = sepIndex > 0 && elements[i].length()> sepIndex+1 ? 
                            elements[i].substring(sepIndex+1).trim() : null;
                    List<String> paramValues = parameters.get(param);
                    if(paramValues == null){
                        paramValues = new ArrayList<String>(3);
                        parameters.put(param, paramValues);
                    }
                    if(value != null){
                        for(String v : value.split(",")){
                            if(v != null){
                                paramValues.add(v);
                            }
                        }
                    }
                }
            }
        } else {
            parameters = Collections.emptyMap();
        }
        return parameters;
    }
}
