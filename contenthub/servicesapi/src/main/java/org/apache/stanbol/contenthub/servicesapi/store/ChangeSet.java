package org.apache.stanbol.contenthub.servicesapi.store;

import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * This represents a set of {@link #size()} changes starting
 * {@link #fromRevision()} {@link #toRevision()} affecting
 * ContentItems with the {@link UriRef}s returned by
 * {@link #changed()};
 * <p>
 * The intended usage of this class is
 * <code><pre>
 *     Store store; //the store
 *     SemanticIndex index; //the index to apply the changes
 *     long revision = Long.MIN_VALUE; //start for scatch
 *     int batchSize = 1000;
 *     ChangeSet cs;
 *     do {
 *         cs = store.changes(revision);
 *         for(UriRef changed : cs.changed()){
 *             ContentItem ci = store.get(changed);
 *             if(ci == null){
 *                 index.remove(changed);
 *             } else {
 *                 index.index(ci);
 *             }
 *         } 
 *     while(!cs.changed().isEmpty());
 *     index.persist(cs.fromRevision());
 * </pre></code>
 */
public interface ChangeSet {
	/**
	 * The lowest revision number included in this ChangeSet
	 * @return the lowest revision number of this set
	 */
    long fromRevision();
    /**
     * The highest revision number included in this ChangeSet
     * @return the highest revision number of this set
     */
    long toRevision();
    
    /**
     * The read only {@link Set} of changes ContentItems included
     * in this ChangeSet.
     * @return the {@link UriRef}s of the changed contentItems
     * included in this ChangeSet
     */
    Set<UriRef>changed();
    /**
     * The reference to the {@link Store} of this {@link ChangeSet}.
     * This can be used to get the next ChangeSet by calling
     * @return
     */
}
