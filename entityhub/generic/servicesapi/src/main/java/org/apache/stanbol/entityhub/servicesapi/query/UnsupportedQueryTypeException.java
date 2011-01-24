package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.Collection;

/**
 * RuntimeException that indicated, that the tyoe of the parsed {@link Query}
 * is not supported by the {@link QueryService}
 * @author Rupert Westenthaler
 *
 */
public class UnsupportedQueryTypeException extends IllegalArgumentException {

    /**
     * Default serial version id
     */
    private static final long serialVersionUID = 1L;
    public UnsupportedQueryTypeException(String unsupportedQueryType, Collection<String> supportedTypes) {
        this(unsupportedQueryType,supportedTypes,null);
    }
    public UnsupportedQueryTypeException(String unsupportedQueryType,
            Collection<String> supportedTypes,String serviceName) {
        super(String.format("Queries of type {} are not supported by {} (supportedTypes: {}",
                unsupportedQueryType,
                (serviceName!=null?serviceName:"this query service"),
                supportedTypes));
    }
}
