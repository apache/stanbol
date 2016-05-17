
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
import org.ontoware.rdf2go.model.node.RDFTerm;
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
            RDFTerm subj = stmt.getSubject();
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
