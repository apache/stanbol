package org.apache.stanbol.contenthub.servicesapi.search;

import org.apache.stanbol.contenthub.servicesapi.AbstractContenthubException;

/**
 * @author anil.sinaci
 * 
 */
public class SearchException extends AbstractContenthubException {

    private static final long serialVersionUID = -8961306574004699946L;

    /**
     * @param msg
     */
    public SearchException(String msg) {
        super(msg);
    }

    /**
     * @param cause
     */
    public SearchException(Throwable cause) {
        super(cause);
    }
    
    /**
     * @param msg
     * @param cause
     */
    public SearchException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
