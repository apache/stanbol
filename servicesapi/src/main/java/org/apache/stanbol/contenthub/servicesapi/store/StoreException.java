package org.apache.stanbol.contenthub.servicesapi.store;

import org.apache.stanbol.contenthub.servicesapi.AbstractContenthubException;

/**
 * @author anil.sinaci
 *
 */
public class StoreException extends AbstractContenthubException {

    private static final long serialVersionUID = 5670121947624979014L;

    /**
     * @param msg
     */
    public StoreException(String msg) {
        super(msg);
    }
    
    /**
     * @param cause
     */
    public StoreException(Throwable cause) {
        super(cause);
    }

    /**
     * @param msg
     * @param cause
     */
    public StoreException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
