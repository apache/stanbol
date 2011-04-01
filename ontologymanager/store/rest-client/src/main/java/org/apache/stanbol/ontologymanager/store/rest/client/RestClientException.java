package org.apache.stanbol.ontologymanager.store.rest.client;

/**
 * Class to represent any exception in execution of methods of RestClient
 * 
 * @author Cihan
 */
public class RestClientException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = -5713995314670538506L;

    public RestClientException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
