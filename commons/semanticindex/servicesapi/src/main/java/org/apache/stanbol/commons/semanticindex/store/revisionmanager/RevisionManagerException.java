package org.apache.stanbol.commons.semanticindex.store.revisionmanager;

import org.apache.stanbol.commons.semanticindex.store.StoreException;

public class RevisionManagerException extends StoreException {

    private static final long serialVersionUID = -3536940194241661109L;

    /**
     * @param msg
     */
    public RevisionManagerException(String msg) {
        super(msg);
    }

    /**
     * @param cause
     */
    public RevisionManagerException(Throwable cause) {
        super(cause);
    }

    /**
     * @param msg
     * @param cause
     */
    public RevisionManagerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
