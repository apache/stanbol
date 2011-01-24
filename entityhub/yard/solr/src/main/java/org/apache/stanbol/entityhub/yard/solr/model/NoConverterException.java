package org.apache.stanbol.entityhub.yard.solr.model;

import java.lang.reflect.Type;

/**
 * This exception is thrown when no adapter is available to do a required
 * java-object to {@link IndexValue} or {@link IndexValue} to java-object
 * adapter is registered to the used {@link IndexValueFactory}.
 *
 * @author Rupert Westenthaler
 */
public class NoConverterException extends RuntimeException {

    /**
     * default serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create an instance of <code>NoAdapterException</code>
     * indicating that no adapter is available for the type.
     *
     * @param type the type for which no adapter is available
     */
    public NoConverterException(Type type) {
        super("No adapter available for type "+type);
    }
}
