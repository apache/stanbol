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
package org.apache.stanbol.entityhub.core.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.utils.TimeUtils;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;


/**
 * This class is used to convert values to a specific dataType. By default the
 * Factory comes initialised with converters for all dataTypes defined in the
 * {@link DataTypeEnum}.<p>
 * When the default configuration is sufficient, than one should use the
 * static {@link #getInstance()} methods. When one needs to change the configuration
 * it is advised to create an own instance by using the
 * {@link #ValueConverterFactory(ValueFactory)}.<p>
 * Calling {@link #registerConverter(ValueConverter)} on an instance created by
 * the static {@link #getInstance()} methods will result in an
 * {@link IllegalStateException}.
 *
 * @author Rupert Westenthaler
 *
 */
public class ValueConverterFactory {
    
    private static ValueConverterFactory defaultInstance;
    /**
     * Getter for the ValueConverterFactory instance using the default configurations
     * of converters. This configuration can not be changed.<p>
     * If you need to use a specific configuration use the public constructor
     * to create your own private instance!
     * @return the default ValueConverterFactory instance
     */
    public static ValueConverterFactory getDefaultInstance(){
        if(defaultInstance == null){
            defaultInstance = new ValueConverterFactory(true);
        }
        return defaultInstance;
    }
    private boolean readonly = false;
    /**
     * Creates a new factory instance that supports conversions for all
     * datatypes defines in {@link DataTypeEnum}.<p>
     * Please note the static {@link #getInstance(ValueFactory)} methods that
     * should be used instead if one do not plan to change the configuration of
     * the created instance.
     * @param valueFactory the {@link ValueFactory} instance to be used to
     * create {@link Text} and {@link Reference} instances. If <code>null</code>
     * the {@link InMemoryValueFactory} is used.
     */
    public ValueConverterFactory(){
        this(false);
    }
    /**
     * Internally used to ensure readonly state for instances created by the
     * static {@link #getInstance(ValueFactory)} methods.
     * @see #ValueConverterFactory(ValueFactory)
     */
    private ValueConverterFactory(boolean readonly){
        init();
        this.readonly = readonly;
    }
    /**
     * Populates the factory with the default configuration that supports all
     * {@link DataTypeEnum} entries.
     */
    private void init(){
        registerConverter(new AnyUriConverter());
        registerConverter(new BooleanConverter());
        registerConverter(new ByteConverter());
        registerConverter(new DateConverter());
        registerConverter(new DateTimeConverter());
        registerConverter(new DecimalConverter());
        registerConverter(new DoubleConverter());
        registerConverter(new DurationConverter());
        registerConverter(new FloatConverter());
        registerConverter(new IntConverter());
        registerConverter(new IntegerConverter());
        registerConverter(new LongConverter());
        registerConverter(new ReferenceConverter());
        registerConverter(new ShortConverter());
        registerConverter(new StringConverter());
        registerConverter(new TextConverter());
        registerConverter(new TimeConverter());
    }
    Map<String,ValueConverter<?>> uri2converter = new HashMap<String, ValueConverter<?>>();
//    Map<Class<?>,ValueConverter<?>> type2converter = new HashMap<Class<?>, ValueConverter<?>>();
    /**
     * Registers a converter for the {@link ValueConverter#getDataType()}. If
     * a converter for this datatype is already present, than it is replaced by
     * this one.
     */
    protected void registerConverter(ValueConverter<?> converter){
        if(readonly){
            throw new IllegalStateException("Unable to register an converter for a read only ValueConverter Factory!" +
                    "Do not use the static getInstance(..) Methods if you need to change the configuration.");
        }
        uri2converter.put(converter.getDataType(), converter);
    }
//    public <T> ValueConverter<T> getConverter(Class<T> javaType){
//        return null;
//    }
    /**
     * Getter for the converter of the parsed datatype uri.
     * @param the uri of the datatype. For datatypes registered in the
     *   {@link DataTypeEnum} the {@link DataTypeEnum#getUri()} should be used.
     * @return the converter or <code>null</code> if no converter is present for
     * the parsed datatype uri.
     */
    public ValueConverter<?> getConverter(String dataTypeUri){
        return uri2converter.get(dataTypeUri);

    }
    /**
     * Converts the parsed value to the specified dataType.
     * @param value the value to convert. <code>null</code> is parsed to the
     *    converter and may be supported for some datatypes. If not supported,
     *    than parsing <code>null</code> results in <code>null</code> to be
     *    returned.
     * @param dataTypeUri the URI of the dataType
     * @return the converted value or <code>null</code> if no conversion was
     * possible.
     * @throws IllegalArgumentException if the parsed dataTyeUri is <code>null</code>
     */
    public Object convert(Object value,String dataTypeUri,ValueFactory valueFactory) throws IllegalArgumentException {
        if(dataTypeUri == null){
            throw new IllegalArgumentException("The parsed datatype URI MUST NOT be NULL!");
        }
        ValueConverter<?> converter = uri2converter.get(dataTypeUri);
        return converter != null?converter.convert(value,valueFactory):null;
    }


