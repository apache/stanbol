package org.apache.stanbol.enhancer.servicesapi;

/**
 * Enhancement Engine should throw this exception when the provided Content Item
 * does not match there declared expectation (i.e. a malformed JPEG file).
 *
 * @author ogrisel
 */
public class InvalidContentException extends EngineException {

    private static final long serialVersionUID = 1L;

    public InvalidContentException(String message) {
        super(message);
    }

    public InvalidContentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidContentException(EnhancementEngine ee, ContentItem ci,
            Throwable cause) {
        super(String.format("'%s' failed to process invalid content item '%s'"
                + " with type '%s': %s", ee.getClass().getSimpleName(),
                ci.getId(), ci.getMimeType(), cause.getMessage()), cause);
    }

}
