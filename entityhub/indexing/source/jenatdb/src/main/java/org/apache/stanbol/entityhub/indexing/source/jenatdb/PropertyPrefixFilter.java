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
package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;

public class PropertyPrefixFilter implements RdfImportFilter {
    
    private final Logger log = LoggerFactory.getLogger(PropertyPrefixFilter.class);
    
    /**
     * Links to a file that defines included & excluded properties (one per line)<p>
     * <b>Syntax</b>
     * <ul>
     * <li>Lines starting with '#' are ignored
     * <li>'!{prefix}' will exclude all properties starting with the {prefix}.
     * <li>'{prefix}' will include all properties starting with {prefix}
     * <li>'*' will include all properties not explicitly excluded
     * <li> Namespace prefixes are supported!
     * <li> '{prefix}*' is also supported. However all {prefix} values are
     * interpreted like that.
     * </ul>
     * <b>NOTES</b>: (1) Longer prefixes are matched first. (1) All processed 
     * values are stored in-memory. That means that matching prefixes are only 
     * calculate on the first appearance of an property. 
     */
    public static final String PARAM_PROPERTY_FILTERS = "if-property-filter";
    
    
    public PropertyPrefixFilter(){}
    /**
     * For unit tests only
     * @param nsPrefixService
     * @param lines
     */
    protected PropertyPrefixFilter(NamespacePrefixProvider nsPrefixService, 
            List<String> lines){
        parsePropertyPrefixConfig(nsPrefixService, lines);
    }
    
    private Map<String, Boolean> propertyPrefixMap;
    private Map<String, Boolean> propertyMap;
    private boolean includeAll;
    
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        NamespacePrefixService nsPrefixService = indexingConfig.getNamespacePrefixService();
        log.info("Configure {}",getClass().getSimpleName());
        Object value = config.get(PARAM_PROPERTY_FILTERS);
        if(value == null){
            propertyPrefixMap = Collections.emptyMap();
            propertyMap = Collections.emptyMap();
            includeAll = true;
        } else {
            log.info(" > property Prefix Filters");
            //ensure that longer prefixes are first
            File propertyPrefixConfig = indexingConfig.getConfigFile(value.toString());
            List<String> lines;
            InputStream in = null;
            try {
                in = new FileInputStream(propertyPrefixConfig);
                lines = IOUtils.readLines(in,"UTF-8");
            }catch (IOException e) {
                throw new IllegalArgumentException("Unable to read property filter configuration "
                    + "from the configured File "+propertyPrefixConfig.getAbsolutePath(),e);
            } finally {
                IOUtils.closeQuietly(in);
            }
            parsePropertyPrefixConfig(nsPrefixService, lines);
        }
        
    }

    /**
     * @param nsPrefixService
     * @param propertyPrefixConfig
     */
    private void parsePropertyPrefixConfig(NamespacePrefixProvider nsPrefixService, List<String> lines) {
        propertyPrefixMap = new TreeMap<String,Boolean>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int length = o2.length() - o1.length();
                if(length != 0){
                    return length;
                } else {
                    return o1.compareTo(o2);
                }
            }
        });
        propertyMap = new HashMap<String,Boolean>();
        includeAll = lines.remove("*");
        log.info("    - includeAll: {}",includeAll);
        for(String line : lines){
            if(line.startsWith("#") || line.isEmpty() || line.equals("*")){
                continue; //ignore comment, empty lines and multiple '*'
            }
            boolean exclude = line.charAt(0) == '!';
            String prefix = exclude ? line.substring(1) : line;
            prefix = prefix.trim();
            if(includeAll && !exclude){
                continue; //ignore includes if * is active
            }
            String uri; 
            String nsPrefix = NamespaceMappingUtils.getPrefix(prefix);
            if(nsPrefix != null){
                String ns = nsPrefixService.getNamespace(nsPrefix);
                if(ns == null){
                    throw new IllegalArgumentException("Unable to resolve namesoace prefix used by '"
                            +prefix+"' by using the NamespacePrefixService!");
                }
                uri = new StringBuilder(ns).append(prefix,nsPrefix.length()+1, prefix.length()).toString();
            } else {
                uri = prefix;
            }
            if(uri.charAt(uri.length()-1) == '*'){
                uri = uri.substring(0, uri.length()-1);
            }
            log.info("    - '{}' {}", uri, exclude ? "excluded" : "included");
            propertyPrefixMap.put(uri, !exclude);
        }
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean accept(Node s, Node p, Node o) {
        if(p.isURI()){
            if(includeAll && propertyPrefixMap.isEmpty()){
                return true;
            }
            String property = p.getURI();
            Boolean state = propertyMap.get(property);
            if(state != null){
                return state;
            }
            //first time we encounter this property ... need to calculate
            for(Entry<String,Boolean> entry : propertyPrefixMap.entrySet()){
               if(property.startsWith(entry.getKey())){
                   propertyMap.put(property, entry.getValue());
                   return entry.getValue();
               }
            }
            //no match ... set based on includeAll
            propertyMap.put(property, includeAll);
        } else {
            return false;
        }
        return false;
    }

}
