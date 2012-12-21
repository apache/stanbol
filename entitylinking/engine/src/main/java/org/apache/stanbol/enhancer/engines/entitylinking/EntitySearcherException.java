package org.apache.stanbol.enhancer.engines.entitylinking;

public class EntitySearcherException extends Exception {

    /** default serial version UID */
    private static final long serialVersionUID = 1L;

    public EntitySearcherException(String message) {
        super(message);
    }

    public EntitySearcherException(Throwable cause) {
        super(cause);
    }

    public EntitySearcherException(String message, Throwable cause) {
        super(message, cause);
    }

}
