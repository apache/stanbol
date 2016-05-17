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
package org.apache.stanbol.enhancer.engines.htmlextractor.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities functions for RDF Graphs
 * 
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 * 
 */

public final class ClerezzaRDFUtils {

    /**
     * Restrict instantiation
     */
    private ClerezzaRDFUtils() {}

    private static final Logger LOG = LoggerFactory.getLogger(ClerezzaRDFUtils.class);
    
    public static void urifyBlankNodes(Graph model) {
        HashMap<BlankNode,IRI> blankNodeMap = new HashMap<BlankNode,IRI>();
        Graph remove = new SimpleGraph();
        Graph add = new SimpleGraph();
        for (Triple t: model) {
            BlankNodeOrIRI subj = t.getSubject();
            RDFTerm obj = t.getObject();
            IRI pred = t.getPredicate();
            boolean match = false;
            if (subj instanceof BlankNode) {
                match = true;
                IRI ru = blankNodeMap.get(subj);
                if (ru == null) {
                    ru = createRandomUri();
                    blankNodeMap.put((BlankNode)subj, ru);
                }
                subj = ru;
            }
            if (obj instanceof BlankNode)  {
                match = true;
                IRI ru = blankNodeMap.get(obj);
                if (ru == null) {
                    ru = createRandomUri();
                    blankNodeMap.put((BlankNode)obj, ru);
                }
                obj = ru;
            }
            if (match) {
                remove.add(t);
                add.add(new TripleImpl(subj,pred,obj));
            }
        }
        model.removeAll(remove);
        model.addAll(add);
    }
    
    public static IRI createRandomUri() {
        return new IRI("urn:rnd:"+randomUUID());
    }
    
    public static void makeConnected(Graph model, BlankNodeOrIRI root, IRI property) {
        Set<BlankNodeOrIRI> roots = findRoots(model);
        LOG.debug("Roots: {}",roots.size());
        boolean found = roots.remove(root);
        //connect all hanging roots to root by property
        for (BlankNodeOrIRI n: roots) {
            model.add(new TripleImpl(root,property,n));            
        }
    }
    
    public static Set<BlankNodeOrIRI> findRoots(Graph model) {
        Set<BlankNodeOrIRI> roots = new HashSet<BlankNodeOrIRI>();
        Set<BlankNodeOrIRI> visited = new HashSet<BlankNodeOrIRI>();
        for (Triple t: model) {
            BlankNodeOrIRI subj = t.getSubject();
            findRoot(model, subj, roots, visited);
        }
        return roots;
    }
    
    private static void findRoot(Graph model, BlankNodeOrIRI node, Set<BlankNodeOrIRI> roots, Set<BlankNodeOrIRI> visited) {
        if (visited.contains(node)) {
            return;
        }
        visited.add(node);
        Iterator<Triple> it = model.filter(null,null,node);
        // something that is not the object of some stement is a root
        if (!it.hasNext()) {
            roots.add(node);
            LOG.debug("Root found: {}",node);
            return;
        }
        while (it.hasNext()) {
            Triple t = it.next();
            BlankNodeOrIRI subj = t.getSubject();
            findRoot(model, subj, roots, visited);
        }
    }


}
