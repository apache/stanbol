/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.enhancer.clerezza;


import java.util.ArrayList;
import java.util.Set;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.clerezza.rdf.utils.UnionMGraph;

/**
 *
 * @author mir
 */
@Component
@Service(value = Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/enhancer")
public class EnhancerHandler {

    @Reference
    Store store;

    @Reference
    TcManager tcManager;

    @GET
    public MGraph getMetadata(@QueryParam("uri") UriRef uri) {
        return store.get(uri.getUnicodeString()).getMetadata();
    }

    @POST
    public ResultSet sparql(@FormParam(value="query") String sqarqlQuery) throws ParseException {
        SelectQuery query = (SelectQuery)QueryParser.getInstance().parse(sqarqlQuery);
        Set<UriRef> graphUris = tcManager.listTripleCollections();
        ArrayList<TripleCollection> tripleCollections = new ArrayList<TripleCollection>();
        for (UriRef uriRef : graphUris) {
            try {
                tripleCollections.add(tcManager.getTriples(uriRef));
            } catch (NoSuchEntityException ex) {
                continue;
            }
        }
        MGraph unionGraph = new UnionMGraph(tripleCollections.toArray(new TripleCollection[0]));
        ResultSet resultSet = tcManager.executeSparqlQuery(query, unionGraph);
        return resultSet;
    }

}
