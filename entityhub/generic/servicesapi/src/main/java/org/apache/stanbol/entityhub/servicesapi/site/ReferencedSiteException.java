package org.apache.stanbol.entityhub.servicesapi.site;

import org.apache.stanbol.entityhub.servicesapi.EntityhubException;

public class ReferencedSiteException extends EntityhubException {

    /**
     * Default serial version id
     */
    private static final long serialVersionUID = 1L;

    public ReferencedSiteException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public ReferencedSiteException(String reason) {
        super(reason);
    }

}
