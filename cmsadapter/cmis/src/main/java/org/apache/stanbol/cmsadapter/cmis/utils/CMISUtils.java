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
package org.apache.stanbol.cmsadapter.cmis.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.clerezza.rdf.core.UriRef;

public class CMISUtils {
    public static final String RDF_METADATA_DOCUMENT_EXTENSION = "_metadata";

    /**
     * This method converts the raw values of a property based on the specified {@link PropertyType}.
     * 
     * @param property
     *            type of a {@link Property}
     * @param values
     *            values of a {@link Property}
     * @return typed values
     */
    public static List<Object> getTypedPropertyValues(PropertyType property, List<?> values) {
        List<Object> typedValues = new ArrayList<Object>();
        if (values != null) {
            for (Object v : values) {
                typedValues.add(getTypedPropertyValue(property, v));
            }
        }
        return typedValues;
    }

    /**
     * This method converts raw value of a property based on the specified {@link PropertyType}.
     * 
     * @param propertyType
     *            property type of a {@link Property}
     * @param value
     *            value of a {@link Property}
     * @return typed value
     */
    public static Object getTypedPropertyValue(PropertyType propertyType, Object value) {
        if (value == null) {
            return value;
        }
        switch (propertyType) {
            case BOOLEAN:
                return (Boolean) value;
            case DECIMAL:
                return (Integer) value;
            case DATETIME:
                return ((Calendar) value).getTime();
            case HTML:
                // not meet with this property
                return null;
            case ID:
                return value.toString();
            case INTEGER:
                return ((BigInteger) value).intValue();
            case STRING:
                return value;
            case URI:
                // not meet with this property
                return new UriRef(value.toString());
            default:
                return value.toString();
        }
    }
}
