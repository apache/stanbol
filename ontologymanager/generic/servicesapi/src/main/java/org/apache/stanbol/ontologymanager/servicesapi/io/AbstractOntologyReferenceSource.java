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
package org.apache.stanbol.ontologymanager.servicesapi.io;

/**
 * Input sources that do not deliver an ontology object, but only a means for consumers to obtain one if they
 * wish, should subclass this one.
 * 
 * These input sources should be used whenever it is possible to avoid creating an ontology object, thereby
 * saving resources. Examples include cases where the ontology is already stored in Stanbol, or whenever
 * loading has to be deferred.
 * 
 * @author alexdma
 * 
 * @param <O>
 *            the ontology object
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractOntologyReferenceSource extends AbstractGenericInputSource {

    public AbstractOntologyReferenceSource(Origin<?> origin) {
        this.origin = origin;
        this.rootOntology = null;
    }

}
