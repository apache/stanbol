package org.apache.stanbol.cmsadapter.servicesapi.mapping;

public class RDFBridgeException extends Exception {
    private static final long serialVersionUID = 7175642856547106105L;

    public RDFBridgeException(String message) {
        super(message);
    }

    public RDFBridgeException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
