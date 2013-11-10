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
package org.apache.stanbol.reasoners.web.utils;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
//import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.reasoners.web.resources.ReasoningResult;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOntologyStorer;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Builds a response according to the reasoning output.
 * 
 * @author enridaga
 *
 */
public class ResponseTaskBuilder {
    private final Logger log = LoggerFactory.getLogger(getClass());
//    private UriInfo info;
//    private ServletContext context;
//    private HttpHeaders headers;
    private ReasoningResult result;
//    public ResponseTaskBuilder(UriInfo info, HttpHeaders headers) {
//        this.info = info;
////        this.context = context;
//        this.headers = headers;
//    }

    public ResponseTaskBuilder(ReasoningResult reasoningPrettyResultResource) {
        this.result = reasoningPrettyResultResource;
    }

    /**
     * This is special, in case of task CHECK
     * 
     * @param output
     * @return
     */
    private Response build(boolean result) {
        return buildCheckResponse(result);
    }
    
    private Response build(){
        //return Response.ok().build();
        ResponseBuilder rb = Response.ok();
//        addCORSOrigin(context, rb, headers);
        return rb.build();
    }
    
    /**
     * Process the given object (result content output),
     * returning an HTML representation or delegating the rendering to jersey writers.
     * 
     * @param object
     * @return
     */
    private Response build(Object object){
        if (isHTML()) {
            OutputStream out = stream(object);
            this.result.setResult(out);
            ResponseBuilder rb = Response.ok( 
                   new Viewable("result", result));
            
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
//            addCORSOrigin(context, rb, headers);
            return rb.build();
        /*    return Response.ok(
                    new Viewable("result",
                            new ReasoningPrettyResultResource(
                                    context, info, out)),
                    TEXT_HTML).build();*/
        } else {
            //return Response.ok(object).build();
            ResponseBuilder rb = Response.ok( object );
//            addCORSOrigin(context, rb, headers);
            return rb.build();
        }
    }
    
    /**
     * This supports OWLOntology and jena Model objects.
     * In the case of Jena the reuslt is printed as Turtle, 
     * in case of OWLApi the result is in Manchester syntax (more readable).
     * 
     * FIXME: Both should return the same format
     * 
     * @param object
     * @return
     */
    private OutputStream stream(Object object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if(object instanceof OWLOntology){
            OWLOntology o = (OWLOntology) object;
            ManchesterOWLSyntaxOntologyStorer mosos = new ManchesterOWLSyntaxOntologyStorer();
            try {
                mosos.storeOntology(o.getOWLOntologyManager(), o, new StreamDocumentTarget(out), new ManchesterOWLSyntaxOntologyFormat());
            } catch (OWLOntologyStorageException e) {
                log.error("Cannot stream the ontology",e);
                throw new RuntimeException(e);
            }
        }else if (object instanceof Model){
            Model m = (Model) object;
            // FIXME Both should return the same format
            m.write(out, "TURTLE");
        }
        
        return out;
    }

    /**
     * Check if the client needs a serialization of the output or a human
     * readable form (HTML)
     * 
     * @param headers
     * @return
     */
    private boolean isHTML() {
        // We only want to state if HTML format is the preferred format
        // requested
        Set<String> htmlformats = new HashSet<String>();
        htmlformats.add(TEXT_HTML);
        Set<String> rdfformats = new HashSet<String>();
        String[] formats = { TEXT_HTML, "text/plain", KRFormat.RDF_XML,
                KRFormat.TURTLE, "text/turtle", "text/n3" };
        rdfformats.addAll(Arrays.asList(formats));
        List<MediaType> mediaTypes = result.getHeaders().getAcceptableMediaTypes();
        for (MediaType t : mediaTypes) {
            String strty = t.toString();
            log.debug("Acceptable is {}", t);
            if (htmlformats.contains(strty)) {
                log.debug("Requested format is HTML {}", t);
                return true;
            } else if (rdfformats.contains(strty)) {
                log.debug("Requested format is RDF {}", t);
                return false;
            }
        }
        // Default behavior? Should never happen!
        return true;
    }
    

    /**
     * To build the Response for any CHECK task execution
     * 
     * @param isConsistent
     * @return
     */
    private Response buildCheckResponse(boolean isConsistent) {
        if (isHTML()) {
            if (isConsistent) {
                log.debug("The input is consistent");
                result.setResult("The input is consistent :)");
                ResponseBuilder rb = Response.ok( 
                    new Viewable("result",
                        result
                        )
                     );
             
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
//                addCORSOrigin(context, rb, headers);
                return rb.build();
             
               /*return Response.ok(
                        new Viewable("result",
                                new ReasoningPrettyResultResource(
                                        context, info,
                                        "The input is consistent :)")),
                        TEXT_HTML).build();*/
            } else {
                log.debug("The input is not consistent");
                ResponseBuilder rb = Response.status(Status.CONFLICT);
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
//                addCORSOrigin(context, rb, headers);
                result.setResult("The input is NOT consistent :(");
                rb.entity(new Viewable("result", result));
                return rb.build();
                
                /*return Response
                        .status(Status.CONFLICT)
                        .entity(new Viewable("result",
                                new ReasoningPrettyResultResource(
                                        context, info,
                                        "The input is NOT consistent :(")))
                        .type(TEXT_HTML).build();*/
            }
        } else {
            if (isConsistent) {
                log.debug("The input is consistent");
                //return Response.ok("The input is consistent :)").build();
                ResponseBuilder rb = Response.ok("The input is consistent :)");
//                addCORSOrigin(context, rb, headers);
                return rb.build();
            } else {
                log.debug("The input is not consistent");
                //return Response.status(Status.CONFLICT).build();
                
                ResponseBuilder rb = Response.status(Status.CONFLICT);
//                addCORSOrigin(context, rb, headers);
                return rb.build();
            }
        }
    }
    
    /**
     * Builds a response according to the given {@see ReasoningServiceResult}
     * 
     * @param result
     * @return
     */
    public Response build(ReasoningServiceResult<? extends Object> result){
        // If task is CHECK
        if(result.getTask().equals(ReasoningServiceExecutor.TASK_CHECK)){
            return build(result.isSuccess());
        }else{
            // Elsewhere, if some data is provided, serialize result
            if(result.get()!=null){
                return build(result.get());
            }else{
                return build();
            }
        }
    }
}
