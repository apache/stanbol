package org.apache.stanbol.enhancer.servicesapi;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Indicates that a COntent Item doesn't has the requested part
 *
 */
public class NoSuchPartException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoSuchPartException(int index) {
		super("The Content Item has no part with index "+index);
	}
    public NoSuchPartException(UriRef partUri) {
        super("The Content Item has no part with index "+partUri);
    }
	public NoSuchPartException(String message) {
		super(message);
	}

}
