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
package org.apache.stanbol.ontologymanager.servicesapi.ontology;

import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * Exceptions regarding an ontology whose public key is known should extend this class.
 * 
 * @author alexdma
 * 
 */
public abstract class RuntimeOntologyException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 4544963016549911963L;

    protected OWLOntologyID publicKey;

    public RuntimeOntologyException(OWLOntologyID publicKey) {
        this.publicKey = publicKey;
    }

    public RuntimeOntologyException(OWLOntologyID publicKey, String message) {
        super(message);
        this.publicKey = publicKey;
    }

    public OWLOntologyID getOntologyKey() {
        return publicKey;
    }

}
