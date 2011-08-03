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
package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

/**
 * Thrown whenever an illegal operation that modifies an ontology space is detected and denied.
 */
public class OntologySpaceModificationException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = -5147080356192253724L;

    protected OntologySpace space;

    /**
     * Creates a new instance of OntologySpaceModificationException.
     * 
     * @param space
     *            the ontology space whose modification was attempted.
     */
    public OntologySpaceModificationException(OntologySpace space) {
        this.space = space;
    }

    /**
     * Creates a new instance of OntologySpaceModificationException.
     * 
     * @param space
     *            the ontology space whose modification was attempted.
     */
    public OntologySpaceModificationException(OntologySpace space, Throwable cause) {
        this(space);
        initCause(cause);
    }

    /**
     * Returns the ontology space that threw the exception (presumably after a failed modification attempt).
     * 
     * @return the ontology space on which the exception was thrown.
     */
    public OntologySpace getSpace() {
        return space;
    }

}
