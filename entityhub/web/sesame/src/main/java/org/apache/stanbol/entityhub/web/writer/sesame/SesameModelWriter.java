package org.apache.stanbol.entityhub.web.writer.sesame;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.model.sesame.RdfRepresentation;
import org.apache.stanbol.entityhub.model.sesame.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.web.ModelWriter;
import org.apache.stanbol.entityhub.web.fieldquery.FieldQueryToJsonUtils;
import org.apache.stanbol.entityhub.yard.sesame.SesameQueryResultList;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service
public class SesameModelWriter implements ModelWriter {
    
    private final Logger log = LoggerFactory.getLogger(SesameModelWriter.class);
    
    public static final MediaType TURTLE = MediaType.valueOf("text/turtle");
    public static final MediaType JSONLD = MediaType.valueOf("application/ld+json");
    public static final MediaType N3 = MediaType.valueOf("text/rdf+n3");
    public static final MediaType N_TRIPLE = MediaType.valueOf("text/rdf+nt");
    public static final MediaType RDF_JSON = MediaType.valueOf("application/rdf+json");
    public static final MediaType RDF_XML = MediaType.valueOf("application/rdf+xml");
    public static final MediaType X_TURTLE = MediaType.valueOf("application/x-turtle");

    public static final List<MediaType> SUPPORTED_RDF_TYPES = Collections.unmodifiableList(
        Arrays.asList(TURTLE, JSONLD, N3, N_TRIPLE, RDF_JSON, RDF_XML, X_TURTLE));

    private final static RdfValueFactory valueFactory = RdfValueFactory.getInstance();
    private final static ValueFactory sesameFactory = ValueFactoryImpl.getInstance();

    private final static URI FOAF_DOCUMENT = sesameFactory.createURI(NamespaceEnum.foaf+"Document");
    private final static URI FOAF_PRIMARY_TOPIC = sesameFactory.createURI(NamespaceEnum.foaf+"primaryTopic");
    private final static URI FOAF_PRIMARY_TOPIC_OF = sesameFactory.createURI(NamespaceEnum.foaf+"isPrimaryTopicOf");
    private final static URI RDF_TYPE = sesameFactory.createURI(NamespaceEnum.rdf+"type");
    private final static URI EH_SIGN_SITE = sesameFactory.createURI(RdfResourceEnum.site.getUri());
    
    /**
     * The URI used for the query result list (static for all responses)
     */
    private static final URI QUERY_RESULT_LIST = sesameFactory.createURI(RdfResourceEnum.QueryResultSet.getUri());
    /**
     * The property used for all results
     */
    private static final URI QUERY_RESULT = sesameFactory.createURI(RdfResourceEnum.queryResult.getUri());
    /**
     * The property used for the JSON serialised FieldQuery (STANBOL-298)
     */
    private static final URI FIELD_QUERY = sesameFactory.createURI(RdfResourceEnum.query.getUri());

    
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    NamespacePrefixService nsPrefixService;
    
    @Override
    public Class<? extends Representation> getNativeType() {
        return RdfRepresentation.class;
    }

    @Override
    public List<MediaType> supportedMediaTypes() {
        return SUPPORTED_RDF_TYPES;
    }

    @Override
    public MediaType getBestMediaType(MediaType mediaType) {
        for(MediaType supported : SUPPORTED_RDF_TYPES){
            if(supported.isCompatible(mediaType)){
                return supported;
            }
        }
        return null;
    }

    @Override
    public void write(Representation rep, OutputStream out, MediaType mediaType) throws WebApplicationException,
            IOException {
        writeRdf(toRDF(rep), out, mediaType);
    }

    @Override
    public void write(Entity entity, OutputStream out, MediaType mediaType) throws WebApplicationException,
            IOException {
        writeRdf(toRDF(entity), out, mediaType);
    }

    @Override
    public void write(QueryResultList<?> result, OutputStream out, MediaType mediaType) throws WebApplicationException,
            IOException {
        Model queryRdf = toRDF(result);
        //we need also to the JSON formatted FieldQuery as a literal to the
        //RDF data.
        FieldQuery query = result.getQuery();
        if(query != null){
            try {
                JSONObject fieldQueryJson = FieldQueryToJsonUtils.toJSON(query,
                    nsPrefixService);
                if(fieldQueryJson != null){
                    //add the triple with the fieldQuery
                    queryRdf.add(QUERY_RESULT_LIST, FIELD_QUERY, 
                        sesameFactory.createLiteral(fieldQueryJson.toString()));
                }
            } catch (JSONException e) {
                log.warn(String.format("Unable to serialize Fieldquery '%s' to JSON! "
                    + "Query response will not contain the serialized query.",
                    query),e);
            }
        }
        //now serialise the data
        writeRdf(queryRdf,out,mediaType);

    }

