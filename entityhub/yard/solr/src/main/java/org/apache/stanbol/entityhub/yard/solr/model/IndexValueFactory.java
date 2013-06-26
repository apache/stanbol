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
package org.apache.stanbol.entityhub.yard.solr.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods to convert java objects to {@link IndexValue} and vice versa.
 * <p>
 * Implementation Note: This class needs to be thread save.
 * 
 * @author Rupert Westenthaler
 */
public class IndexValueFactory {

    private static Logger log = LoggerFactory.getLogger(IndexValueFactory.class);

    private static ValueFactory valueFactory = InMemoryValueFactory.getInstance();
    private static IndexValueFactory instance = new IndexValueFactory();
    static {
        // register the default converters
        instance.registerConverter(new BigDecimalConverter());
        instance.registerConverter(new BigIntegerConverter());
        instance.registerConverter(new DateConverter());
        instance.registerConverter(new BooleanConverter());
        instance.registerConverter(new DoubleConverter());
        instance.registerConverter(new FloatConverter());
        instance.registerConverter(new IntegerConverter());
        instance.registerConverter(new LongConverter());
        instance.registerConverter(new ReferenceConverter(valueFactory));
        instance.registerConverter(new StringConverter());
        instance.registerConverter(new TextConverter(valueFactory));
    }

    /**
     * Get a <code>IndexValueFactory</code>.
     * 
     * @return the <code>IndexValueFactory</code> instance
     */
    public static IndexValueFactory getInstance() {
        return instance;
    }

    // TODO: add support for IndexTypeConverter
    // private Map<IndexType,TypeConverter<?>> indexTypeConverters =
    // new HashMap<IndexType, TypeConverter<?>>();
    /**
     * Holds the java class to {@link TypeConverter} mapping for all converters registered for a Java Class.
     * <p>
     * NOTE: this implementation distinguishes between classed and interfaces, because for Classes a simple
     * get lookup in the Map can be used while for Interfaces we need to Iterate over the entries of the Map
     * and check with {@link Class#isAssignableFrom(Class)}.
     */
    private Map<Class<?>,TypeConverter<?>> javaClassConverters = Collections
            .synchronizedMap(new HashMap<Class<?>,TypeConverter<?>>());
    /**
     * Holds the java interface to {@link TypeConverter} mappings for all converters registered for a Java
     * Interface
     * <p>
     * NOTE: this implementation distinguishes between classed and interfaces, because for Classes a simple
     * get lookup in the Map can be used while for Interfaces we need to Iterate over the entries of the Map
     * and check with {@link Class#isAssignableFrom(Class)}.
     */
    private Map<Class<?>,TypeConverter<?>> javaInterfaceConverters = new HashMap<Class<?>,TypeConverter<?>>();

    /**
     * Registers a converter to this factory. Note that only one converter per java type can be registered
     * 
     * @see TypeConverter#getJavaType()
     * @param converter
     *            the converter to be registered
     */
    public void registerConverter(TypeConverter<?> converter) {
        if (converter == null) {
            return;
        }
        Class<?> javaType = converter.getJavaType();
        if (javaType.isInterface()) {
            // NOTE: To ensure thread save iterations over Entries of this Map
            // create new map instance, add to the new instance and replace reference
            // ... i know this is slow, but such calls are very uncommon
            Map<Class<?>,TypeConverter<?>> javaInterfaceConverterMap = new HashMap<Class<?>,TypeConverter<?>>(
                    this.javaInterfaceConverters);
            javaInterfaceConverterMap.put(javaType, converter);
            // TODO: add support for IndexTypeConverter
            this.javaInterfaceConverters = javaInterfaceConverterMap;
        } else {
            // there are no Iterations over this Map!
            javaClassConverters.put(javaType, converter);
        }
    }

