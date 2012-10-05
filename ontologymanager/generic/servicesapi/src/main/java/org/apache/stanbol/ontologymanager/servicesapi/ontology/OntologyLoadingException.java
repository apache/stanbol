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

/**
 * A runtime exception denoting that trying to load an ontology into the Ontology Manager has caused an
 * undesired status. The reason is most likely to be found in the cause registered with this exception.
 * 
 * @author alexdma
 * 
 */
public class OntologyLoadingException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -8496827319814210715L;

    /**
     * Creates a new instance of {@link OntologyLoadingException}.
     * 
     * @param cause
     *            the throwable that caused this exception to be thrown.
     */
    public OntologyLoadingException(Throwable cause) {
        initCause(cause);
    }

    /**
     * Creates a new instance of {@link OntologyLoadingException}.
     * 
     * @param message
     *            the exception message.
     */
    public OntologyLoadingException(String message) {
        super(message);
    }

}
