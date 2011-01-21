package org.apache.stanbol.entityhub.servicesapi.yard;

import org.apache.stanbol.entityhub.servicesapi.EntityhubException;

/**
 * Used to indicate an error while performing an operation on a yard
 * @author Rupert Westenthaler
 *
 */
public class YardException extends EntityhubException {

    /**
     * The default serial version UIR
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message and a cause
     * @param reason the message describing the reason for the error
     * @param cause the parent
     */
    public YardException(String reason, Throwable cause) {
        super(reason, cause);
    }

    /**
     * Creates an exception with a message and a cause
     * @param reason the message describing the reason for the error
     */
    public YardException(String reason) {
        super(reason);
    }

}
