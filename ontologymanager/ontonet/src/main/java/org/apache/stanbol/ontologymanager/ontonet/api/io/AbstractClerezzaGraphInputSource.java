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
package org.apache.stanbol.ontologymanager.ontonet.api.io;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.stanbol.owl.util.OWLUtils;

/**
 * Default implementation of an {@link OntologyInputSource} that returns {@link Graph} objects as ontologies.
 * 
 * Subclasses must implement the {@link #getImports(boolean)} method, as the availability of imported
 * ontologies might depend on the input source being able to access the {@link TcManager} where they are
 * stored.
 * 
 * @author alexdma
 * 
 */
public abstract class AbstractClerezzaGraphInputSource extends AbstractGenericInputSource<Graph> {

    protected UriRef ontologyId = null;

    @Override
    protected void bindRootOntology(Graph ontology) {
        super.bindRootOntology(ontology);
        this.ontologyId = OWLUtils.guessOntologyIdentifier(ontology);
    }

    @Override
    public Set<Graph> getImports(boolean recursive) {
        return getImportedGraphs(rootOntology, recursive);
    }

    protected Set<Graph> getImportedGraphs(TripleCollection g, boolean recursive) {
        Set<Graph> result = new HashSet<Graph>();
        Iterator<Triple> it = g.filter(OWLUtils.guessOntologyIdentifier((Graph) g), OWL.imports, null);
        while (it.hasNext()) {
            Resource r = it.next().getObject();
            if (r instanceof UriRef) {
                TripleCollection gr = TcManager.getInstance().getTriples((UriRef) r);
                if (gr instanceof Graph) result.add((Graph) gr);
                else if (gr instanceof MGraph) result.add(((MGraph) gr).getGraph());
                if (recursive) result.addAll(getImportedGraphs(gr, true));
            }
        }
        return result;
    }

}
