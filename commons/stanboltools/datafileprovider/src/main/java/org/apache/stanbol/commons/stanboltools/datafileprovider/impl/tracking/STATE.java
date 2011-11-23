package org.apache.stanbol.commons.stanboltools.datafileprovider.impl.tracking;

/**
 * The state of a DataFile. UNKNOWN indicates that this DataFile was
 * never tracked before.
 * @author Rupert Westenthaler
 *
 */
public enum STATE {
    /**
     * never checked
     */
    UNKNOWN,
    /**
     * not available on the last check
     */
    UNAVAILABLE,
    /**
     * available on the last check
     */
    AVAILABLE, 
    /**
     * Indicates that an ERROR was encountered while notifying an change
     * in the Event state
     */
    ERROR
}