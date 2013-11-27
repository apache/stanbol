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

import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.DEREFERENCE_ENTITIES_FIELDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;

public class DereferenceUtils {

    
    /**
     * Parsed the {@link DereferenceConstants#DEREFERENCE_ENTITIES_FIELDS}
     * config from the parsed Dictionary regardless if it is defined as 
     * <code>String[]</code>, <code>Collection&lt;String&gt;</code> or
     * <code>String</code> (single value).<p>
     * This returns the fields as parsed by the configuration.<p>
     * <b>NOTE:</b> This does not check/convert <code>{prefix}:{localname}</code>
     * configurations to URIs. The receiver of the list is responsible for
     * that 
     * @param conf the configuration as parsed to the OSGI component
     * @return the {@link List} with the unprocessed dereference fields 
     * @throws ConfigurationException if the value is not any of the supported
     * types
     */
    public static List<String> parseDereferencedFieldsConfig(Dictionary<String,Object> conf) throws ConfigurationException {
        Object value = conf.get(DEREFERENCE_ENTITIES_FIELDS);
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
        } else if(value != null){
            throw new ConfigurationException(DEREFERENCE_ENTITIES_FIELDS, 
                "Dereference Entities Fields MUST BE parsed as String[], Collection<String> or "
                + "String (single value). The actual value '"+value+"'(type: '"+value.getClass() 
                + "') is NOT supported");
        } else { //value == null 
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
     * @param conf the configuration as parsed to the OSGI component
     * @return the unparsed LDPath program as String 
     * @throws ConfigurationException if the value is not any of the supported
     * types
     */
    public static String parseLdPathConfig(Dictionary<String,Object> conf) throws ConfigurationException {
        Object value = conf.get(DereferenceConstants.DEREFERENCE_ENTITIES_LDPATH);
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
        } else {
            throw new ConfigurationException(DEREFERENCE_ENTITIES_FIELDS, 
                "Dereference LDPath statements MUST BE parsed as String, String[] or "
                + "Collection<String>. The actual value '"+value+"'(type: '"+value.getClass() 
                + "') is NOT supported");            
        }
        //we we have not found non blank lines return null!
        return !first ? sb.toString() : null;
        
        
    }
    
}
