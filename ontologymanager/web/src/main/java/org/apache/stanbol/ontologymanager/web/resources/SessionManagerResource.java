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
package org.apache.stanbol.ontologymanager.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
//import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.viewable.Viewable;
//import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;


@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/ontonet/session")
public class SessionManagerResource extends BaseStanbolResource {

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    @Reference
    protected SessionManager sessionManager;

    public SessionManagerResource() {
//        this.sessionManager = (SessionManager) ContextHelper.getServiceFromContext(SessionManager.class,
//            servletContext);
    }

    @POST
    public Response createSessionWithAutomaticId(@Context UriInfo uriInfo, @Context HttpHeaders headers) {
        Session s;
        try {
            s = sessionManager.createSession();
        } catch (SessionLimitException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        }
        String uri = uriInfo.getRequestUri().toString();
        while (uri.endsWith("/"))
            uri = uri.substring(0, uri.length() - 1);
        uri += "/" + s.getID();
        ResponseBuilder rb = Response.created(URI.create(uri));
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public Set<Session> getSessions() {
        Set<Session> result = new HashSet<Session>();
        for (String id : sessionManager.getRegisteredSessionIDs())
            result.add(sessionManager.getSession(id));
        return result;
    }

    @GET
    @Produces(value = {RDF_XML, OWL_XML, TURTLE, X_TURTLE, FUNCTIONAL_OWL, MANCHESTER_OWL, RDF_JSON, N3,
                       N_TRIPLE, TEXT_PLAIN})
    public Response listSessions(@Context UriInfo uriInfo, @Context HttpHeaders headers) {
        OWLOntologyManager ontMgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = ontMgr.getOWLDataFactory();
        OWLClass cSession = df.getOWLClass(IRI.create("http://stanbol.apache.org/ontologies/meta/Session"));

        OWLOntology o;
        try {
            o = ontMgr.createOntology(IRI.create(uriInfo.getRequestUri()));
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
            for (String id : sessionManager.getRegisteredSessionIDs()) {
                IRI sessionid = IRI.create(sessionManager.getDefaultNamespace() + sessionManager.getID() + "/" + id);
                OWLNamedIndividual ind = df.getOWLNamedIndividual(sessionid);
                changes.add(new AddAxiom(o, df.getOWLClassAssertionAxiom(cSession, ind)));
            }
            ontMgr.applyChanges(changes);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        ResponseBuilder rb = Response.ok(o);
        MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
        if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

}
