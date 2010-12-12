package eu.iksproject.rick.servicesapi.model;

/**
 * Defines a natural language text in a given language.
 * @author Rupert Westenthaler
 */
public interface Text {
    /**
     * Getter for the text (not <code>null</code>)
     * @return the text
     */
    String getText();
    /**
     * Getter for the language. <code>null</code> indicates, that the text
     * is not specific to a language (e.g. the name of a person)
     * @return the language
     */
    String getLanguage();
    /**
     * The text without language information - this is the same as returned
     * by {@link #getText()}
     * @return the text
     */
    String toString();
}