    /**
     * Removes the converter for the parsed java type
     * 
     * @param type
     *            the java type
     * @return the removed converter or <code>null</code> if none was registered for the parsed type.
     */
    @SuppressWarnings("unchecked")
    public <T> TypeConverter<T> removeConverter(Class<T> type) {
        if (type == null) {
            return null;
        }
        TypeConverter<T> converter;
        if (type.isInterface()) {
            if (javaInterfaceConverters.containsKey(type)) {
                // create new map instance, remove to the converter and replace reference
                // ... i know this is slow, but such calls are very uncommon
                Map<Class<?>,TypeConverter<?>> javaInterfaceConverterMap = new HashMap<Class<?>,TypeConverter<?>>(
                        this.javaInterfaceConverters);
                converter = (TypeConverter<T>) javaInterfaceConverterMap.remove(type);
                this.javaInterfaceConverters = javaInterfaceConverterMap;
            } else {
                converter = null;
            }
        } else {
            converter = (TypeConverter<T>) javaClassConverters.remove(type);
        }
        return converter;
    }

    /**
     * Creates the value as used to index the parsed object
     * 
     * @param value
     *            the value to be indexed
     * @return the index representation of the parsed value
     * @throws NoConverterException
     *             thrown if <code>value</code> is of an invalid type
     * @throws IllegalArgumentException
     *             if the parsed value is null
     */
    @SuppressWarnings("unchecked")
    public IndexValue createIndexValue(Object value) throws NoConverterException, IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Parameter value MUST NOT be NULL!");
        }
        // first try to get the class and find a converter registered for a class
        TypeConverter<Object> converter = (TypeConverter<Object>) javaClassConverters.get(value.getClass());
        if (converter != null) {
            return converter.createIndexValue(value);
        }
        // if not successful we need still to search for converters registered for interfaces
        for (Entry<Class<?>,TypeConverter<?>> entry : javaInterfaceConverters.entrySet()) {
            if (entry.getKey().isAssignableFrom(value.getClass())) {
                return ((TypeConverter<Object>) entry.getValue()).createIndexValue(value);
            }
        }
        throw new NoConverterException(value.getClass());
    }

    /**
     * Converts a IndexValue instance to an instance of the specified class
     * 
     * @param <T>
     * @param type
     *            the <code>Class</code> of the returned object
     * @param indexValue
     *            the index value instance
     * @return a java object representing the value of the index value
     * @throws NoConverterException
     *             thrown if <code>type</code> is unsupported
     * @throws UnsupportedIndexTypeException
     *             if the {@link IndexDataType} of the parsed {@link IndexValue} is not supported by the
     *             registered converter
     * @throws IllegalArgumentException
     *             if any of the two parameter is <code>null</code>
     */
    public <T> T createValue(Class<T> type, IndexValue indexValue) throws NoConverterException,
                                                                  UnsupportedIndexTypeException,
                                                                  IllegalArgumentException {
        return createValue(type, indexValue.getType(), indexValue.getType(), indexValue.getLanguage());
    }

    /**
     * Converts a IndexValue instance to an instance of the specified class
     * 
     * @param <T>
     * @param javaType
     *            the requested java type
     * @param indexType
     *            the index type
     * @param indexValue
     *            the value in the index
     * @param language
     *            the language of the value in the index
     * @return a java object representing the value of the index value
     * @throws NoConverterException
     *             thrown if <code>type</code> is unsupported
     * @throws UnsupportedIndexTypeException
     *             if the {@link IndexDataType} of the parsed {@link IndexValue} is not supported by the
     *             registered converter
     * @throws IllegalArgumentException
     *             if any of the two parameter is <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public <T> T createValue(Class<T> javaType, IndexDataType indexType, Object indexValue, String language) throws NoConverterException,
                                                                                                            UnsupportedIndexTypeException,
                                                                                                            IllegalArgumentException {
        if (javaType == null) {
            throw new IllegalArgumentException("Parameter Class<T> type MUST NOT be NULL");
        }
        if (indexValue == null) {
            throw new IllegalArgumentException("Parameter IndexValue MUST NOT be NULL");
        }
        // search interface converter map if the parsed type is an interface
        TypeConverter<T> converter = (TypeConverter<T>) (javaType.isInterface() ? javaInterfaceConverters
                .get(javaType) : javaClassConverters.get(javaType));
        if (converter != null) {
            return converter.createObject(indexType, indexValue, language);
        } else {
            throw new NoConverterException(javaType);
        }
    }

    // TODO: add support for IndexTypeConverter
    // /**
    // * Converts a IndexValue instance to an java object. The type of the java
    // * object.
    // * @param indexValue the index value instance
    // * @return a java object representing the value of the index value
    // * @throws NoConverterException if no converter for the index value is registered
    // */
    // public Object createObject(IndexValue indexValue) throws NoConverterException {
    //
    // }

    /*
     * ==== Internal Classes for the default converter Implementations ====
     */

    public static class DateConverter implements TypeConverter<Date> {
        private static final DateTimeFormatter XML_DATE_TIME_FORMAT = ISODateTimeFormat.dateTime().withZone(
            DateTimeZone.UTC);
        private static final DateTimeFormatter XML_DATE_TIME_FORMAT_noMillis = ISODateTimeFormat
                .dateTimeNoMillis().withZone(DateTimeZone.UTC);
        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.DATE.getIndexType();

        @Override
        public IndexValue createIndexValue(Date value) {
            if(value == null){
                return null;
            }
            return new IndexValue(XML_DATE_TIME_FORMAT.print(value.getTime()), INDEX_TYPE);
        }

        @Override
        public Date createObject(IndexValue indexValue) {
            if (indexValue == null) {
                return null;
            }
            return createObject(indexValue.getType(), indexValue, indexValue.getLanguage());
        }

        @Override
        public Class<Date> getJavaType() {
            return Date.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public Date createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                               UnsupportedValueException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (!type.equals(INDEX_TYPE)) {
                throw new UnsupportedIndexTypeException(this, type);
            }
            if (value == null) {
                return null;
            }
            if (value instanceof Date) {
                return (Date) value;
            } else if (value instanceof Calendar) {
                return ((Calendar) value).getTime();
            } else {
                DateTime date;
                try {
                    // NOTE: Solr only support UTC ... so we need to change the Timezone
                    date = XML_DATE_TIME_FORMAT.parseDateTime(value.toString());
                } catch (IllegalArgumentException e) {
                    try {
                        date = XML_DATE_TIME_FORMAT_noMillis.parseDateTime(value.toString());
                    } catch (IllegalArgumentException e1) {
                        log.warn(
                            "Unable to parse Date/Time for Value "
                                    + value.toString()
                                    + " (use ISO date format (milliseconds optional))! -> no Date Mapping added!",
                            e1);
                        throw new UnsupportedValueException(this, type, value, e);
                    }
                }
                return date.toDate();
            }
        }

    }

    public static class BooleanConverter implements TypeConverter<Boolean> {

        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.BOOLEAN.getIndexType();

        @Override
        public IndexValue createIndexValue(Boolean value) {
            if (value == null) {
                return null;
            }
            return new IndexValue(value.toString(), INDEX_TYPE);
        }

        @Override
        public Boolean createObject(IndexValue value) {
            if (value == null) {
                return null;
            }
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public Class<Boolean> getJavaType() {
            return Boolean.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public Boolean createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                                  UnsupportedValueException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (!type.equals(INDEX_TYPE)) {
                throw new UnsupportedIndexTypeException(this, type);
            }
            if (value == null) {
                return null;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return Boolean.valueOf(value.toString());
            }
        }
    }

    public static class StringConverter implements TypeConverter<String> {
        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.STR.getIndexType();
        private boolean acceptAllIndexTypes;

        public final boolean isAcceptAllIndexTypes() {
            return acceptAllIndexTypes;
        }

        public final void setAcceptAllIndexTypes(boolean acceptAllIndexTypes) {
            this.acceptAllIndexTypes = acceptAllIndexTypes;
        }

        public StringConverter() {
            this(true);
        }

        public StringConverter(boolean acceptAllIndexTypes) {
            this.acceptAllIndexTypes = acceptAllIndexTypes;
        }

        @Override
        public IndexValue createIndexValue(String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new IndexValue(value, INDEX_TYPE);
        }

        @Override
        public String createObject(IndexValue value) {
            if (value == null) {
                return null;
            }
            // for now accept any IndexValue regardless of type
            // if(!value.getType().equals(INDEX_TYPE)){
            // new UnsupportedIndexTypeException(this, value);
            // }
            return value.getValue();
        }

        @Override
        public Class<String> getJavaType() {
            return String.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public String createObject(IndexDataType type, Object value, String lang) throws NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            return value != null ? value.toString() : null;
        }
    }

    public static class IntegerConverter implements TypeConverter<Integer> {
        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.INT.getIndexType();
        private boolean acceptLong;

        public final boolean isAcceptLong() {
            return acceptLong;
        }

        public final void setAcceptLong(boolean acceptLong) {
            this.acceptLong = acceptLong;
        }

        public IntegerConverter() {
            this(true);
        }

        public IntegerConverter(boolean acceptLongIndexType) {
            this.acceptLong = acceptLongIndexType;
        }

        @Override
        public IndexValue createIndexValue(Integer value) {
            if (value == null) {
                return null;
            }
            return new IndexValue(value.toString(), INDEX_TYPE);
        }

        @Override
        public Integer createObject(IndexValue value) {
            if (value == null) {
                return null;
            }
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public Class<Integer> getJavaType() {
            // TODO Auto-generated method stub
            return Integer.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public Integer createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                                  UnsupportedValueException,
                                                                                  NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (type.equals(INDEX_TYPE)) {
                if (value == null) { // move in here to ensure returning UnsupportedIndexTypeException on
                                     // wrong types
                    return null;
                }
                if (value instanceof Integer) {
                    return (Integer) value;
                } else {
                    try {
                        return Integer.valueOf(value.toString());
                    } catch (NumberFormatException e) {
                        throw new UnsupportedValueException(this, type, value, e);
                    }
                }
            } else if (acceptLong && type.equals(IndexDataTypeEnum.LONG.getIndexType())) {
                if (value == null) { // move in here to ensure returning UnsupportedIndexTypeException on
                                     // wrong types
                    return null;
                }
                long longValue;
                if (value instanceof Long) {
                    longValue = ((Long) value).longValue();
                } else {
                    try {
                        longValue = Long.parseLong(value.toString());
                    } catch (NumberFormatException e) {
                        throw new UnsupportedValueException(this, type, value, e);
                    }
                }
                if (Integer.MAX_VALUE >= longValue && Integer.MIN_VALUE <= longValue) {
                    return Integer.valueOf((int) longValue);
                } else {
                    // parsed long value outside of the int range
                    throw new UnsupportedValueException(
                            this,
                            type,
                            value,
                            new IllegalStateException(
                                    "Unable to convert LONG Value to Integer, because the value is outside of the Integer Range!"));
                }
            } else {
                throw new UnsupportedIndexTypeException(this, type);
            }
        }
    }

    public static class LongConverter implements TypeConverter<Long> {

        public static final IndexDataType LONG_TYPE = IndexDataTypeEnum.LONG.getIndexType();
        private static final IndexDataType INT_TYPE = IndexDataTypeEnum.INT.getIndexType();

        @Override
        public IndexValue createIndexValue(Long value) {
            if (value == null) {
                return null;
            }
            return new IndexValue(value.toString(), LONG_TYPE);
        }

        @Override
        public Long createObject(IndexValue value) {
            if (value == null) {
                return null;
            }
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public Class<Long> getJavaType() {
            return Long.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return LONG_TYPE;
        }

        @Override
        public Long createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                               UnsupportedValueException,
                                                                               NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (type.equals(LONG_TYPE) || type.equals(INT_TYPE)) {
                if (value == null) {
                    return null;
                }
                if (value instanceof Long) {
                    return (Long) value;
                } else if (value instanceof Integer) {
                    return ((Integer) value).longValue();
                } else {
                    try {
                        return new Long(value.toString());
                    } catch (NumberFormatException e) {
                        throw new UnsupportedValueException(this, type, value, e);
                    }
                }
            } else {
                throw new UnsupportedIndexTypeException(this, type);
            }
        }
    }

    public static class DoubleConverter implements TypeConverter<Double> {

        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.DOUBLE.getIndexType();
        private static final Set<IndexDataType> SUPPORTED = new HashSet<IndexDataType>(Arrays.asList(
            IndexDataTypeEnum.FLOAT.getIndexType(), IndexDataTypeEnum.INT.getIndexType(),
            IndexDataTypeEnum.LONG.getIndexType(), INDEX_TYPE));

        @Override
        public IndexValue createIndexValue(Double value) {
            if (value == null) {
                return null;
            }
            return new IndexValue(value.toString(), INDEX_TYPE);
        }

        @Override
        public Double createObject(IndexValue value) {
            if (value == null) {
                return null;
            }
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public Class<Double> getJavaType() {
            return Double.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public Double createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                                 UnsupportedValueException,
                                                                                 NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (SUPPORTED.contains(type)) {
                if (value == null) {
                    return null;
                }
                if (value instanceof Double) {
                    return (Double) value;
                } else if (value instanceof Float) {
                    return ((Float) value).doubleValue();
                } else {
                    try {
                        return new Double(value.toString());
                    } catch (NumberFormatException e) {
                        throw new UnsupportedValueException(this, type, value, e);
                    }
                }
            } else {
                throw new UnsupportedIndexTypeException(this, type);
            }
        }
    }

    public static class FloatConverter implements TypeConverter<Float> {
        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.FLOAT.getIndexType();
        private static final Collection<IndexDataType> DOUBLE_LONG_TYPES = Arrays.asList(
            IndexDataTypeEnum.LONG.getIndexType(), IndexDataTypeEnum.DOUBLE.getIndexType());
        private final Set<IndexDataType> supported = Collections
                .synchronizedSet(new HashSet<IndexDataType>());

        public FloatConverter() {
            this(true);
        }

        public FloatConverter(boolean acceptDoubleAndLongIndexType) {
            supported.addAll(Arrays.asList(IndexDataTypeEnum.INT.getIndexType(), INDEX_TYPE));
            setAcceptDoubleAndLongIndexTypes(acceptDoubleAndLongIndexType);
        }

        public boolean isAcceptDoubleAndLongIndexTypes() {
            return supported.containsAll(DOUBLE_LONG_TYPES);
        }

        public final void setAcceptDoubleAndLongIndexTypes(boolean state) {
            if (state) {
                supported.addAll(DOUBLE_LONG_TYPES);
            } else {
                supported.removeAll(DOUBLE_LONG_TYPES);
            }
        }

        @Override
        public IndexValue createIndexValue(Float value) {
            if (value == null) {
                return null;
            }
            return new IndexValue(value.toString(), INDEX_TYPE);
        }

        @Override
        public Float createObject(IndexValue value) {
            if (value == null) {
                return null;
            }
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public Class<Float> getJavaType() {
            return Float.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public Float createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                                UnsupportedValueException,
                                                                                NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (supported.contains(type)) {
                if (value == null) {
                    return null;
                }
                if (value instanceof Float) {
                    return (Float) value;
                } else if (value instanceof Double) {
                    return ((Double) value).floatValue();
                } else {
                    try {
                        return new Float(value.toString());
                    } catch (NumberFormatException e) {
                        throw new UnsupportedValueException(this, type, value, e);
                    }
                }
            } else {
                throw new UnsupportedIndexTypeException(this, type);
            }
        }
    }

    public static class BigIntegerConverter implements TypeConverter<BigInteger> {

        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.LONG.getIndexType();
        private static final IndexDataType INT_TYPE = IndexDataTypeEnum.INT.getIndexType();

        @Override
        public IndexValue createIndexValue(BigInteger value) {
            if (value == null) {
                return null;
            }
            return new IndexValue(value.toString(), INDEX_TYPE);
        }

        @Override
        public BigInteger createObject(IndexValue value) {
            if (value == null) {
                return null;
            }
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public Class<BigInteger> getJavaType() {
            return BigInteger.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public BigInteger createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                                     UnsupportedValueException,
                                                                                     NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (type.equals(INDEX_TYPE) || type.equals(INT_TYPE)) {
                if (value == null) {
                    return null;
                }
                try {
                    return new BigInteger(value.toString());
                } catch (NumberFormatException e) {
                    throw new UnsupportedValueException(this, type, value, e);
                }
            } else {
                throw new UnsupportedIndexTypeException(this, type);
            }
        }
    }

    public static class BigDecimalConverter implements TypeConverter<BigDecimal> {

        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.DOUBLE.getIndexType();
        private static final Set<IndexDataType> SUPPORTED = new HashSet<IndexDataType>(Arrays.asList(
            IndexDataTypeEnum.FLOAT.getIndexType(), IndexDataTypeEnum.INT.getIndexType(),
            IndexDataTypeEnum.LONG.getIndexType(), INDEX_TYPE));

        @Override
        public IndexValue createIndexValue(BigDecimal value) {
            if (value == null) {
                return null;
            }
            return new IndexValue(value.toString(), INDEX_TYPE);
        }

        @Override
        public BigDecimal createObject(IndexValue value) {
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public BigDecimal createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                                     UnsupportedValueException,
                                                                                     NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (SUPPORTED.contains(type)) {
                if (value == null) {
                    return null;
                }
                try {
                    return new BigDecimal(value.toString());
                } catch (NumberFormatException e) {
                    throw new UnsupportedValueException(this, type, value, e);
                }
            } else {
                throw new UnsupportedIndexTypeException(type);
            }
        }

        @Override
        public Class<BigDecimal> getJavaType() {
            return BigDecimal.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }
    }

    public static class TextConverter implements TypeConverter<Text> {

        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.TXT.getIndexType();
        private static final IndexDataType STRING_TYPE = IndexDataTypeEnum.STR.getIndexType();
        private final ValueFactory valueFactory;

        public TextConverter(ValueFactory valueFactory) {
            if (valueFactory == null) {
                throw new IllegalArgumentException("Parameter ValueFactory MUST NOT be NULL!");
            }
            this.valueFactory = valueFactory;
        }

        @Override
        public IndexValue createIndexValue(Text value) {
            if (value == null) {
                return null;
            }
            return new IndexValue(value.getText(), INDEX_TYPE, value.getLanguage());
        }

        @Override
        public Text createObject(IndexValue value) throws UnsupportedIndexTypeException {
            if (value == null) {
                return null;
            }
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public Class<Text> getJavaType() {
            return Text.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public Text createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                               UnsupportedValueException,
                                                                               NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (type.equals(INDEX_TYPE) || type.equals(STRING_TYPE)) {
                if (value == null) {
                    return null;
                }
                return valueFactory.createText(value.toString(), lang);
            } else {
                throw new UnsupportedIndexTypeException(this, type);
            }
        }
    }

    public static class ReferenceConverter implements TypeConverter<Reference> {
        public static final IndexDataType INDEX_TYPE = IndexDataTypeEnum.REF.getIndexType();
        private final ValueFactory valueFactory;

        public ReferenceConverter(ValueFactory valueFactory) {
            if (valueFactory == null) {
                throw new IllegalArgumentException("Parameter ValueFactory MUST NOT be NULL!");
            }
            this.valueFactory = valueFactory;
        }

        @Override
        public IndexValue createIndexValue(Reference value) {
            if (value == null || value.getReference() == null || value.getReference().isEmpty()) {
                return null;
            }
            return new IndexValue(value.getReference(), INDEX_TYPE);
        }

        @Override
        public Reference createObject(IndexValue value) throws UnsupportedIndexTypeException {
            if (value == null) {
                return null;
            }
            return createObject(value.getType(), value.getValue(), value.getLanguage());
        }

        @Override
        public Class<Reference> getJavaType() {
            return Reference.class;
        }

        @Override
        public IndexDataType getIndexType() {
            return INDEX_TYPE;
        }

        @Override
        public Reference createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                                    UnsupportedValueException,
                                                                                    NullPointerException {
            if (type == null) {
                throw new IllegalArgumentException("The parsed IndexDataType MUST NOT be null");
            }
            if (type.equals(INDEX_TYPE)) {
                if (value == null) {
                    return null;
                }
                return valueFactory.createReference(value.toString());
            } else {
                throw new UnsupportedIndexTypeException(this, type);
            }
        }
    }

}
