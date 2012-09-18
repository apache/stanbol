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
package org.apache.stanbol.enhancer.engines.zemanta.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration that contains all property definitions for the Zemanta
 * Web Service. This contains also properties such as required default
 * configurations as well as possible values. There are also utility
 * methods to check parsed parameters
 *
 * @author Rupert Westenthaler
 */
public enum ZemantaPropertyEnum {
    /**
     * the request type to be used e.g. 'zemanta.suggest'
     */
    method("zemanta.suggest", "zemanta.suggest"),
    /**
     * the format used to encode results. Zemanta supports "xml", "json",
     * "wnjson" and "rdfxml" but this implementation only allows rdfxml!
     */
    format("rdfxml", "rdfxml"),
    /**
     * enable/disable URIs of Linking Open Data entities
     */
    return_rdf_links("1", "0", "1"),
    /**
     * if set to 'demoz' suggested dmoz categories are returned. Set this
     * parameter to '0' to deactivate this feature
     */
    return_categories("dmoz", "dmoz", "0"),
    /**
     * enable/disable links to images
     */
    return_images("0", "0", "1"),
    /**
     * enable/disable personalised search (based on the api_key)
     */
    personal_scope("0", "0", "1"),
    /**
     * Number of in-text links. the default depending on the number of input
     * words, 1 per each 10 words, and it maxes out at 10
     */
    markup_limit(false),
    /**
     * The api_key needed to use the service (required)
     */
    api_key(true),
    /**
     * The text to analyse use UTF-8 encoding (required)
     */
    text(true),;

    private boolean required;
    private String defaultValue;
    private Set<String> valueList;
    private String toString;

    /**
     * A required property with no default configuration and no value list
     */
    ZemantaPropertyEnum() {
        this(true);
    }

    /**
     * Creates a new property without an default value or a value list
     *
     * @param optional if the property is required or optional
     */
    ZemantaPropertyEnum(boolean optional) {
        this(optional, null);
    }

    /**
     * A optional property with a default configuration and a list of allowed values
     *
     * @param defaultValue the value used if this parameter is not parsed.
     * <code>null</code> indicates no default configuration. This does not
     * mean, that Zemanta does not use an default value for requests that
     * do not contain this parameter
     * @param valueList the list of allowed values for this parameter.
     * <code>null</code> or an empty array indicate that there are no
     * restrictions on possible values. Note that this list only contains
     * values supported by this API Wrapper. This may exclude some options
     * that would be supported by Zemanta!
     */
    ZemantaPropertyEnum(String defaultValue, String... valueList) {
        this(false, defaultValue, valueList);
    }

    /**
     * A general property definition.
     *
     * @param required defines if the property is optional or required
     * @param defaultValue the value used if this parameter is not parsed.
     * <code>null</code> indicates no default configuration. This does not
     * mean, that Zemanta does not use an default value for requests that
     * do not contain this parameter
     * @param valueList the list of allowed values for this parameter.
     * <code>null</code> or an empty array indicate that there are no
     * restrictions on possible values. Note that this list only contains
     * values supported by this API Wrapper. This may exclude some options
     * that would be supported by Zemanta!
     */
    ZemantaPropertyEnum(boolean required, String defaultValue, String... valueList) {
        this.required = required;
        this.defaultValue = defaultValue;
        if (valueList != null && valueList.length > 0) {
            this.valueList = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(valueList)));
        }
        StringBuffer b = new StringBuffer(name());
        b.append('[');
        if (required) {
            b.append("optional");
        } else {
            b.append("required");
        }
        if (this.defaultValue != null) {
            b.append(",default='");
            b.append(this.defaultValue);
            b.append('\'');
        }
        if (this.valueList != null) {
            b.append(", valueList=");
            b.append(this.valueList);
        }
        b.append(']');
        this.toString = b.toString();
    }

    public boolean hasDefault() {
        return defaultValue != null;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public boolean hasValueList() {
        return valueList != null;
    }

    public boolean allowedValue(String value) {
        //if no value list is defined
        if (valueList == null) {
            // return only false if NOT optional and value == null
            return !(value == null && !required);
        } else {
            //check if the value is in the value list
            return valueList.contains(value);
        }
    }

    public Set<String> getValueList() {
        return valueList;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isOptional() {
        return !required;
    }

    @Override
    public String toString() {
        return toString;
    }

}
