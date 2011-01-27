package org.apache.stanbol.enhancer.clerezza.sparql;

import java.util.ArrayList;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.clerezza.rdf.utils.UnionMGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.SparqlQueryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** SPARQL query engine using Clerezza components
 *
 * @deprecated use the TcManager / Store API directly instead
 */
@Deprecated
@Component
@Service(SparqlQueryEngine.class)
public class ClerezzaSparqlQueryEngine implements SparqlQueryEngine {
    Logger log = LoggerFactory.getLogger(ClerezzaSparqlQueryEngine.class);

    @Reference
    TcManager tcManager;

    public ResultSet executeQuery(String sparqlQuery)
            throws SparqlQueryEngineException {
        // This code was copied from EnhancerHandler - refactored to make
        // it reusable
        log.debug("execute query");
        long start = System.currentTimeMillis();
        try {
            // TODO QueryParser should be a @reference??
            SelectQuery query = (SelectQuery) QueryParser.getInstance().parse(
                    sparqlQuery);
            Set<UriRef> graphUris = tcManager.listTripleCollections();
            ArrayList<TripleCollection> tripleCollections = new ArrayList<TripleCollection>();
            for (UriRef uriRef : graphUris) {
                try {
                    tripleCollections.add(tcManager.getTriples(uriRef));
                } catch (NoSuchEntityException ex) {
                    continue;
                }
            }
            if (tripleCollections.isEmpty()) {
                return EmptyResultSet.getInstance();
            }
            MGraph unionGraph = new UnionMGraph(
                    tripleCollections.toArray(new TripleCollection[tripleCollections.size()]));
            long preperation = System.currentTimeMillis();
            ResultSet resultSet = tcManager.executeSparqlQuery(query,
                    unionGraph);
            long done = System.currentTimeMillis();
            // RW: added this logging to check if building the unionGraph is not
            // a performance hook!
            log.info("Querytime " + (done - start) + "ms (preperation="
                    + (preperation - start) + "|query=" + query + ")");
            return resultSet;
        } catch (Exception e) {
            throw new SparqlQueryEngineException("Exception processing query ["
                    + sparqlQuery + "]", e);
        }
    }

    public static class EmptyResultSet implements ResultSet {

        private static EmptyResultSet instance = new EmptyResultSet();

        public void remove() {
        }

        public static EmptyResultSet getInstance() {
            return instance;
        }

        public SolutionMapping next() {
            return null;
        }

        public boolean hasNext() {
            return false;
        }
    }
}
