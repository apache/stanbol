package org.apache.stanbol.enhancer.engines.htmlextractor.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities functions for RDF Graphs
 * 
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 * 
 */

public class ClerezzaRDFUtils {
    
    private static final Logger LOG = LoggerFactory.getLogger(ClerezzaRDFUtils.class);
    
    public static void urifyBlankNodes(MGraph model) {
        HashMap<BNode,UriRef> blankNodeMap = new HashMap<BNode,UriRef>();
        MGraph remove = new SimpleMGraph();
        MGraph add = new SimpleMGraph();
        for (Triple t: model) {
            NonLiteral subj = t.getSubject();
            Resource obj = t.getObject();
            UriRef pred = t.getPredicate();
            boolean match = false;
            if (subj instanceof BNode) {
                match = true;
                UriRef ru = blankNodeMap.get(subj);
                if (ru == null) {
                    ru = createRandomUri();
                    blankNodeMap.put((BNode)subj, ru);
                }
                subj = ru;
            }
            if (obj instanceof BNode)  {
                match = true;
                UriRef ru = blankNodeMap.get(obj);
                if (ru == null) {
                    ru = createRandomUri();
                    blankNodeMap.put((BNode)obj, ru);
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
    
    public static UriRef createRandomUri() {
        return new UriRef("urn:rnd:"+randomUUID());
    }
    
    public static void makeConnected(MGraph model, NonLiteral root, UriRef property) {
        Set<NonLiteral> roots = findRoots(model);
        LOG.debug("Roots: {}",roots.size());
        boolean found = roots.remove(root);
        //connect all hanging roots to root by property
        for (NonLiteral n: roots) {
            model.add(new TripleImpl(root,property,n));            
        }
    }
    
    public static Set<NonLiteral> findRoots(MGraph model) {
        Set<NonLiteral> roots = new HashSet<NonLiteral>();
        Set<NonLiteral> visited = new HashSet<NonLiteral>();
        for (Triple t: model) {
            NonLiteral subj = t.getSubject();
            findRoot(model, subj, roots, visited);
        }
        return roots;
    }
    
    private static void findRoot(MGraph model, NonLiteral node, Set<NonLiteral> roots, Set<NonLiteral> visited) {
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
            NonLiteral subj = t.getSubject();
            findRoot(model, subj, roots, visited);
        }
    }


}
