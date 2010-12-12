package eu.iksproject.rick.servicesapi.model;

/**
 * Indicates, that the type of a source value is not compatible with the requested
 * DataType of the requested target value.
 * @author Rupert Westenthaler
 *
 */
public class UnsupportedTypeException extends RuntimeException {

    /**
     * uses the default serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    protected UnsupportedTypeException(Class<?> type,String dataType) {
        this(type,dataType,null);
    }

    protected UnsupportedTypeException(Class<?> type,String dataType, Throwable cause) {
        super(String.format("Values of Type \"%s\" are not supported for data type \"%s\"",
                type,dataType),cause);
    }

}
