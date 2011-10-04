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

package org.apache.stanbol.contenthub.core.utils.indexing;

import java.util.ArrayList;
import java.util.List;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.jena.commons.Tria2JenaUtil;
import org.apache.clerezza.rdf.jena.facade.JenaGraph;
import org.apache.stanbol.contenthub.core.utils.sparql.QueryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.larq.IndexBuilderString;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * 
 * @author anil.sinaci
 * 
 */
public class EnhancementLARQ {

    private static final Logger logger = LoggerFactory.getLogger(EnhancementLARQ.class);

    private static EnhancementLARQ instance;

    private Model enhancementModel;
    private IndexBuilderString enhancementGraphIndexBuilder;

    private EnhancementLARQ() {
        enhancementModel = null;
        enhancementGraphIndexBuilder = null;
    }

    public static EnhancementLARQ getInstance() {
        if (instance == null) {
            instance = new EnhancementLARQ();
        }
        return instance;
    }

    public void createIndex(MGraph mgraph) {
        // FIXME: To index the enhancement graph, we need a Jena graph, however tcmanager
        // is directly used in here. We need to use a common interface to eliminate this burden
        // of converting one graph to other.
        JenaGraph jenaGraph = new JenaGraph(mgraph);
        enhancementModel = ModelFactory.createModelForGraph(jenaGraph);

        // Build the LARQ index on the enhancement graph
        // TODO We may build the index on a specific property
        enhancementGraphIndexBuilder = new IndexBuilderString();
        enhancementGraphIndexBuilder.indexStatements(enhancementModel.listStatements());
        enhancementModel.register(enhancementGraphIndexBuilder);
        logger.debug("EnhancementLARQ has indexed existing enhancement graph and started to listen");
    }

    public void updateIndex(Triple triple) {
        Tria2JenaUtil jena2TriaUtil = new Tria2JenaUtil(null);
        Resource subject = enhancementModel.createResource(jena2TriaUtil
                .convert2JenaNode(triple.getSubject()).getURI());
        Property predicate = enhancementModel.createProperty(jena2TriaUtil.convert2JenaNode(
            triple.getPredicate()).getURI());
        RDFNode object = enhancementModel.getRDFNode(jena2TriaUtil.convert2JenaNode(triple.getObject()));
        Statement s = enhancementModel.createStatement(subject, predicate, object);
        enhancementModel.add(s);
    }

    public Model getEnhancementModel() {
        if (enhancementModel == null) {
            logger.error("Enhancement Model in EnhancementLARQ is null. Index has not been created yet.");
            return null;
        }
        return enhancementModel;
    }

    public IndexLARQ getIndex() {
        if (enhancementGraphIndexBuilder == null) {
            logger.error("enhancementGraphIndexBuilder in EnhancementLARQ is null. Index has not been created yet.");
            return null;
        }
        return enhancementGraphIndexBuilder.getIndex();
    }

    public void deleteEnhancements(String contentID) {
        enhancementModel.unregister(enhancementGraphIndexBuilder);
        Query query = QueryFactory.create(QueryGenerator.getEnhancementsOfContent(contentID));
        QueryExecution qexec = QueryExecutionFactory.create(query, enhancementModel);
        List<String> enhancementsToBeRemoved = new ArrayList<String>();
        try {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                enhancementsToBeRemoved.add(soln.getResource("enhancement").getURI());
            }
        } finally {
            qexec.close();
        }
        for(String enhancementURI : enhancementsToBeRemoved) {
            StmtIterator it = enhancementModel.listStatements(ResourceFactory.createResource(enhancementURI), (Property) null, (RDFNode) null);
            if(it == null) continue;
            enhancementModel.remove(it);
        }
        enhancementGraphIndexBuilder = new IndexBuilderString();
        enhancementGraphIndexBuilder.indexStatements(enhancementModel.listStatements());
        enhancementModel.register(enhancementGraphIndexBuilder);
    }

    public void printGraph() {
        enhancementModel.write(System.out, "RDF/XML");
    }
    
    
}
