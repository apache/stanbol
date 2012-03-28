package org.apache.stanbol.rules.base.api;

public class AlreadyExistingRecipeException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private String message;

    public AlreadyExistingRecipeException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
