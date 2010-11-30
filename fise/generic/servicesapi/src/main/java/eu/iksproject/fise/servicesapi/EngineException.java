package eu.iksproject.fise.servicesapi;

/**
 * Base exception raised by EnhancementEngine implementations when they fail to
 * process the provided content item.
 *
 * If the failure is imputable to a malformed input in the
 * {@link ContentItem#getStream()} or {@link ContentItem#getMetadata()} one
 * should throw the subclass {@link InvalidContentException} instead.
 *
 * @author ogrisel
 */
public class EngineException extends Exception {

    private static final long serialVersionUID = 1L;

    public EngineException(String message) {
        super(message);
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }

    public EngineException(Throwable cause) {
        super(cause);
    }

    public EngineException(EnhancementEngine ee, ContentItem ci, Throwable cause) {
        super(String.format(
                "'%s' failed to process content item '%s' with type '%s': %s",
                ee.getClass().getSimpleName(), ci.getId(), ci.getMimeType(),
                cause.getMessage()), cause);
    }
}