    /*--------------------------------------------------------------------------
     *    Implementation of the ValueConverters for the dataTypes defined by
     *    DataTypeEnum
     * -------------------------------------------------------------------------
     */
    /**
     * This Interface defines an simple converter interface that allows a
     * registry to get metadata about the type the converter can create and
     * second the {@link #convert(Object)} method that is called to convert
     * to the target type.
     * @author Rupert Westenthaler
     *
     * @param <T> the type of created objects
     */
    public static interface ValueConverter<T> {

        /**
         * The URI of the dataType created by this converter
         * @return the data type
         */
        String getDataType();
        /**
         * Converts the Value or returns <code>null</code> if the conversion was not
         * possible.
         * @param value the value to convert. <code>null</code> is parsed to the
         *    converter and may be supported for some datatypes. If not supported,
         *    than parsing <code>null</code> results in <code>null</code> to be
         *    returned.
         * @param valueFactory the ValueFactory used to create the converted value
         * @return the converted value or <code>null</code> if the conversion was not
         * possible
         */
        T convert(Object value,ValueFactory valueFactory);
    }
    public static class BooleanConverter implements ValueConverter<Boolean>{

        @Override
        public Boolean convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Boolean){
                return (Boolean)value;
            } else {
                //can not use the Boolean.parse method, because I need to return
                //null for strings != true || false ...
                String strValue = value.toString();
                return "true".equalsIgnoreCase(strValue)?
                        Boolean.TRUE:"false".equalsIgnoreCase(strValue)?
                                Boolean.FALSE:null;
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Boolean.getUri();}

    }
    public static class ByteConverter implements ValueConverter<Byte>{
        @Override
        public Byte convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Byte){
                return (Byte)value;
            } else if(value instanceof Long || value instanceof Integer || value instanceof Short){
                long longValue = ((Long)value).longValue();
                if(longValue <= Byte.MAX_VALUE && longValue >= Byte.MIN_VALUE){
                    return (byte)longValue;
                } else {
                    return null;
                }
            } else if(value instanceof Float || value instanceof Double){
                try {
                    return BigDecimal.valueOf(((Number)value).doubleValue()).byteValueExact();
                } catch (ArithmeticException e) { return null; }
            } else {
                try {
                    return new BigDecimal(value.toString()).byteValueExact();
                } catch (NumberFormatException e){ return null;
                } catch (ArithmeticException e) { return null; }
            }        }
        @Override
        public String getDataType() {return DataTypeEnum.Byte.getUri();}

    }
    public static class ShortConverter implements ValueConverter<Short>{
        @Override
        public Short convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Short){
                return (Short)value;
            } else if(value instanceof Long || value instanceof Integer){
                long longValue = ((Long)value).longValue();
                if(longValue <= Short.MAX_VALUE && longValue >= Short.MIN_VALUE){
                    return (short)longValue;
                } else {
                    return null;
                }
            } else if(value instanceof Float || value instanceof Double){
                try {
                    return BigDecimal.valueOf(((Number)value).doubleValue()).shortValueExact();
                } catch (ArithmeticException e) { return null; }
            } else {
                try {
                    return new BigDecimal(value.toString()).shortValueExact();
                } catch (NumberFormatException e){ return null;
                } catch (ArithmeticException e) { return null; }
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Short.getUri();}

    }
    public static class IntConverter implements ValueConverter<Integer>{
        @Override
        public Integer convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Integer){
                return (Integer)value;
            } else if(value instanceof Long){
                long longValue = ((Long)value).longValue();
                if(longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE){
                    return (int)longValue;
                } else {
                    return null;
                }
            } else if(value instanceof Float || value instanceof Double){
                try {
                    return BigDecimal.valueOf(((Number)value).doubleValue()).intValueExact();
                } catch (ArithmeticException e) { return null; }
            } else {
                try {
                    return new BigDecimal(value.toString()).intValueExact();
                } catch (NumberFormatException e){ return null;
                } catch (ArithmeticException e) { return null; }
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Int.getUri();}

    }
    public static class LongConverter implements ValueConverter<Long>{
        @Override
        public Long convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Long){
                return (Long)value;
            } else if(value instanceof Integer){
                return ((Integer)value).longValue();
            } else if(value instanceof Float || value instanceof Double){
                try {
                    return BigDecimal.valueOf(((Number)value).doubleValue()).longValueExact();
                } catch (ArithmeticException e) { return null; }
            } else {
                try {
                    return new BigDecimal(value.toString()).longValueExact();
                } catch (NumberFormatException e){ return null;
                } catch (ArithmeticException e) { return null; }
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Long.getUri();}
    }
    public static class FloatConverter implements ValueConverter<Float>{
        @Override
        public Float convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Float){
                return (Float)value;
            } else {
                try {
                    return Float.parseFloat(value.toString());
                } catch (NumberFormatException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Float.getUri();}
    }
    public static class DoubleConverter implements ValueConverter<Double>{
        @Override
        public Double convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Double){
                return (Double)value;
            } else {
                try {
                    return Double.parseDouble(value.toString());
                } catch (NumberFormatException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Double.getUri();}
    }
    public static class IntegerConverter implements ValueConverter<BigInteger>{
        @Override
        public BigInteger convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof BigInteger){
                return (BigInteger)value;
            } else {
                try { //would also support 10000000000000000000000000000000.0
                    return new BigDecimal(value.toString()).toBigIntegerExact();
                } catch (NumberFormatException e){ return null;
                } catch (ArithmeticException e) { return null; }
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Integer.getUri();}
    }
    public static class DecimalConverter implements ValueConverter<BigDecimal>{
        @Override
        public BigDecimal convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof BigDecimal){
                return (BigDecimal)value;
            } else {
                try {
                    return new BigDecimal(value.toString());
                } catch (NumberFormatException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Decimal.getUri();}
    }
    public static class AnyUriConverter implements ValueConverter<Reference>{
        @Override
        public Reference convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Reference){
                return (Reference)value;
            } else if(value instanceof URI || value instanceof URL){
                return valueFactory.createReference(value);
            } else {
                try {
                    //For converting we only accept absolute URIs
                    if(new URI(value.toString()).isAbsolute()){;
                        return valueFactory.createReference(value);
                    } else {
                        return null;
                    }
                } catch (URISyntaxException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.AnyUri.getUri();}
    }
    public static class ReferenceConverter extends AnyUriConverter {
        //same as AnyUri just parse Reference as DataType
        @Override
        public String getDataType() {return DataTypeEnum.Reference.getUri();}
    }
    public static class DateTimeConverter implements ValueConverter<Date>{
        private final DataTypeEnum dataType;
        public DateTimeConverter(){
            this.dataType = DataTypeEnum.DateTime;
        }
        protected DateTimeConverter(DataTypeEnum dataType){
            if(dataType == null){
                throw new IllegalArgumentException("Parsed DataType MUST NOT be NULL");
            }
            this.dataType = dataType;
        }
        @Override
        public Date convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Date){
                return (Date)value;
            } else if(value instanceof XMLGregorianCalendar){
                return ((XMLGregorianCalendar)value).toGregorianCalendar().getTime();
            } else if(value instanceof Calendar){
                return ((Calendar)value).getTime();
            } else {
                try {
                    return TimeUtils.toDate(dataType, value);
                } catch (RuntimeException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return dataType.getUri();}
    }
    public static class DateConverter extends DateTimeConverter {
        public DateConverter(){
            super(DataTypeEnum.Date);
        }
    }
    public static class TimeConverter extends DateTimeConverter {
        public TimeConverter(){
            super(DataTypeEnum.Time);
        }
    }
    /**
     * Converts String values to {@link Text} (without an language tag. Does
     * NOT convert any other values such as {@link Number}s or {@link Reference}s
     * @author Rupert Westenthaler
     *
     */
    public static class TextConverter implements ValueConverter<Text> {
        @Override
        public Text convert(Object value, ValueFactory valueFactory) {
            if (value == null) {
                return null;
            }
            if(value instanceof Text){
                return (Text)value;
            } else if(value instanceof String){
                return valueFactory.createText(value);
            } else { //do not convert other values
                return null;
            }
        }
        @Override
        public String getDataType() { return DataTypeEnum.Text.getUri(); }
    }
    /**
     * Converts any value to {@link String} by using the {@link #toString()}
     * method of the parsed value
     * @author Rupert Westenthaler
     *
     */
    public static class StringConverter implements ValueConverter<String> {
        @Override
        public String convert(Object value, ValueFactory valueFactory) { return value.toString(); }
        @Override
        public String getDataType() { return DataTypeEnum.String.getUri(); }
    }
    public static class DurationConverter implements ValueConverter<Duration> {
        private boolean nullAsZeroLengthDuration;
        /**
         * Creates a converter for durations
         */
        public DurationConverter(){
            this(false);
        }
        /**
         * Creates a converter for durations
         * @param nullAsZeroLengthDuration if true, than null values parsed to the
         * {@link #convert(Object)} are interpreted as durations with zero length.
         */
        public DurationConverter(boolean nullAsZeroLengthDuration){
            this.nullAsZeroLengthDuration = nullAsZeroLengthDuration;
        }
        @Override
        public Duration convert(Object value, ValueFactory valueFactory) {
            if(value == null){
                if(nullAsZeroLengthDuration){
                    return TimeUtils.toDuration(null,true);
                } else {
                    return null;
                }
            } else  if(value instanceof Duration){
                return (Duration)value;
            } else {
                try{
                    return TimeUtils.toDuration(value);
                } catch (RuntimeException e){ return null; }
            }
        }
        /**
         * Getter for the state if a null value should be interpreted as a
         * duration with zero length.
         * @return the state
         */
        public final boolean isNullAsZeroLengthDuration() {
            return nullAsZeroLengthDuration;
        }
        /**
         * Setter for the state if a null value should be interpreted as a
         * duration with zero length.
         * @param nullAsZeroLengthDuration the new state
         */
        public final void setNullAsZeroLengthDuration(boolean nullAsZeroLengthDuration) {
            this.nullAsZeroLengthDuration = nullAsZeroLengthDuration;
        }
        @Override
        public String getDataType() { return DataTypeEnum.Duration.getUri(); }
    }
}
