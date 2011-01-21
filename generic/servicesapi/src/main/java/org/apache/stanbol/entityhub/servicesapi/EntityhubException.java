package org.apache.stanbol.entityhub.servicesapi;

/**
 * Indicates an error while performing an operation within the Entityhub.<p>
 * This class is abstract use one of the more specific subclasses
 * @author Rupert Westenthaler
 *
 */
public abstract class EntityhubException extends Exception {

    /**
     * default serial version uid.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message and a cause
     * @param reason the message describing the reason for the error
     * @param cause the parent
     */
    protected EntityhubException(String reason, Throwable cause) {
        super(reason, cause);
    }

    /**
     * Creates an exception with a message and a cause
     * @param reason the message describing the reason for the error
     */
    protected EntityhubException(String reason) {
        super(reason);
    }

}
