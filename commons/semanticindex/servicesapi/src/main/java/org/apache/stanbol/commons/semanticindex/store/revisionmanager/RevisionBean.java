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
package org.apache.stanbol.commons.semanticindex.store.revisionmanager;

/**
 * A tiny bean holding the changes obtained from a {@link RevisionManager}. This it keeps the uri of the
 * changed item and corresponding new revision.
 * 
 * @author suat
 * 
 */
public class RevisionBean {
    private String id;

    private long revision;

    public RevisionBean(String id, long revision) {
        this.id = id;
        this.revision = revision;
    }

    public String getID() {
        return this.id;
    }

    public long getRevision() {
        return this.revision;
    }
}
