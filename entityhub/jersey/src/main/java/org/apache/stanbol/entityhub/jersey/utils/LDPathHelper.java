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
package org.apache.stanbol.entityhub.jersey.utils;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.getAcceptableMediaType;
import static org.apache.stanbol.entityhub.ldpath.LDPathUtils.getReader;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.fields.FieldMapping;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.marmotta.ldpath.model.selectors.PropertySelector;
import org.apache.marmotta.ldpath.model.transformers.DoubleTransformer;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.jersey.resource.EntityhubRootResource;
import org.apache.stanbol.entityhub.jersey.resource.ReferencedSiteRootResource;
import org.apache.stanbol.entityhub.jersey.resource.SiteManagerRootResource;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.AbstractBackend;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LDPathHelper {

    /**
     * Restrict instantiation
     */
    private LDPathHelper() {}

    private static final Logger log = LoggerFactory.getLogger(LDPathHelper.class);
    
    /**
     * LDPath {@link FieldMapping} for the {@link RdfResourceEnum#resultScore}
     * property used for the score of query results
     */
    public static final FieldMapping<Double,Object> RESULT_SCORE_MAPPING = 
        new FieldMapping<Double,Object>(RdfResourceEnum.resultScore.getUri(), 
            URI.create("http://www.w3.org/2001/XMLSchema#double"), 
            new PropertySelector<Object>(
                    InMemoryValueFactory.getInstance().createReference(
                        RdfResourceEnum.resultScore.getUri())), 
                    new DoubleTransformer<Object>(), null);
    
    /**
     * Executes the LDPath program on the contexts stored in the backend and
     * returns the result as an RDF graph 
     * @param contexts the contexts to execute the program on
     * @param ldpath the LDPath program to execute
     * @param backend the {@link RDFBackend} to use
     * @return The results stored within an RDF graph
     * @throws LDPathParseException if the parsed LDPath program is invalid
     */
    private static Graph executeLDPath(RDFBackend<Object> backend,
                                 String ldpath,
                                 Set<String> contexts ) throws LDPathParseException {
        Graph data = new IndexedGraph();
        RdfValueFactory vf = new RdfValueFactory(data);
        EntityhubLDPath ldPath = new EntityhubLDPath(backend,vf);
        Program<Object> program = ldPath.parseProgram(getReader(ldpath));
        if(log.isDebugEnabled()){
            log.debug("Execute on Context(s) '{}' LDPath program: \n{}",
                contexts,program.getPathExpression(backend));
        }
        /*
         * NOTE: We do not need to process the Representations returned by
         * EntityhubLDPath#exdecute, because the RdfValueFactory used uses
         * the local variable "Graph data" to backup all created
         * RdfRepresentation. Because of this all converted data will be
         * automatically added the Graph. The only thing we need to do is to
         * wrap the Graph in the response.
         */
        for(String context : contexts){
            ldPath.execute(vf.createReference(context), program);
        }
        return data;
    }
    /**
     * Utility that gets the messages of the parsing error. The message about the
     * problem is contained in some parent Exception. Therefore this follows
     * {@link Exception#getCause()}s. The toString method of the returned map
     * will print the "exception: message" in the correct order.
     * @param e the exception
     * @return the info useful to replay in BAD_REQUEST responses
     */
    public static Map<String,String> getLDPathParseExceptionMessage(LDPathParseException e) {
        Map<String,String> messages = new LinkedHashMap<String,String>();
        Throwable t = e;
        do { // the real parsing error is in some cause ... 
            messages.put(t.getClass().getSimpleName(),t.getMessage()); // ... so collect all messages
            t = t.getCause();
        } while (t != null);
        return messages;
    }
    /**
     * Processes LDPath requests as supported by the {@link SiteManagerRootResource},
     * {@link ReferencedSiteRootResource}, {@link EntityhubRootResource}.
     * @param resource The resource used as context when sending RESTful Service API
     * {@link Viewable} as response entity.
     * @param backend The {@link RDFBackend} implementation
     * @param ldpath the parsed LDPath program
     * @param contexts the Entities to execute the LDPath program
     * @param headers the parsed HTTP headers (used to determine the accepted
     * content type for the response
     * @param servletContext The Servlet context needed for CORS support
     * @return the Response {@link Status#BAD_REQUEST} or {@link Status#OK}.
     */
    public static Response handleLDPathRequest(BaseStanbolResource resource,
                                         RDFBackend<Object> backend,
                                         String ldpath,
                                         Set<String> contexts,
                                         HttpHeaders headers) {
        Collection<String> supported = new HashSet<String>(JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = getAcceptableMediaType(headers,
            supported, MediaType.APPLICATION_JSON_TYPE);
        boolean printDocu = false;
        //remove null and "" element
        contexts.remove(null);
        contexts.remove("");
        if(contexts == null || contexts.isEmpty()){
            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                printDocu = true;
            } else {
                return Response.status(Status.BAD_REQUEST)
                .entity("No context was provided by the Request. Missing parameter context.\n")
                .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        if(!printDocu & (ldpath == null || ldpath.isEmpty())){
            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                printDocu = true;
            } else {
                return Response.status(Status.BAD_REQUEST)
                .entity("No ldpath program was provided by the Request. Missing or empty parameter ldpath.\n")
                .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        if(printDocu){ //a missing parameter and the content type is compatible to HTML
            ResponseBuilder rb = Response.ok(new Viewable("ldpath", resource));
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } else if(acceptedMediaType.equals(TEXT_HTML_TYPE)){
            //HTML is only supported for documentation
            return Response.status(Status.NOT_ACCEPTABLE)
            .entity("The requested content type "+TEXT_HTML+" is not supported.\n")
            .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        }
        Graph data;
        try {
            data = executeLDPath(backend, ldpath, contexts);
        } catch (LDPathParseException e) {
            log.warn("Unable to parse LDPath program:\n"+ldpath,e);
            return Response.status(Status.BAD_REQUEST)
            .entity(("Unable to parse LDPath program (Messages: "+
                    getLDPathParseExceptionMessage(e)+")!\n"))
            .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        }
        ResponseBuilder rb = Response.ok(data);
        rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
    
    
    /**
     * Transform the results of a query
     * @param resultIt The Iterator over the results
     * @param program the LDPath {@link Program} to execute on the results
     * @param selectedFields additional selected fields of the query
     * @param ldPath the Entityhub LDPath
     * @param backend the {@link AbstractBackend} mainly used to 
     * {@link AbstractBackend#addLocal(Representation) add representations} of
     * the query to the local cache
     * @param vf the {@link ValueFactory} used create {@link Reference}s for the
     * String {@link Representation#getId() id}s of the {@link Representation} in
     * the query results
     * @return A collection with the transformed Representations in the processed
     * order.
     */
    public static Collection<Representation> transformQueryResults(Iterator<Representation> resultIt,
                                                             Program<Object> program,
                                                             Set<String> selectedFields,
                                                             EntityhubLDPath ldPath,
                                                             AbstractBackend backend,
                                                             ValueFactory vf) {
        Collection<Representation> transformedResults = new LinkedHashSet<Representation>();
        while(resultIt.hasNext()){
            Representation rep = resultIt.next();
            backend.addLocal(rep); //add results to local cache
            Representation transformed = ldPath.execute(vf.createReference(rep.getId()), program);
            //also add additional selected fields
            for(String selected : selectedFields){
                Iterator<Object> values = rep.get(selected);
                if(values != null){
                    while(values.hasNext()){
                        transformed.add(selected, values.next());
                    }
                }
            }
            transformedResults.add(transformed);
        }
        return transformedResults;
    }


    /**
     * 
     * @param ldpathProgram the LDPath program as string
     * @param selectedFields the selected fields of the query
     * @param backend the RDFBackend  (only needed for logging)
     * @param ldPath the {@link LDPath} used to parse the program.
     * @return the pre-processed and validated program
     * @throws LDPathParseException if the parsed LDPath program string is not
     * valid
     * @throws IllegalStateException if the fields selected by the LDPath
     * program conflict with the fields selected by the query.
     */
    public static Program<Object> prepareQueryLDPathProgram(String ldpathProgram,
                                                Set<String> selectedFields,
                                                AbstractBackend backend,
                                                EntityhubLDPath ldPath) throws LDPathParseException {
        Program<Object> program = ldPath.parseProgram(getReader(ldpathProgram));
        
        //We need to do two things:
        // 1) ensure that no fields define by LDPath are also selected
        StringBuilder conflicting = null;
        // 2) add the field of the result score if not defined by LDPath
        String resultScoreProperty = RdfResourceEnum.resultScore.getUri();
        boolean foundRsultRankingField = false;
        for(FieldMapping<?,Object> ldPathField : program.getFields()){
            String field = ldPathField.getFieldName();
            if(!foundRsultRankingField && resultScoreProperty.equals(field)){
                foundRsultRankingField = true;
            }
            //remove from selected fields -> if we decide later that
            //this should not be an BAD_REQUEST
            if(selectedFields.remove(ldPathField.getFieldName())){
                if(conflicting == null){
                    conflicting = new StringBuilder();
                }
                conflicting.append('\n').append("  > ")
                .append(ldPathField.getPathExpression(backend));
            }
        }
        if(conflicting != null){ //there are conflicts
            throw new IllegalStateException("Selected Fields conflict with Fields defined by" +
                "the LDPath program! Conflicts: "+conflicting.toString());
        }
        if(!foundRsultRankingField){ //if no mapping for the result score
            program.addMapping(RESULT_SCORE_MAPPING); //add the default mapping
        }
        return program;
    }
}
