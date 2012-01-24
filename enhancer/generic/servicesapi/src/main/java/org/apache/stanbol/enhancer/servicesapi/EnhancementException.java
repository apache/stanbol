package org.apache.stanbol.enhancer.servicesapi;

/**
 * Abstract super class over {@link EngineException} and {@link Chain}
 * as typically used by components that need to used both
 * {@link EnhancementEngine}s and {@link Chain}s. <p>
 *
 */
public abstract class EnhancementException extends Exception {

    private static final long serialVersionUID = 1L;

    public EnhancementException(String message) {
        super(message);
    }

    public EnhancementException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnhancementException(Throwable cause) {
        super(cause);
    }
}
