package eu.iksproject.rick.servicesapi;

/**
 * Indicates an error while performing an operation within the RICK.<p>
 * This class is abstract use one of the more specific subclasses
 * @author Rupert Westenthaler
 *
 */
public abstract class RickException extends Exception {

    /**
     * default serial version uid.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message and a cause
     * @param reason the message describing the reason for the error
     * @param cause the parent
     */
    protected RickException(String reason, Throwable cause) {
        super(reason, cause);
    }

    /**
     * Creates an exception with a message and a cause
     * @param reason the message describing the reason for the error
     */
    protected RickException(String reason) {
        super(reason);
    }

}
