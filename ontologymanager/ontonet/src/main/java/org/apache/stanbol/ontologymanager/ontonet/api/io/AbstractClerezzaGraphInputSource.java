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
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.stanbol.commons.owl.util.OWLUtils;

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
public abstract class AbstractClerezzaGraphInputSource extends
        AbstractGenericInputSource<TripleCollection,TcProvider> {

    @Override
    protected void bindRootOntology(TripleCollection ontology) {
        super.bindRootOntology(ontology);
    }

    @Override
    public Set<TripleCollection> getImports(boolean recursive) {
        return getImportedGraphs(rootOntology, recursive);
    }

    protected Set<TripleCollection> getImportedGraphs(TripleCollection g, boolean recursive) {
        Set<TripleCollection> result = new HashSet<TripleCollection>();
        UriRef u = OWLUtils.guessOntologyIdentifier(g);
        Iterator<Triple> it = g.filter(u, OWL.imports, null);
        while (it.hasNext()) {
            Resource r = it.next().getObject();
            if (r instanceof UriRef) {
                TripleCollection gr = TcManager.getInstance().getTriples((UriRef) r);
                // Avoid calls to getGraph() to save memory
                // if (gr instanceof Graph)
                result.add(/* (Graph) */gr);
                // else if (gr instanceof MGraph) result.add(((MGraph) gr).getGraph());
                if (recursive) result.addAll(getImportedGraphs(gr, true));
            }
        }
        return result;
    }

}