    /**
     * @param data
     * @param out
     * @param mediaType
     */
    private void writeRdf(Model data, OutputStream out, MediaType mediaType) {
        RDFFormat rdfFormat = Rio.getWriterFormatForMIMEType(mediaType.toString());
        if(rdfFormat == null){
            throw new IllegalStateException("JAX-RS called for unsupported mediaType '"
                + mediaType +"'! If this is a valid RDF type this indicates a missing "
                + "Sesame Serializer implementation. Otherwise please report this "
                + "as a bug for the Stanbol Issue Tracker.");
        }
        try {
            Rio.write(data, out, rdfFormat);
        } catch (RDFHandlerException e) {
            throw new WebApplicationException("Unable to serialize QueryResultList with requested Format '" +
                    rdfFormat +"'!", e);
        }
    }
 
    private Model toRDF(Representation representation) {
        return valueFactory.toRdfRepresentation(representation).getModel();
    }

    private void addRDFTo(Model graph, Representation representation) {
        graph.addAll(valueFactory.toRdfRepresentation(representation).getModel());
    }

    private Model toRDF(Entity entity) {
        Model graph = new LinkedHashModel();
        addRDFTo(graph, entity);
        return graph;
    }

    private void addRDFTo(Model graph, Entity entity) {
        addRDFTo(graph, entity.getRepresentation());
        addRDFTo(graph, entity.getMetadata());
        //now add some triples that represent the Sign
        addEntityTriplesToGraph(graph, entity);
    }


    /**
     * Adds the Triples that represent the Sign to the parsed graph. Note that
     * this method does not add triples for the representation. However it adds
     * the triple (sign,singRepresentation,representation)
     *
     * @param graph the graph to add the triples
     * @param sign the sign
     */
    private void addEntityTriplesToGraph(Model graph, Entity sign) {
        URI id = sesameFactory.createURI(sign.getId());
        URI metaId = sesameFactory.createURI(sign.getMetadata().getId());
        //add the FOAF triples between metadata and content
        graph.add(id, FOAF_PRIMARY_TOPIC_OF, metaId);
        graph.add(metaId, FOAF_PRIMARY_TOPIC, metaId);
        graph.add(metaId, RDF_TYPE, FOAF_DOCUMENT);
        //add the site to the metadata
        //TODO: this should be the HTTP URI and not the id of the referenced site
        Literal siteName = sesameFactory.createLiteral(sign.getSite());
        graph.add(metaId, EH_SIGN_SITE, siteName);
        
    }

    private Model toRDF(QueryResultList<?> resultList) {
        final Model resultGraph;
        Class<?> type = resultList.getType();
        if (String.class.isAssignableFrom(type)) {
            resultGraph = new LinkedHashModel(); //create a new Graph
            for (Object result : resultList) {
                //add a triple to each reference in the result set
                resultGraph.add(QUERY_RESULT_LIST, QUERY_RESULT, sesameFactory.createURI(result.toString()));
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
            if (resultList instanceof SesameQueryResultList) {
                resultGraph = ((SesameQueryResultList) resultList).getModel();
                if (isSignType) { //if we build a ResultList for Signs, we need to do more things
                    //first remove all triples representing results
                    resultGraph.filter(null, QUERY_RESULT, null).clear();
                    //now add the Sign specific triples and add result triples
                    //to the Sign IDs
                    for (Object result : resultList) {
                        URI signId = sesameFactory.createURI(((Entity) result).getId());
                        addEntityTriplesToGraph(resultGraph, (Entity) result);
                        resultGraph.add(QUERY_RESULT_LIST, QUERY_RESULT, signId);
                    }
                }
            } else { //any other implementation of the QueryResultList interface
                resultGraph = new LinkedHashModel(); //create a new graph
                if (Representation.class.isAssignableFrom(type)) {
                    for (Object result : resultList) {
                        URI resultId;
                        if (!isSignType) {
                            addRDFTo(resultGraph, (Representation) result);
                            resultId = sesameFactory.createURI(((Representation) result).getId());
                        } else {
                            addRDFTo(resultGraph, (Entity) result);
                            resultId = sesameFactory.createURI(((Entity) result).getId());
                        }
                        //Note: In case of Representation this Triple points to
                        //      the representation. In case of Signs it points to
                        //      the sign.
                        resultGraph.add(QUERY_RESULT_LIST, QUERY_RESULT, resultId);
                    }
                }
            }
        }
        return resultGraph;
    }
}
