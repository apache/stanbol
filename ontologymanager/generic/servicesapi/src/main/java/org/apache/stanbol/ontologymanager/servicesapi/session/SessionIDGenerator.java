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
package org.apache.stanbol.ontologymanager.servicesapi.session;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;

/**
 * Implementations of this interface provide algorithms for generating valid
 * identifiers for KReS sessions. These algorithms should take into account the
 * need for excluding existing session IDs.
 * 
 * @author alexdma
 * 
 */
public interface SessionIDGenerator {

	/**
	 * Generates a new context-free session ID. Whether this causes duplicate
	 * IDs, it should be care of the object that invoked this method to check
	 * it.
	 * 
	 * @return the newly generated session ID.
	 */
    String createSessionID();

	/**
	 * Generates a new session ID that is different from any IRI in the
	 * <code>exclude</code> set. Whether this causes duplicate IDs (supposing
	 * the <code>exclude</code> set does not include all of them), it should be
	 * care of the object that invoked this method to check it.
	 * 
	 * @param exclude
	 *            the set of IRIs none of which the generate ID must be equal
	 *            to.
	 * @return the newly generated session ID.
	 */
    String createSessionID(Set<String> exclude);

	/**
	 * Returns the base IRI for all generated IDs to start with. It should be
	 * used by all <code>createSessionID()</code> methods, or ignore if null.
	 * 
	 * @param baseIRI
	 *            the base IRI.
	 */
    IRI getBaseIRI();

	/**
	 * Sets the base IRI for all generated IDs to start with. It should be used
	 * by all <code>createSessionID()</code> methods, or ignore if null.
	 * 
	 * @param baseIRI
	 *            the base IRI.
	 */
    void setBaseIRI(IRI baseIRI);
}
