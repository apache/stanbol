package org.apache.stanbol.enhancer.engines.metaxa.core;

import java.util.HashMap;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.impl.DiffImpl;
import org.ontoware.rdf2go.model.impl.URIGenerator;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;

/**
 * RDF2GoUtils.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */
public class RDF2GoUtils {

    public static void urifyBlankNodes(Model model) {
        HashMap<BlankNode, URI> nodeMap = new HashMap<BlankNode, URI>();
        Model add = RDF2Go.getModelFactory().createModel();
        add.open();

        Model remove = RDF2Go.getModelFactory().createModel();
        remove.open();
        for (Statement stmt : model) {
            Resource subj = stmt.getSubject();
            URI pred = stmt.getPredicate();
            Node obj = stmt.getObject();
            boolean match = false;
            if (subj instanceof BlankNode) {
                match = true;
                URI newSubj = nodeMap.get(subj);
                if (newSubj == null) {
                    newSubj = URIGenerator.createNewRandomUniqueURI();
                    nodeMap.put(subj.asBlankNode(), newSubj);
                }
                subj = newSubj;
            }
            if (obj instanceof BlankNode) {
                match = true;
                URI newObj = nodeMap.get(obj);
                if (newObj == null) {
                    newObj = URIGenerator.createNewRandomUniqueURI();
                    nodeMap.put(obj.asBlankNode(), newObj);
                }
                obj = newObj;
            }
            if (match) {
                remove.addStatement(stmt);
                add.addStatement(subj, pred, obj);
            }
        }
        ClosableIterator<Statement> addIt = add.iterator();
        ClosableIterator<Statement> removeIt = remove.iterator();
        model.update(new DiffImpl(addIt, removeIt));
        addIt.close();
        removeIt.close();
        add.close();
        remove.close();
    }

}
