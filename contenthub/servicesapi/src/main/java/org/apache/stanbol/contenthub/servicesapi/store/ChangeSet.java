package org.apache.stanbol.contenthub.servicesapi.store;

import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * This interface represents a set of {@link #size()} changes starting {@link #fromRevision()}
 * {@link #toRevision()} affecting ContentItems with the {@link UriRef}s returned by {@link #changed()}.
 * Instead of getting all changes as a whole, they can be retrieved iteratively through the {@link Store}
 * instance.
 * <p>
 * The intended usage of this class is <code><pre>
 *     Store store; //the store
 *     SemanticIndex index; //the index to apply the changes
 *     long revision = Long.MIN_VALUE; //start from scratch
 *     int batchSize = 1000;
 *     int offset = 0;
 *     ChangeSet cs;
 *     do {
 *         cs = store.changes(revision, offset, batchSize);
 *         for(UriRef changed : cs.changed()){
 *             ContentItem ci = store.get(changed);
 *             if(ci == null){
 *                 index.remove(changed);
 *             } else {
 *                 index.index(ci);
 *             }
 *         }
 *         offset+=cs.changed().size(); 
 *     while(!cs.changed().isEmpty());
 *     index.persist(cs.fromRevision());
 * </pre></code>
 */
public interface ChangeSet {
    /**
     * The lowest revision number included in this ChangeSet
     * 
     * @return the lowest revision number of this set
     */
    long fromRevision();

    /**
     * The highest revision number included in this ChangeSet
     * 
     * @return the highest revision number of this set
     */
    long toRevision();

    /**
     * The read only {@link Set} of changes ContentItems included in this ChangeSet.
     * 
     * @return the {@link UriRef}s of the changed contentItems included in this ChangeSet
     */
    Set<UriRef> changed();

    /**
     * The reference to the {@link Store} of this {@link ChangeSet}. This Store can be used to iterate on the
     * changes.
     * 
     * @return
     */
    Store getStore();
}
