package org.apache.stanbol.contenthub.servicesapi.ldpath;

import org.apache.stanbol.contenthub.servicesapi.AbstractContenthubException;

/**
 * @author anil.sinaci
 *
 */
public class LDPathException extends AbstractContenthubException {

    private static final long serialVersionUID = 4755524861924506181L;

    /**
     * @param msg
     */
    public LDPathException(String msg) {
        super(msg);
    }
    
    /**
     * @param cause
     */
    public LDPathException(Throwable cause) {
        super(cause);
    }

    /**
     * @param msg
     * @param cause
     */
    public LDPathException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
