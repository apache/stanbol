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
package org.apache.stanbol.owl;

import java.net.URISyntaxException;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

public class PhonyIRIMapper implements OWLOntologyIRIMapper {

    private Set<IRI> notMapped;

    private IRI blankIri = null;

    private String blankResourcePath = "/ontologies/blank.owl";

    public PhonyIRIMapper(Set<IRI> notMapped) {
        this.notMapped = notMapped;
        try {
            blankIri = IRI.create(this.getClass().getResource(blankResourcePath));
        } catch (URISyntaxException e) {
            // How can it happen?
        }
    }

    @Override
    public IRI getDocumentIRI(IRI arg0) {
        if (notMapped.contains(arg0)) return null;
        else return blankIri;
    }

}
