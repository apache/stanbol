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
package org.apache.stanbol.commons.owl;

import java.net.URISyntaxException;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

/**
 * An ontology IRI mapper that can be used to trick an OWLOntologyManager into believing all imports are
 * loaded except for those indicated in the exclusions set. It can be used when imported ontologies have to be
 * provided in other ways than by dereferencing URLs, for example when we want to load the same ontology from
 * a triple store programmatically.
 * 
 * @author alexdma
 * 
 */
public class PhonyIRIMapper implements OWLOntologyIRIMapper {

    private Set<IRI> exclusions;

    private IRI blankIri = null;

    private String blankResourcePath = "/ontologies/blank.owl";

    /**
     * 
     * @param notMapped
     *            the set of IRIs that will not be mapped by this object, so that the ontology manager will
     *            only try to load from these IRIs, unless another attached IRI mapper specifies otherwise.
     */
    public PhonyIRIMapper(Set<IRI> exclusions) {
        this.exclusions = exclusions;
        try {
            blankIri = IRI.create(this.getClass().getResource(blankResourcePath));
        } catch (URISyntaxException e) {
            // How can it happen?
        }
    }

    @Override
    public IRI getDocumentIRI(IRI arg0) {
        if (exclusions==null) return blankIri;
        if (exclusions.contains(arg0)) return null;
        else return blankIri;
    }

}
