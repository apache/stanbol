package org.apache.stanbol.entityhub.jersey.writers;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.entityhub.query.clerezza.RdfQueryResultList;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;


final class QueryResultsToRDF {

    private QueryResultsToRDF() { /* do not create instances of utility classes */}

    static final UriRef queryResultList = new UriRef(RdfResourceEnum.QueryResultSet.getUri());
    static final UriRef queryResult = new UriRef(RdfResourceEnum.queryResult.getUri());

    static MGraph toRDF(QueryResultList<?> resultList) {
        final MGraph resultGraph;
        Class<?> type = resultList.getType();
        if (String.class.isAssignableFrom(type)) {
            resultGraph = new SimpleMGraph(); //create a new Graph
            for (Object result : resultList) {
                //add a triple to each reference in the result set
                resultGraph.add(new TripleImpl(queryResultList, queryResult, new UriRef(result.toString())));
            }
        } else {
            //first determine the type of the resultList
            final boolean isSignType;
            if (Representation.class.isAssignableFrom(type)) {
                isSignType = false;
            } else if (Representation.class.isAssignableFrom(type)) {
                isSignType = true;
            } else {
                //incompatible type -> throw an Exception
                throw new IllegalArgumentException("Parsed type " + type + " is not supported");
            }
            //special treatment for RdfQueryResultList for increased performance
            if (resultList instanceof RdfQueryResultList) {
                resultGraph = ((RdfQueryResultList) resultList).getResultGraph();
                if (isSignType) { //if we build a ResultList for Signs, that we need to do more things
                    //first remove all triples representing results
                    Iterator<Triple> resultTripleIt = resultGraph.filter(queryResultList, queryResult, null);
                    while (resultTripleIt.hasNext()) {
                        resultTripleIt.next();
                        resultTripleIt.remove();
                    }
                    //now add the Sign specific triples and add result triples
                    //to the Sign IDs
                    for (Object result : resultList) {
                        UriRef signId = new UriRef(((Sign) result).getId());
                        SignToRDF.addSignTriplesToGraph(resultGraph, (Sign) result);
                        resultGraph.add(new TripleImpl(queryResultList, queryResult, signId));
                    }
                }
            } else { //any other implementation of the QueryResultList interface
                resultGraph = new SimpleMGraph(); //create a new graph
                if (Representation.class.isAssignableFrom(type)) {
                    for (Object result : resultList) {
                        UriRef resultId;
                        if (!isSignType) {
                            SignToRDF.addRDFTo(resultGraph, (Representation) result);
                            resultId = new UriRef(((Representation) result).getId());
                        } else {
                            SignToRDF.addRDFTo(resultGraph, (Sign) result);
                            resultId = new UriRef(((Sign) result).getId());
                        }
                        //Note: In case of Representation this Triple points to
                        //      the representation. In case of Signs it points to
                        //      the sign.
                        resultGraph.add(new TripleImpl(queryResultList, queryResult, resultId));
                    }
                }
            }
        }
        return resultGraph;
    }

}
