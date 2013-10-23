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
package org.apache.stanbol.ontologymanager.sources.owlapi;

import org.apache.stanbol.ontologymanager.servicesapi.io.AbstractGenericInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Abstract OWL API implementation of {@link OntologyInputSource} with the basic methods for obtaining root
 * ontologies and their physical IRIs where applicable.<br/>
 * </br> Implementations should either invoke abstract methods {@link #bindPhysicalOrigin(IRI)} and
 * {@link #bindRootOntology(OWLOntology)} in their constructors, or override them.
 * 
 */
public abstract class AbstractOWLOntologyInputSource extends AbstractGenericInputSource<OWLOntology> {

}
