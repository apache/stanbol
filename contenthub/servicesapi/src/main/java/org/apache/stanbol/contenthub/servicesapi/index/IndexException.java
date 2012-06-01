package org.apache.stanbol.contenthub.servicesapi.index;

import org.apache.stanbol.contenthub.servicesapi.exception.ContenthubException;

/**
 * Indicates a problem with an {@link SemanticIndex}
 */
public class IndexException extends ContenthubException {

    private static final long serialVersionUID = 1528679884791958046L;

    public IndexException(String msg) {
		super(msg);
	}

	public IndexException(Throwable cause) {
		super(cause);
	}

	public IndexException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
