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

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ontonet/{ontologyId:.+}")
public class StoredOntologyResource extends BaseStanbolResource {

    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    protected OntologyProvider<?> ontologyProvider;

    public StoredOntologyResource(@PathParam(value = "ontologyId") String ontologyId,
                                  @Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.ontologyProvider = (OntologyProvider<?>) ContextHelper.getServiceFromContext(
            OntologyProvider.class, servletContext);
    }

    /**
     * Gets the ontology with the given identifier in its version managed by the session.
     * 
     * @param sessionId
     *            the session identifier.
     * @param ontologyId
     *            the ontology identifier.
     * @param uriInfo
     * @param headers
     * @return the requested managed ontology, or {@link Status#NOT_FOUND} if either the sessionn does not
     *         exist, or the if the ontology either does not exist or is not managed.
     */
    @GET
    @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getManagedOntology(@PathParam("ontologyId") String ontologyId,
                                       @DefaultValue("false") @QueryParam("merge") boolean merged,
                                       @Context UriInfo uriInfo,
                                       @Context HttpHeaders headers) {
        if (ontologyId == null) return Response.status(Status.BAD_REQUEST).build();
        try {
            IRI iri = IRI.create(ontologyId);
            OWLOntology o = (OWLOntology) ontologyProvider.getStoredOntology(iri, OWLOntology.class, merged);
            if (o == null) return Response.status(NOT_FOUND).build();

            // Rewrite imports
            String uri = uriInfo.getRequestUri().toString();
            URI base = URI.create(uri.substring(0, uri.lastIndexOf(ontologyId) - 1));

            // Rewrite import statements
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
            OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
            /*
             * TODO manage import rewrites better once the container ID is fully configurable (i.e. instead of
             * going upOne() add "session" or "ontology" if needed).
             */
            for (OWLImportsDeclaration oldImp : o.getImportsDeclarations()) {
                changes.add(new RemoveImport(o, oldImp));
                String s = oldImp.getIRI().toString();
                s = s.substring(s.indexOf("::") + 2, s.length());
                IRI target = IRI.create(base + "/" + s);
                changes.add(new AddImport(o, df.getOWLImportsDeclaration(target)));
            }
            o.getOWLOntologyManager().applyChanges(changes);

            return Response.ok(o).build();
        } catch (Exception ex) {
            return Response.status(Status.BAD_REQUEST).build();
        }

    }

}
