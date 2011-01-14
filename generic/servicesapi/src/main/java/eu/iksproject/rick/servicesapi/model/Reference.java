package eu.iksproject.rick.servicesapi.model;

/**
 * Defines a reference to an other entity <p>
 * Implementations of that interface MUST BE immutable
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
