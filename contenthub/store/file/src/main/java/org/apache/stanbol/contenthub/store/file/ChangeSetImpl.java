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
package org.apache.stanbol.contenthub.store.file;

import java.util.Set;

import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;

public class ChangeSetImpl<Item> implements ChangeSet<Item> {
    private long from;
    private long to;
    private Set<String> changedUris;
    private Store<Item> store;

    @Override
    public long fromRevision() {
        return from;
    }

    @Override
    public long toRevision() {
        return to;
    }

    @Override
    public Set<String> changed() {
        return changedUris;
    }

    @Override
    public Store<Item> getStore() {
        return store;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public void setChangedUris(Set<String> changedUris) {
        this.changedUris = changedUris;
    }

    public void setStore(Store<Item> store) {
        this.store = store;
    }
}
