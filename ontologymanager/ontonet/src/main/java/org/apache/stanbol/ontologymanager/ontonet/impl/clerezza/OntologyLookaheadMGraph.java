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
package org.apache.stanbol.ontologymanager.ontonet.impl.clerezza;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.commons.owl.util.OWL2Constants;

/**
 * An utility modifiable graph that only
 * 
 * @author alexdma
 * 
 */
public class OntologyLookaheadMGraph extends SimpleMGraph {

    private UriRef ontologyIRI = null, versionIRI = null;

    @Override
    public boolean performAdd(Triple t) {
        boolean b = false;

        // filter the interesting Triples
        if (RDF.type.equals(t.getPredicate()) && OWL.Ontology.equals(t.getObject())) b = super.performAdd(t);
        else if (new UriRef(OWL2Constants.OWL_VERSION_IRI).equals(t.getPredicate())) b = super.performAdd(t);

        // check the currently available triples for the Ontology ID
        checkOntologyId();

        if (ontologyIRI != null) throw new RuntimeException(); // stop importing
        // TODO: add a limit to the triples you read

        return b;
    }

    public UriRef getOntologyIRI() {
        return ontologyIRI;
    }

    private void checkOntologyId() {
        for (Iterator<Triple> it = this.filter(null, RDF.type, OWL.Ontology); it.hasNext();) {
            NonLiteral s = it.next().getSubject();
            if (s instanceof UriRef) {
                ontologyIRI = (UriRef) s;
                break;
            }
        }
    }

}
