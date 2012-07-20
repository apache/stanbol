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
package org.apache.stanbol.contenthub.servicesapi.index;

/**
 * This enumeration defines the possible states for a {@link SemanticIndex}.
 */
public enum IndexState {
    /**
     * The index was defined, the configuration is ok, but the contents are not yet indexed and the indexing
     * has not yet started. (Intended to be used as default state after creations)
     */
    UNINIT,
    /**
     * The indexing of content items is currently in progress. This indicates that the index is currently NOT
     * active.
     */
    INDEXING,
    /**
     * The semantic index is available and in sync
     */
    ACTIVE,
    /**
     * The (re)-indexing of content times is currently in progress. This indicates that the configuration of
     * the semantic index was changed in a way that requires to rebuild the whole semantic index. This still
     * requires the index to be active - meaning the searches can be performed normally - but recent
     * updates/changes to ContentItems might not be reflected. This also indicates that the index will be
     * replaced by a different version (maybe with changed fields) in the near future.
     */
    REINDEXING;

    private static final String prefix = "http://stanbol.apache.org/ontology/contenthub#indexState_";

    public String getUri() {
        return prefix + name().toLowerCase();
    }

    @Override
    public String toString() {
        return getUri();
    }
}
