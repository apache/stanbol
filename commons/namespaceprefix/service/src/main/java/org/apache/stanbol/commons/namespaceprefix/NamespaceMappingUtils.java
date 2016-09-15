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
package org.apache.stanbol.commons.namespaceprefix;


import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.osgi.service.cm.ConfigurationException;

public final class NamespaceMappingUtils {

    /**
     * Restrict instantiation
     */
    private NamespaceMappingUtils() {}

   /**
     * This pattern checks for invalid chars within an prefix.
     * The used pattern is <code>[^a-zA-Z0-9\-_]</code>. Meaning that prefixes
     * are allows to include alpha numeric characters including '-' and '_'
     */
    private static final Pattern PREFIX_VALIDATION_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-_]");
    /**
     * Getter for the prefix for the parsed {prefix}:{localName} value.
     * If the parsed value does not follow this pattern (but is an URI) than
     * this method will return <code>null</code>
     * @param shortNameOrUri the shortName or an URI
     * @return the prefix or <code>null</code> if an URI was parsed
     */
    public static String getPrefix(String shortNameOrUri){
        //ignore null and empty strings
        if(shortNameOrUri == null || shortNameOrUri.isEmpty()) {
            return null; //not a short uri
        }
        int index = shortNameOrUri.indexOf(':');
        if(index < 0 && shortNameOrUri.charAt(0) != '/'){
            return ""; //default namespace
        } else if (index > 0){
            if(shortNameOrUri.length() == (index+1) || //{prefix}: was parsed
                    shortNameOrUri.charAt(index+1) == '/' || 
                    (index == 3 && shortNameOrUri.startsWith("urn"))){ 
                return null; // URI was parsed ({protocol}:/...)
            } else {
                return shortNameOrUri.substring(0, index);
            }
        } else {
            return null; //not a short name
        }
    }
    /**
     * Extracts the namespace form the parsed URI or returns <code>null</code>
     * of the URI does not contain an namesoace (e.g. http://www.test.org, 
     * urn:someValue)
     * @param uri the uri
     * @return the namespace including the separator ('#' or '/' or ':')
     */
    public static String getNamespace(String uri){
        if(uri == null){
            return uri;
        }
        final int index;
        if(uri.startsWith("urn:")){
            index = uri.lastIndexOf(':');
            if(index < 5){ //urn:?: is the shortest possible namesoace
                return null;
            }
        } else{
            int protocolIndex = uri.indexOf(":/");
            if(protocolIndex < 1){
                return null; //not an absolute URI
            }
            index = Math.max(uri.lastIndexOf('#'),uri.lastIndexOf('/'));
            if(protocolIndex + 3 > index) { //in '{port}://' the 2nd '/' is no namespace
                return null;
            }
        }
        //do not convert if the parsed uri does not contain a local name
        if(index > 0) {// and the namespace is not the protocol
            return uri.substring(0, index+1);
        } else {
            return null;
        }
    }
    /**
     * Uses the NamespacePrefixService#PREFIX_VALIDATION_PATTERN to check
     * if the parsed prefix is valid
     * @param prefix the prefix to check
     * @return <code>true</code> if valid. Othervise <code>false</code>
     */
    public static boolean checkPrefix(String prefix){
        if(prefix == null){
            return false;
        }
        return !PREFIX_VALIDATION_PATTERN.matcher(prefix).find();
    }
    /**
     * Checks of the parsed namespace is valid. Namespaces starting with
     * '<code>urn:</code>' need to end with ':'. Otherwise namespaces need to
     * end with '/' or '#' and contain a protocol (checked by searching ':/').
     * No further validation on the parsed namespace are done 
     * @param namespace the namespace
     * @return <code>true</code> if valid. Othervise <code>false</code>
     */
    public static boolean checkNamespace(String namespace){
        if(namespace == null || namespace.isEmpty()){
            return false;
        }
        return namespace.startsWith("urn:") ?
                namespace.charAt(namespace.length()-1) == ':' :
                    (namespace.charAt(namespace.length()-1) == '#' ||
                    namespace.charAt(namespace.length()-1) == '/') && 
                    namespace.indexOf(":/") > 0;
    }
    
    
    /**
     * Utility intended to be used during activate of OSGI components that support
     * the use of '{prefix}:{localname}' in its configurations. The
     * {@link NamespacePrefixService} is assumed as optional so that users can
     * use <code>ReferenceCardinality.OPTIONAL_UNARY</code> to inject the service.
     * <p>
     * Here is an example
     * <code>
     *     Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
     *     protected NamespacePrefixService nps;
     * </code>
     * @param nps the {@link NamespacePrefixService} or <code>null</code> if not
     * available
     * @param property the configuration property (used for creating {@link ConfigurationException}s)
     * @param value configured value. Might be both a '{prefix}:{localname}' or the full URI.
     * @return the full URI
     * @throws ConfigurationException if the conversion was not possible because
     * the prefix is <code>null</code> or the prefix is 
     * unknown to the service
     */
    public static String getConfiguredUri(NamespacePrefixService nps, String property, String value) throws ConfigurationException{
        if(nps != null){
            String fieldUri = nps.getFullName(value);
            if(fieldUri == null){
                throw new ConfigurationException(property, "The prefix '"
                        + NamespaceMappingUtils.getPrefix(value)+"' is unknown (not mapped to an "
                        + "namespace) by the Stanbol Namespace Prefix Mapping Service. Please "
                        + "change the configuration to use the full URI instead of '"+value+"'!");
            }
            return fieldUri;
        } else if(NamespaceMappingUtils.getPrefix(value) != null){
            throw new ConfigurationException(property, "'{prefix}:{localname}' configurations "
                + "such as '"+value+"' are only supported if the NamespacePrefixService is "
                + "available for the Stanbol instance (what is currently not the case). Please "
                + "change the configuration to use the full URI");
        } else { //no service but a full uri
            return value;
        }
    }
    
    /**
     * Utility intended to be used to by components that do allow the use of
     * '{prefix}:{localname}' in its configurations. The {@link NamespacePrefixService}
     * is considered optional.  
     * @param nps the {@link NamespacePrefixService} or <code>null</code> if not
     * available
     * @param value configured value. Might be both a '{prefix}:{localname}' or the full URI.
     * @return the full URI
     * @throws IllegalArgumentException if the conversion was not possible because
     * the prefix is <code>null</code> or the prefix is 
     * unknown to the service
     */
    public static String getConfiguredUri(NamespacePrefixService nps, String value) throws IllegalArgumentException {
        if(nps != null){
            String fieldUri = nps.getFullName(value);
            if(fieldUri == null){
                throw new IllegalArgumentException("The prefix '"
                        + NamespaceMappingUtils.getPrefix(value)+"' is unknown (not mapped to an "
                        + "namespace) by the Stanbol Namespace Prefix Mapping Service. Please "
                        + "change the configuration to use the full URI instead of '"+value+"'!");
            }
            return fieldUri;
        } else if(NamespaceMappingUtils.getPrefix(value) != null){
            throw new IllegalArgumentException("'{prefix}:{localname}' configurations "
                + "such as '"+value+"' are only supported if the NamespacePrefixService is "
                + "available for the Stanbol instance (what is currently not the case). Please "
                + "change the configuration to use the full URI");
        } else { //no service but a full uri
            return value;
        }
    }
    
}
