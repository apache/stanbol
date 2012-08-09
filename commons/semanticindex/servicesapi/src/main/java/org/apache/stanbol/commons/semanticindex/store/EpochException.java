package org.apache.stanbol.commons.semanticindex.store;

/**
 * Indicates that the active Epoch of an IndexingSource is different from the requested one.
 * 
 */
public class EpochException extends RuntimeException {

    /**
     * default serial version UID
     */
    private static final long serialVersionUID = 1L;

    private long requestedEpoch;

    private long activeEpoch;

    public EpochException(IndexingSource<?> source, long activeEpoch, long requestedEpoch) {
        super(String.format("The Epoch %s was requested but the %s '%s' uses %s as active Epoch",
            requestedEpoch, source.getClass().getSimpleName(), source.getName(), activeEpoch));
        this.requestedEpoch = requestedEpoch;
        this.activeEpoch = activeEpoch;
    }

    public long getRequestEpoch() {
        return this.requestedEpoch;
    }

    public long getActiveEpoch() {
        return this.activeEpoch;
    }
}
