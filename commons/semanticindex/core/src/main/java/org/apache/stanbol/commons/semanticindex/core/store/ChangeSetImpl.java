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
package org.apache.stanbol.commons.semanticindex.core.store;

import java.util.Collections;
import java.util.Iterator;

import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;

public class ChangeSetImpl<Item> implements ChangeSet<Item> {
    private final long from;
    private final long to;
    private final long epoch;
    private final Iterable<String> changedUris;
    private final IndexingSource<Item> source;

    public ChangeSetImpl(IndexingSource<Item> source,long epoch,long from, long to, Iterable<String> changed) {
    	if(source == null){
    		throw new IllegalArgumentException("The parsed IndexingSource MUST NOT be NULL!");
    	}
    	if(from > to){
    		throw new IllegalArgumentException("The pared from revision MUST NOT be bigger as the to revision!");
    	}
    	if(changed == null){
    		if(to != from){
    			throw new IllegalArgumentException("For empty ChangeSets from and to revisions MUST BE the same!");
    		}
        	this.changedUris = Collections.emptyList();
    	} else {
    		this.changedUris = changed;
    	}
    	this.epoch = epoch;
    	this.from = from;
    	this.to = to;
    	this.source = source;
	}
    
    @Override
    public long fromRevision() {
        return from;
    }

    @Override
    public long toRevision() {
        return to;
    }

    @Override
    public IndexingSource<Item> getIndexingSource() {
        return source;
    }

	@Override
	public long getEpoch() {
		return epoch;
	}

	@Override
	public Iterator<String> iterator() {
		return changedUris.iterator();
	}
}
