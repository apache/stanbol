package org.apache.stanbol.entityhub.jersey.utils;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.entityhub.ldpath.LDPathUtils.getReader;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.entityhub.jersey.resource.EntityhubRootResource;
import org.apache.stanbol.entityhub.jersey.resource.ReferencedSiteRootResource;
import org.apache.stanbol.entityhub.jersey.resource.SiteManagerRootResource;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.exception.LDPathParseException;
import at.newmedialab.ldpath.model.programs.Program;

public class LDPathHelper {
    private static final Logger log = LoggerFactory.getLogger(LDPathHelper.class);
    /**
     * Executes the LDPath program on the contexts stored in the backend and
     * returns the result as an RDF graph 
     * @param contexts the contexts to execute the program on
     * @param ldpath the LDPath program to execute
     * @param backend the {@link RDFBackend} to use
     * @return The results stored within an RDF graph
     * @throws LDPathParseException if the parsed LDPath program is invalid
     */
    private static MGraph executeLDPath(RDFBackend<Object> backend,
                                 String ldpath,
                                 Set<String> contexts ) throws LDPathParseException {
        MGraph data = new SimpleMGraph();
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
         * the local variable "MGraph data" to backup all created
         * RdfRepresentation. Because of this all converted data will be
         * automatically added the MGraph. The only thing we need to do is to
         * wrap the MGraph in the response.
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
    private static Map<String,String> getLDPathParseExceptionMessage(LDPathParseException e) {
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
                                         HttpHeaders headers,
                                         ServletContext servletContext) {
        Collection<String> supported = new HashSet<String>(JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,
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
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
        MGraph data;
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
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
}
