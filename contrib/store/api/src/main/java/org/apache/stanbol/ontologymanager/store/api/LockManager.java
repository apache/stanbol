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
package org.apache.stanbol.ontologymanager.store.api;

/**
 * Single writer, multiple readers lock implementation for persistence store
 * 
 * @author Cihan
 */
public interface LockManager {

    String GLOBAL_SPACE = "GLOBAL_SPACE";

    /**
     * Obtain a read lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    void obtainReadLockFor(String ontologyURI);

    /**
     * Release read lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    void releaseReadLockFor(String ontologyURI);

    /**
     * Obtain a write lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    void obtainWriteLockFor(String ontologyURI);

    /**
     * Release write lock for specified ontology
     * 
     * @param ontologyURI
     */
    void releaseWriteLockFor(String ontologyURI);

}