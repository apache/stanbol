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
package org.apache.stanbol.cmsadapter.jcr.utils;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.cmsadapter.servicesapi.helper.NamespaceEnum;

public class JCRUtils {
    private static final UriRef base64Uri = dataTypeURI("base64Binary");
    private static final UriRef dateTimeUri = dataTypeURI("dateTime");
    private static final UriRef booleanUri = dataTypeURI("boolean");
    private static final UriRef stringUri = dataTypeURI("string");
    private static final UriRef xsdInteger = dataTypeURI("integer");
    private static final UriRef xsdInt = dataTypeURI("int");
    private static final UriRef xsdShort = dataTypeURI("short");
    private static final UriRef xsdLong = dataTypeURI("long");
    private static final UriRef xsdDouble = dataTypeURI("double");
    private static final UriRef xsdAnyURI = dataTypeURI("anyURI");

    /**
     * This method converts the raw values of a property based on the specified property type.
     * 
     * @param propertyType
     *            type of a {@link Property}
     * @param values
     *            values of a {@link Property}
     * @return typed values
     */
    public static List<Object> getTypedPropertyValues(int propertyType, Value[] values) throws RepositoryException {
        List<Object> typedValues = new ArrayList<Object>();
        for (Value val : values) {
            typedValues.add(getTypedPropertyValue(propertyType, val));
        }
        return typedValues;
    }

    /**
     * This method converts the raw value of a property based on the specified property type.
     * 
     * @param propertyType
     *            type of a {@link Property}
     * @param value
     *            values of a {@link Property}
     * @return typed value
     */
    public static Object getTypedPropertyValue(int propertyType, Value value) throws RepositoryException {
        switch (propertyType) {
            case PropertyType.STRING:
                return value.getString();
            case PropertyType.BINARY:
                return value.getString();
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
                return value.getDate().getTime();
            case PropertyType.URI:
                return value.getString();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.DECIMAL:
                return value.getDecimal().toBigInteger();
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.PATH:
                return value.getString();
            default:
                return value.getString();
        }
    }

    /**
     * Return related {@link PropertyType} according to data type of a {@link Resource} if it is an instance
     * of {@link TypedLiteral} or {@link UriRef}, otherwise it return {@code PropertyType#STRING} as default
     * type.
     * 
     * @param r
     * @link {@link Resource} instance of which property type is demanded
     * @return related {@link PropertyType}
     */
    public static int getPropertyTypeByResource(Resource r) {
        if (r instanceof TypedLiteral) {
            UriRef type = ((TypedLiteral) r).getDataType();
            if (type.equals(stringUri)) {
                return PropertyType.STRING;
            } else if (type.equals(base64Uri)) {
                return PropertyType.BINARY;
            } else if (type.equals(booleanUri)) {
                return PropertyType.BOOLEAN;
            } else if (type.equals(dateTimeUri)) {
                return PropertyType.DATE;
            } else if (type.equals(xsdAnyURI)) {
                /*
                 * Normally this case should return PropertyType.URI, but JCR API seems to fail when
                 * retrieving values of URI typed properties.
                 */
                return PropertyType.STRING;
            } else if (type.equals(xsdDouble)) {
                return PropertyType.DOUBLE;
            } else if (type.equals(xsdInt)) {
                return PropertyType.DECIMAL;
            } else if (type.equals(xsdInteger)) {
                return PropertyType.DECIMAL;
            } else if (type.equals(xsdLong)) {
                return PropertyType.LONG;
            } else if (type.equals(xsdShort)) {
                return PropertyType.DECIMAL;
            } else {
                return PropertyType.STRING;
            }
        } else if (r instanceof UriRef) {
            /*
             * Normally this case should return PropertyType.URI, but JCR API seems to fail when retrieving
             * values of URI typed properties.
             */
            return PropertyType.STRING;
        } else {
            return PropertyType.STRING;
        }
    }

    private static UriRef dataTypeURI(String type) {
        return new UriRef(NamespaceEnum.xsd + type);
    }
}
