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
package org.apache.stanbol.enhancer.engines.geonames.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines information for a Property of an GET or POST request such as the name, required or optional,
 * default values and a list of possible values.
 * <p>
 * This class is intended to allow to define meta data about a used web service (e.g. within an
 * Enumeration)
 *
 * @author Rupert Westenthaler
 */
public class RequestProperty {

    private final String name;
    private final boolean required;
    private final String defaultValue;
    private final Set<String> valueList;
    private final String toString;

    /**
     * Constructs a Property definition for a (RESTful) web service.
     *
     * @param name the name of the property (MUST NOT be <code>null</code>)
     * @param required defines if the property is optional or required
     * @param defaultValue the value used if this parameter is not parsed.
     * <code>null</code> indicates no default configuration.
     * @param valueList the list of allowed values for this parameter.
     * <code>null</code> or an empty array indicate that there are no
     * restrictions on possible values.
     */
    protected RequestProperty(String name, boolean required, String defaultValue, String... valueList) {
        if (name == null) {
            throw new IllegalArgumentException("The name of an Porperty MUST NOT be NULL");
        }
        this.name = name;
        this.required = required;
        this.defaultValue = defaultValue;
        if (valueList != null && valueList.length > 0) {
            this.valueList = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(valueList)));
        } else {
            this.valueList = null;
        }
        StringBuffer b = new StringBuffer(name);
        b.append('[');
        if (required) {
            b.append("required");
        } else {
            b.append("optional");
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

    public String getName() {
        return name;
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
        // if no value list is defined
        if (valueList == null) {
            // return only false if required and value == null
            return !(value == null && isRequired());
        } else {
            // check if the value is in the value list or null and optional
            return valueList.contains(value) || (value == null && isOptional());
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

    /**
     * Encodes the Property for the given parameters.
     *
     * @param requestString The string builder used to create the request
     * @param first if the property is the first property added to the request
     * @param values the value(s) for the property. If <code>null</code> or an empty list, than the
     * {@link #defaultValue()} is added if present. Also if the parsed collection contains the
     * <code>null</code> value the {@link #defaultValue()} is added instead.
     *
     * @return <code>true</code> if the parsed request string was modified as a result of this call -
     *         meaning that parameter was added to the request.
     */
    public boolean encode(StringBuilder requestString, boolean first, Collection<String> values) {
        boolean added = false;
        if (values == null || values.isEmpty()) {
            // replace with null element to ensure the default value is added
            values = Collections.singleton(null);
        }
        for (String value : values) {
            if (value == null && hasDefault()) {
                value = defaultValue();
            }
            // NOTE: value == null may still be OK
            if (allowedValue(value)) {
                // NOTE: also this may still say that NULL is OK
                if (value != null) {
                    if (!first) {
                        requestString.append('&');
                    } else {
                        requestString.append('?');
                        first = false;
                    }
                    requestString.append(getName());
                    requestString.append('=');
                    try {
                        requestString.append(URLEncoder.encode(value, "UTF8"));
                        added = true;
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                } // else property is not present
            } else {
                // Illegal parameter
                GeonamesAPIWrapper.log.warn("Value " + value + " is not valied for property " + toString());
            }
        }
        return added;
    }

    @Override
    public String toString() {
        return toString;
    }
}
