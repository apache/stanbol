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
package org.apache.stanbol.entityhub.core.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;

/**
 * This class contains some utilities for osgi
 * TODO: Check if they are not available in some std. library
 * @author Rupert Westenthaler
 *
 */
public final class OsgiUtils {

    //private static final Logger log = LoggerFactory.getLogger(OsgiUtils.class);

    private OsgiUtils() {/* do not create instances of utility classes*/}

    /**
     * Checks if a value is present
     * @param propertyName The key for the property
     * @return the value
     * @throws ConfigurationException if the property is not present
     */
    public static final Object checkProperty(Dictionary<?, ?> properties, String propertyName) throws ConfigurationException{
        return checkProperty(properties, propertyName, null);
    }
    /**
     * Checks if the value is present. If not it returns the parse defaultValue.
     * If the value and the default value is null, it throws an {@link ConfigurationException}
     * @param properties the properties to search
     * @param propertyName the name of the proeprty
     * @param defaultValue the default value or <code>null</code> if none
     * @return the value of the property (guaranteed NOT <code>null</code>)
     * @throws ConfigurationException In case the property is not present and no default value was parsed
     */
    public static final Object checkProperty(Dictionary<?, ?> properties, String propertyName,Object defaultValue) throws ConfigurationException{
        Object value = properties.get(propertyName);
         if(value == null){
             if(defaultValue != null){
                 return defaultValue;
             } else {
                 throw new ConfigurationException(propertyName,"No value found for this required property");
             }
         } else {
             return value;
         }
    }

    /**
     * Checks if the property is present and the value can be converted to an {@link URI}
     * @param propertyName The key for the property
     * @return the value
     * @throws ConfigurationException if the property is not present or the
     * configured value is no valid URI
     */
    public static final URI checkUriProperty(Dictionary<?, ?> properties,String propertyName) throws ConfigurationException {
        Object uri = checkProperty(properties,propertyName);
        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            throw new ConfigurationException(propertyName,"Property needs to be a valid URI", e);
        }
    }
    /**
     * Checks if the property is present and the value can be converted to an {@link URL}
     * @param propertyName The key for the property
     * @return the value
     * @throws ConfigurationException if the property is not present or the
     * configured value is no valid URL
     */
    public static final URL checkUrlProperty(Dictionary<?, ?> properties,String propertyName) throws ConfigurationException {
        Object uri = checkProperty(properties,propertyName);
        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            throw new ConfigurationException(propertyName,"Property value needs to be a valid URL", e);
        }
    }
    /**
     * Checks if the value of a property is a member of the parsed Enumeration
     * @param <T> the Enumeration
     * @param enumeration the class of the enumeration
     * @param properties the configuration
     * @param propertyName the name of the property to check
     * @return the member of the enumeration
     * @throws ConfigurationException if the property is missing or the value is
     * not a member of the parsed enumeration
     */
    public static final <T extends Enum<T>> T checkEnumProperty(Class<T> enumeration,Dictionary<?, ?> properties,String propertyName) throws ConfigurationException{
        Object value =checkProperty(properties, propertyName);
        try {
            return Enum.valueOf(enumeration,value.toString());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(propertyName,String.format("Property value %s is not a member of Enumeration %s!",value,enumeration.getName()), e);
        }
    }

    /**
     * Copy all properties excluding "{@value Constants#OBJECTCLASS}",
     * "component.*" and "service.*" to the returned Dictionary
     * @param source the source
     * @return the target
     */
    public static Dictionary<String, Object> copyConfig(Dictionary<String, Object> source) {
        Dictionary<String, Object> config = new Hashtable<String, Object>();
        for(Enumeration<?> keys = source.keys();keys.hasMoreElements();){
            String key = keys.nextElement().toString();
            if(!key.startsWith("component.") &&
                    !key.startsWith("service.") &&
                    !key.equals(Constants.OBJECTCLASS)){
                config.put(key, source.get(key));
            }
        }
        return config;
    }

}
