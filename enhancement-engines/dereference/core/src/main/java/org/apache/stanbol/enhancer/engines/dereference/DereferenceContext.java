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
package org.apache.stanbol.enhancer.engines.dereference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;

public class DereferenceContext {

    /**
     * The {@link OfflineMode} status
     */
    protected final boolean offlineMode;
    /** 
     * Read-only set with languages that need to be dereferenced.
     */
    private Set<String> languages = new HashSet<String>();
    
    /**
     * Create a new DereferenceContext.
     * @param offlineMode the {@link OfflineMode} state
     */
    protected DereferenceContext(boolean offlineMode){
        this.offlineMode = offlineMode;
    }

    /**
     * If the {@link OfflineMode} is active
     * @return the offline mode status
     */
    public boolean isOfflineMode() {
        return offlineMode;
    }
    /**
     * Setter for the languages of literals that should be dereferenced
     * @param languages the ContentLanguages
     */
    protected void setLanguages(Set<String> languages) {
        if(languages == null){
            this.languages = Collections.emptySet();
        } else {
            this.languages = Collections.unmodifiableSet(languages);
        }
    }
    /**
     * Getter for the languages that should be dereferenced. If 
     * empty all languages should be included.
     * @return the languages for literals that should be dereferenced.
     */
    public Set<String> getLanguages() {
        return languages;
    }
}
