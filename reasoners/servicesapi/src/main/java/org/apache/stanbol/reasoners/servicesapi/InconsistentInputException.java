package org.apache.stanbol.reasoners.servicesapi;


/**
 * The process cannot be completed because the input is inconsistent. This Exception must be used by reasoning
 * services which must stop the inference process if any inconsistency is found.
 */
public class InconsistentInputException extends Exception {
public InconsistentInputException() {
}
    public InconsistentInputException(Exception cause) {
        super(cause);
    }

    /**
	 * 
	 */
    private static final long serialVersionUID = 117198026192803326L;

}
