package eu.iksproject.rick.servicesapi.site;

import eu.iksproject.rick.servicesapi.RickException;

public class ReferencedSiteException extends RickException {

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
