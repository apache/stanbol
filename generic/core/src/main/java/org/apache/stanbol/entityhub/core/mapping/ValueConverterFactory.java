package org.apache.stanbol.entityhub.core.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections.map.ReferenceMap;
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
    /**
     * Map with the instances. Weak references are used for both keys and values.
     */
    @SuppressWarnings("unchecked")
    private static Map<ValueFactory,ValueConverterFactory> instances = Collections.synchronizedMap(
            new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK, true));
    /**
     * Getter for the ValueConverterFactory instance using the default {@link ValueFactory}
     * @return the default ValueConverterFactory instance
     */
    public static ValueConverterFactory getInstance(){
        return getInstance(null);
    }
    /**
     * Getter for the ValueConverterFactory instance using a specific
     * {@link ValueFactory}.
     * @param valueFactory the valueFatory
     * @return the ValueConverterFactory for the parsed {@link ValueFactory}.
     */
    public static ValueConverterFactory getInstance(ValueFactory valueFactory){
        if(valueFactory == null){
            valueFactory = InMemoryValueFactory.getInstance();
        }
        synchronized (instances) {
            ValueConverterFactory converter = instances.get(valueFactory);
            if(converter == null){
                //create read only instance!
                converter = new ValueConverterFactory(valueFactory,true);
                instances.put(valueFactory, converter);
            }
            return converter;
        }
    }
    /**
     * The {@link ValueFactory} used by converters to create instances for converted
     * values.
     */
    protected final ValueFactory valueFactory;
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
    public ValueConverterFactory(ValueFactory valueFactory){
        this(valueFactory,false);
    }
    /**
     * Internally used to ensure readonly state for instances created by the
     * static {@link #getInstance(ValueFactory)} methods.
     * @see #ValueConverterFactory(ValueFactory)
     */
    private ValueConverterFactory(ValueFactory valueFactory,boolean readonly){
        if(valueFactory == null){
            this.valueFactory = InMemoryValueFactory.getInstance();
        } else {
            this.valueFactory = valueFactory;
        }
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
    public Object convert(Object value,String dataTypeUri) throws IllegalArgumentException {
        if(dataTypeUri == null){
            throw new IllegalArgumentException("The parsed datatype URI MUST NOT be NULL!");
        }
        ValueConverter<?> converter = uri2converter.get(dataTypeUri);
        return converter != null?converter.convert(value):null;
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
         * @return the converted value or <code>null</code> if the conversion was not
         * possible
         */
        T convert(Object value);
    }
    public class BooleanConverter implements ValueConverter<Boolean>{

        @Override
        public Boolean convert(Object value) {
            if (value == null) return null;
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
    public class ByteConverter implements ValueConverter<Byte>{
        @Override
        public Byte convert(Object value) {
            if (value == null) return null;
            if(value instanceof Byte){
                return (Byte)value;
            } else {
                try {
                    return Byte.parseByte(value.toString());
                } catch (NumberFormatException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Byte.getUri();}

    }
    public class ShortConverter implements ValueConverter<Short>{
        @Override
        public Short convert(Object value) {
            if (value == null) return null;
            if(value instanceof Short){
                return (Short)value;
            } else {
                try {
                    return Short.parseShort(value.toString());
                } catch (NumberFormatException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Short.getUri();}

    }
    public class IntConverter implements ValueConverter<Integer>{
        @Override
        public Integer convert(Object value) {
            if (value == null) return null;
            if(value instanceof Integer){
                return (Integer)value;
            } else {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Int.getUri();}

    }
    public class LongConverter implements ValueConverter<Long>{
        @Override
        public Long convert(Object value) {
            if (value == null) return null;
            if(value instanceof Long){
                return (Long)value;
            } else {
                try {
                    return Long.parseLong(value.toString());
                } catch (NumberFormatException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Long.getUri();}
    }
    public class FloatConverter implements ValueConverter<Float>{
        @Override
        public Float convert(Object value) {
            if (value == null) return null;
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
    public class DoubleConverter implements ValueConverter<Double>{
        @Override
        public Double convert(Object value) {
            if (value == null) return null;
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
    public class IntegerConverter implements ValueConverter<BigInteger>{
        @Override
        public BigInteger convert(Object value) {
            if (value == null) return null;
            if(value instanceof BigInteger){
                return (BigInteger)value;
            } else {
                try {
                    return new BigInteger(value.toString());
                } catch (NumberFormatException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.Integer.getUri();}
    }
    public class DecimalConverter implements ValueConverter<BigDecimal>{
        @Override
        public BigDecimal convert(Object value) {
            if (value == null) return null;
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
    public class AnyUriConverter implements ValueConverter<Reference>{
        @Override
        public Reference convert(Object value) {
            if (value == null) return null;
            if(value instanceof Reference){
                return (Reference)value;
            } else if(value instanceof URI || value instanceof URL){
                return valueFactory.createReference(value);
            } else {
                try {
                    new URI(value.toString()); //just for validating the string
                    return valueFactory.createReference(value);
                } catch (URISyntaxException e){ return null;}
            }
        }
        @Override
        public String getDataType() {return DataTypeEnum.AnyUri.getUri();}
    }
    public class ReferenceConverter extends AnyUriConverter {
        //same as AnyUri just parse Reference as DataType
        @Override
        public String getDataType() {return DataTypeEnum.Reference.getUri();}
    }
    public class DateTimeConverter implements ValueConverter<Date>{
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
        public Date convert(Object value) {
            if (value == null) return null;
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
    public class DateConverter extends DateTimeConverter {
        public DateConverter(){
            super(DataTypeEnum.Date);
        }
    }
    public class TimeConverter extends DateTimeConverter {
        public TimeConverter(){
            super(DataTypeEnum.Time);
        }
    }
    public class TextConverter implements ValueConverter<Text> {
        @Override
        public Text convert(Object value) {
            if (value == null) return null;
            if(value instanceof Text){
                return (Text)value;
            } else {
                return valueFactory.createText(value);
            }
        }
        @Override
        public String getDataType() { return DataTypeEnum.Text.getUri(); }
    }
    public class StringConverter implements ValueConverter<String> {
        @Override
        public String convert(Object value) { return value.toString(); }
        @Override
        public String getDataType() { return DataTypeEnum.String.getUri(); }
    }
    public class DurationConverter implements ValueConverter<Duration> {
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
        public Duration convert(Object value) {
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
