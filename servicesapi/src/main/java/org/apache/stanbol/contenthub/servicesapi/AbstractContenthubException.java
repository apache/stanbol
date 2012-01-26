package org.apache.stanbol.contenthub.servicesapi;

/**
 * 
 * @author anil.sinaci
 *
 */
public abstract class AbstractContenthubException extends Exception {

    private static final long serialVersionUID = -2303415622874917355L;

    protected AbstractContenthubException(String msg) {
        super(msg);
    }
    
    protected AbstractContenthubException(Throwable cause) {
        super(cause);
    }
    
    protected AbstractContenthubException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
