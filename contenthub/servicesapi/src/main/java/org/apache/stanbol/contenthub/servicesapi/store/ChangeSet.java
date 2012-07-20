/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 *     ChangeSet cs;
 *     do {
 *         cs = store.changes(revision, batchSize);
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
