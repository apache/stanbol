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
package org.apache.stanbol.commons.namespaceprefix.impl;

import static org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils.checkNamespace;
import static org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils.checkPrefix;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link NamespacePrefixProvider}.
 * Mappings can not be modified.
 */
public class NamespacePrefixProviderImpl implements NamespacePrefixProvider {

    Logger log = LoggerFactory.getLogger(NamespacePrefixProviderImpl.class);
    
    private SortedMap<String,String> prefixMap = new TreeMap<String,String>();
    private SortedMap<String,List<String>> namespaceMap = new TreeMap<String,List<String>>();
    
    /**
     * Reads "{prefix}\t{namespace}\n" mappings form the parsed InputStream
     * @param is the stream to read the data from
     * @throws IOException on any error while reading from the parsed stream
     */
    public NamespacePrefixProviderImpl(InputStream is) throws IOException {
        readPrefixMappings(is,true);
    }
    /**
     * Read the mappings form the parsed map
     * @param mappings Mappings
     */
    public NamespacePrefixProviderImpl(Map<String,String> mappings){
        for(Entry<String,String> mapping : mappings.entrySet()){
            addMapping(mapping.getKey(),mapping.getValue(),true);
        }
    }
    /**
     * Expected to be called only during activation
     * @param in
     * @param validate if mappings should be validated before adding
     * @throws IOException
     */
    private void readPrefixMappings(InputStream in,boolean validate) throws IOException {
        LineIterator it = IOUtils.lineIterator(in, "UTF-8");
        while(it.hasNext()){
            String mapping = it.nextLine();
            if(mapping.charAt(0) != '#'){
                int sep = mapping.indexOf('\t');
                if(sep < 0 || mapping.length() <= sep+1){
                    log.warn("Illegal prefix mapping '{}'",mapping);
                } else {
                    String old = addMapping(mapping.substring(0, sep),mapping.substring(sep+1),validate);
                    if(old != null){
                        log.info("Duplicate mention of prefix {}. Override mapping from {} to {}",
                            new Object[]{mapping.substring(0, sep), old, mapping.substring(sep+1)});
                    }
                }
            } else { //comment
                log.debug(mapping);
            }
        }
    }    
    /**
     * Internally used to add an mapping
     * @param prefix the prefix
     * @param namespace the namespace
     * @param validate if true the prefix and namespace values are validated
     * using {@link NamespaceMappingUtils#checkPrefix(String)} and 
     * {@link NamespaceMappingUtils#checkNamespace(String)}.
     * @return the previous mapping or <code>null</code> if none.
     */
    protected String addMapping(String prefix, String namespace, boolean validate){
        if(validate){
            boolean p = checkPrefix(prefix);
            boolean n = checkNamespace(namespace);
            if(!p || !n){
                log.warn("Invalid Namespace Mapping: prefix '{}' {} , namespace '{}' {} -> mapping ignored!",
                    new Object[]{prefix,p?"valid":"invalid",namespace,n?"valid":"invalid"});
                return null;
            }
        }
        String old = prefixMap.put(prefix, namespace);
        if(!namespace.equals(old)){ //if the mapping changed
            //(2) update the inverse mappings (ensure read only lists!)
            List<String> prefixes = namespaceMap.get(namespace);
            if(prefixes == null){
                namespaceMap.put(namespace, Collections.singletonList(prefix));
            } else {
                String[] ps = new String[prefixes.size()+1];
                int i=0;
                for(;i<prefixes.size();i++){
                    ps[i] = prefixes.get(i);
                }
                ps[i] = prefix;
                namespaceMap.put(namespace, Arrays.asList(ps));
            }
        }
        return old;
    }    
    @Override
    public String getNamespace(String prefix) {
        return prefixMap.get(prefix);
    }

    @Override
    public String getPrefix(String namespace) {
        List<String> prefixes = namespaceMap.get(namespace);
        return prefixes == null || prefixes.isEmpty() ? null : prefixes.get(0);
    }

    @Override
    public List<String> getPrefixes(String namespace) {
        List<String> prefixes = namespaceMap.get(namespace);
        return prefixes == null ? Collections.EMPTY_LIST : prefixes;
    }

}
