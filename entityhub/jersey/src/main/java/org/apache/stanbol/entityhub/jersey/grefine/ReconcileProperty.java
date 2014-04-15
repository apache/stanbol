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
package org.apache.stanbol.entityhub.jersey.grefine;

import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special properties are used by this Google Refine Reconciliation API
 * implementation to enable Users to use special features of the Entityhub
 * 
 * The Syntax used by those properties is
 * <code><pre>
 *     @{propName}[:{propParameter}]
 * </pre><code>
 * 
 * where:<ul>
 * <li> '@' is the special property indicator
 * <li> '{propertyName} is parsed as the name of the special property
 * <li> ':' separates the property name with an optional property value
 * <li> {propertyParameter} an additional parameter for this special property.
 * The syntax of the parameter is special property specific.
 * </ul>
 * Both the {propertyName} and the {propertyValue} are trimmed.<p>
 * @author Rupert Westenthaler
 *
 */
public final class ReconcileProperty {
    
    private static final Logger log = LoggerFactory.getLogger(ReconcileProperty.class);
    
    public static final char SPECIAL_PROPERTY_PREFIX = '@';
    public static final char SPECAIL_PROPERTY_VALUE_SEPARATOR = ':';
    private final boolean special;
    private final String name;
    private final String parameter;
    
    private ReconcileProperty(boolean special, String name, String parameter){
        this.special = special;
        this.name = name;
        this.parameter = parameter;
    }
    
    /**
     * Tests if the parsed name represents a specail property
     * @param name the name
     * @return <code>true</code> if the parsed propertyString is not <code>null</code>,
     * not empty and starts with {@link #SPECIAL_PROPERTY_PREFIX}. Otherwise
     * <code>false</code>
     */
    public static boolean isSpecialProperty(String propertyString){
        propertyString = StringUtils.trimToNull(propertyString);
        return propertyString != null && propertyString.length() > 1 && 
                propertyString.charAt(0) == SPECIAL_PROPERTY_PREFIX;
    }
    /**
     * Parses the Reconcile property from the parsed propertyString
     * @param propertyString the property string
     * @return the {@link ReconcileProperty} or <code>null</code> if the parsed
     * String is illegal formatted.
     */
    public static ReconcileProperty parseProperty(String propertyString, NamespacePrefixService nxPrefixService){
        propertyString = StringUtils.trimToNull(propertyString);
        if(propertyString != null){
            propertyString = StringUtils.trimToNull(propertyString);
            if(propertyString == null){
                log.warn("Unable to parse Reconcile Property: The parsed propertyString MUST contain some none trimable chars!");
                return null;
            }
            String propertyUri = nxPrefixService.getFullName(propertyString);
            if(propertyUri == null){
                log.warn("The property '{}' uses the unknown prefix '{}' -> ignored",
                    propertyString,NamespaceMappingUtils.getPrefix(propertyString));
                return null;
            }
            boolean special = propertyUri.charAt(0) == SPECIAL_PROPERTY_PREFIX;
            if(!special){
                return new ReconcileProperty(special, propertyUri, null);
            } // else parse special property name and parameter
            if(propertyUri.length() < 1){
                log.warn("Unable to parse Reconcile Property: The parsed propertyString MUST NOT " +
                        "contain only the special property prefix '{}'!",
                        SPECIAL_PROPERTY_PREFIX);
                return null;
            }
            int valueSeparatorIndex = propertyUri.indexOf(SPECAIL_PROPERTY_VALUE_SEPARATOR);
            String name = StringUtils.trimToNull(
                propertyUri.substring(1, valueSeparatorIndex > 0 ? 
                        valueSeparatorIndex : propertyUri.length()));
            if(name == null) {
                log.warn("Unable to parse Reconcile Property: The parsed special " +
                		"property '{}' has an empty property name!",propertyUri);
                return null;
            }
            return new ReconcileProperty(special, name, 
                //parse the parameter from the parsed value
                valueSeparatorIndex > 0 && valueSeparatorIndex < propertyUri.length() ?
                        StringUtils.trimToNull(propertyUri.substring(valueSeparatorIndex+1)):null);
        } else {
            log.warn("Unable to parse Reconcile Property from NULL or an empty String!");
            return null;
        }
    }

    /**
     * Getter for the name of the property
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the parameter 
     * @return the value or <code>null</code> if none
     */
    public String getParameter() {
        return parameter;
    }
    
    /**
     * Checks if this special property has a value or not.
     * @return if the special property has a value or not.
     */
    public boolean hasParameter(){
        return parameter != null;
    }
    /**
     * If this reconcile property is a special property (starting with an '@')
     * @return
     */
    public boolean isSpecial(){
        return special;
    }
    
    @Override
    public int hashCode() {
        return name.hashCode()+(parameter != null?parameter.hashCode():0)+
                (special?1:0);
    }
    @Override
    public boolean equals(Object o) {
        if(o instanceof ReconcileProperty && name.equals(((ReconcileProperty)o).name)
                && special == ((ReconcileProperty)o).special){
            return parameter == null && ((ReconcileProperty)o).parameter == null || (
                    parameter != null && parameter.equals(((ReconcileProperty)o).parameter));
        } else {
            return false;
        }
    }
    /**
     * Serialised the {@link ReconcileProperty} as defined by the syntax
     * <code><pre>
     *     @{propName}[:{propValue}]
     * </pre><code>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(special){
            sb.append(SPECIAL_PROPERTY_PREFIX);
        }
        sb.append(name);
        if(parameter != null){
            sb.append(SPECAIL_PROPERTY_VALUE_SEPARATOR).append(parameter);
        }
        return sb.toString();
    }
    
}
