package eu.iksproject.rick.servicesapi.model;

/**
 * Indicates, that the requested type is not supported. <p>
 * The definition of the model requires some types to be supported.
 * Implementation may support additional types. Components that use a specific
 * implementation may therefore use types that are not required to be supported.
 * However such components should also be able to deal with this kind of
 * exceptions.
 * 
 * @author Rupert Westenthaler
 *
 */
public class UnsupportedTypeException extends IllegalArgumentException {

    /**
     * uses the default serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    public UnsupportedTypeException(Class<?> type,String dataType) {
        this(type,dataType,null);
    }

    public UnsupportedTypeException(Class<?> type,String dataType, Throwable cause) {
        super(String.format("Values of Type \"%s\" are not supported for data type \"%s\"",
                type,dataType),cause);
    }

}
