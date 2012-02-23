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
package org.apache.stanbol.ontologymanager.registry.api;

import java.util.Set;

import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * A factory that creates the basic elements of the ontology registry metamodel from OWL objects that are
 * required not to be anonymous.<br/>
 * <br/>
 * <b>Note that implementations should not be aggressive</b>, in that they should <b>not</b> recursively
 * create and/or append the parents and children of any generated object. Refer to
 * {@link RegistryManager#createModel(Set)} to recursively populate a registry item starting from a set of
 * registries.
 * 
 * @author alexdma
 */
public interface RegistryItemFactory {

    /**
     * Creates a new {@link Library} object named after the ID of the supplied individual.
     * 
     * @param ind
     *            the named individual to extract the library model from.
     * @return the library model.
     */
    Library createLibrary(OWLNamedObject ind);

    /**
     * Creates a new {@link Registry} object named after the ID of the supplied ontology.
     * 
     * @param o
     *            the ontology to extract the registry model from. Should be a named ontology, lest the method
     *            return null.
     * @return the registry model, or null if <code>o</code> is anonymous.
     */
    Registry createRegistry(OWLOntology o);

    /**
     * Creates a new {@link RegistryOntology} object named after the ID of the supplied individual.
     * 
     * @param ind
     *            the named individual to extract the ontology model from.
     * @return the ontology model.
     */
    RegistryOntology createRegistryOntology(OWLNamedObject ind);

}
