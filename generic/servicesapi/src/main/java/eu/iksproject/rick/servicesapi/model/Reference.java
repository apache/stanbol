package eu.iksproject.rick.servicesapi.model;

/**
 * Defines a reference to an other entity
 * @author Rupert Westenthaler
 *
 */
public interface Reference {
    /**
     * Getter for the reference (not <code>null</code>)
     * @return the reference
     */
    String getReference();
    /**
     * The lexical representation of the reference (usually the same value
     * as returned by {@link #getReference()}
     * @return the reference
     */
    String toString();
}
