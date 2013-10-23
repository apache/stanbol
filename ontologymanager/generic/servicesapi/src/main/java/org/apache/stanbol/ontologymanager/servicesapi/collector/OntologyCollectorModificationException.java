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
package org.apache.stanbol.ontologymanager.servicesapi.collector;

/**
 * Thrown whenever an illegal operation that modifies an ontology collector is detected and denied.
 */
public abstract class OntologyCollectorModificationException extends RuntimeException {

    /**
	 * 
	 */
    private static final long serialVersionUID = -5147080356192253724L;

    /**
     * The affected ontology collector.
     */
    protected OntologyCollector collector;

    /**
     * Creates a new instance of OntologySpaceModificationException.
     * 
     * @param space
     *            the ontology space whose modification was attempted.
     */
    public OntologyCollectorModificationException(OntologyCollector collector) {
        this.collector = collector;
    }

    /**
     * Creates a new instance of OntologySpaceModificationException.
     * 
     * @param space
     *            the ontology space whose modification was attempted.
     */
    public OntologyCollectorModificationException(OntologyCollector collector, Throwable cause) {
        this(collector);
        initCause(cause);
    }

    /**
     * Returns the ontology space that threw the exception (presumably after a failed modification attempt).
     * 
     * @return the ontology space on which the exception was thrown.
     */
    public OntologyCollector getOntologyCollector() {
        return collector;
    }

}
