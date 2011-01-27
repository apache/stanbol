package org.apache.stanbol.enhancer.servicesapi;

import org.apache.clerezza.rdf.core.sparql.ResultSet;

/** SPARQL query engine interface.
 *
 *     TODO we might want to use a more flexible interface where the
 *     query language can be specified, and which can return different
 *     object types.
 */
public interface SparqlQueryEngine {

    class SparqlQueryEngineException extends Exception {
        private static final long serialVersionUID = 1L;

        public SparqlQueryEngineException(String reason, Throwable cause) {
            super(reason, cause);
        }

        public SparqlQueryEngineException(String reason) {
            super(reason);
        }
    }

    ResultSet executeQuery(String sparqlQuery) throws SparqlQueryEngineException;

}
