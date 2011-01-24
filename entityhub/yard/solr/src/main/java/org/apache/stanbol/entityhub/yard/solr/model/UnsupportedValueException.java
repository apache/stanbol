package org.apache.stanbol.entityhub.yard.solr.model;

/**
 * Thrown when a parsed object value can not be converted by the converter
 *
 * @author Rupert Westenthaler
 */
public class UnsupportedValueException extends RuntimeException {

    /**
     * default serial version UID
     */
    private static final long serialVersionUID = 1L;
    /**
     * Constructs the exception to be thrown if a converter does not support the
     * the parsed value {@link IndexValue}.
     * @param converter the converter (implement the {@link TypeConverter#toString()} method!)
     * @param type the IndexDataType
     * @param value the value
     */
    public UnsupportedValueException(TypeConverter<?> converter, IndexDataType type, Object value) {
        this(converter, type, value,null);
    }
    /**
     * Constructs the exception to be thrown if a converter does not support the
     * the parsed value {@link IndexValue}.
     * @param converter the converter (implement the {@link TypeConverter#toString()} method!)
     * @param type the IndexDataType
     * @param value the value
     * @param cause the cause
     */
    public UnsupportedValueException(TypeConverter<?> converter, IndexDataType type, Object value,Throwable cause) {
        super(String.format("%s does not support the parsed value %s! Value is not compatible with the parsed IndexDataType %s",
                converter,value,type),cause);
    }
}
