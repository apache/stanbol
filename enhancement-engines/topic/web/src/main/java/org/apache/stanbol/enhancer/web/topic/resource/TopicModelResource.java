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
package org.apache.stanbol.enhancer.web.topic.resource;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.commons.viewable.Viewable;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.topic.api.ClassifierException;
import org.apache.stanbol.enhancer.topic.api.TopicClassifier;
import org.apache.stanbol.enhancer.topic.api.training.TrainingSet;
import org.apache.stanbol.enhancer.topic.api.training.TrainingSetException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.List;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;


/**
 * RESTful interface for classification models: register concept hierarchies, introspect model state and
 * trigger training if a training set is provided.
 * 
 */
@Path("/topic/model/{classifier}")
public final class TopicModelResource extends BaseStanbolResource {

    final TopicClassifier classifier;

    public TopicModelResource(@PathParam(value = "classifier") String classifierName,
                              @Context ServletContext servletContext,
                              @Context UriInfo uriInfo) throws InvalidSyntaxException {
        this.servletContext = servletContext;
        this.uriInfo = uriInfo;
        BundleContext bundleContext = ContextHelper.getBundleContext(servletContext);
        ServiceReference[] references = bundleContext.getServiceReferences(TopicClassifier.class.getName(),
            String.format("(%s=%s)", EnhancementEngine.PROPERTY_NAME, classifierName));
        if (references == null || references.length == 0) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        classifier = (TopicClassifier) bundleContext.getService(references[0]);
    }

    public TopicClassifier getClassifier() {
        return classifier;
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @OPTIONS
    @Path("concept")
    public Response handleCorsPreflightOnConcept(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    // TODO: make it possible to fetch concept descriptions (with broader and narrower links) using the GET
    // verb

    @POST
    @Path("concept")
    @Consumes(MediaType.WILDCARD)
    public Response addConcept(@QueryParam(value = "id") String concept,
                               @QueryParam(value = "primary_topic") String primaryTopicUri,
                               @QueryParam(value = "broader") List<String> broaderConcepts,
                               @Context HttpHeaders headers) throws ClassifierException {
        classifier.addConcept(concept, primaryTopicUri, broaderConcepts);
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @DELETE
    @Path("concept")
    @Consumes(MediaType.WILDCARD)
    public Response remoteConcept(@QueryParam(value = "id") String concept, @Context HttpHeaders headers) throws ClassifierException {
        if (concept != null && !concept.isEmpty()) {
            classifier.removeConcept(concept);
        } else {
            classifier.removeAllConcepts();
        }
        // TODO: count the number of deleted entries and return is a text entity
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @OPTIONS
    @Path("performance")
    public Response handleCorsPreflightOnPerformance(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    // TODO: make it possible to fetch performance reports and evaluation running state using the GET verb

    @POST
    @Path("performance")
    @Consumes(MediaType.WILDCARD)
    public Response updatePerformance(@QueryParam(value = "incremental") Boolean incremental,
                                      @Context HttpHeaders headers) throws TrainingSetException,
                                                                   ClassifierException {
        if (incremental == null) {
            incremental = Boolean.TRUE;
        }
        int updated = classifier.updatePerformanceEstimates(incremental);
        ResponseBuilder rb = Response.ok(String.format(
            "Successfully updated the performance estimates of %d concept(s).\n", updated));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @OPTIONS
    @Path("trainer")
    public Response handleCorsPreflightOnTrainer(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    // TODO: make it possible to fetch training set statistics and training state using the GET verb

    @POST
    @Path("trainer")
    @Consumes(MediaType.WILDCARD)
    public Response updateModel(@QueryParam(value = "incremental") Boolean incremental,
                                @Context HttpHeaders headers) throws TrainingSetException,
                                                             ClassifierException {
        if (incremental == null) {
            incremental = Boolean.TRUE;
        }
        int updated = classifier.updateModel(incremental);
        ResponseBuilder rb = Response.ok(String.format(
            "Successfully updated the statistical model(s) of %d concept(s).\n", updated));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public Response handleCorsPreflightOnTrainingSet(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    // TODO: make it possible browse the training set content on the GET verb using a subresource

    @POST
    @Path("trainingset")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response registerExample(@QueryParam(value = "example_id") String exampleId,
                                    @QueryParam(value = "concept") List<String> concepts,
                                    String textContent,
                                    @Context HttpHeaders headers) throws TrainingSetException,
                                                                 ClassifierException {
        ResponseBuilder rb;
        if (!classifier.isUpdatable()) {
            rb = Response.status(Response.Status.BAD_REQUEST).entity(
                String.format("Classifier %s is not updateble.\n", classifier.getName()));
        } else {
            TrainingSet trainingSet = classifier.getTrainingSet();
            exampleId = trainingSet.registerExample(exampleId, textContent, concepts);
            // TODO: make example GETable resources and return a 201 to it instead of a simple message.
            rb = Response.ok(String.format(
                "Successfully added or updated example '%s' in training set '%s'.\n", exampleId,
                trainingSet.getName()));
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    // TODO make the following a DELETE method on the example sub-resources them-selves once we have a GET for
    // them

    @DELETE
    @Path("trainingset")
    @Consumes(MediaType.WILDCARD)
    public Response removeExample(@QueryParam(value = "example_id") List<String> exampleIds,
                                  @Context HttpHeaders headers) throws TrainingSetException,
                                                               ClassifierException {
        ResponseBuilder rb;
        if (!classifier.isUpdatable()) {
            rb = Response.status(Response.Status.BAD_REQUEST).entity(
                String.format("Classifier %s is not updateble.\n", classifier.getName()));
        } else {
            TrainingSet trainingSet = classifier.getTrainingSet();
            if (exampleIds != null && !exampleIds.isEmpty()) {
                for (String exampleId : exampleIds) {
                    trainingSet.registerExample(exampleId, null, null);
                }
            } else {
                // implement a way to cleanup a complete training set? or is it too dangerous and we should
                // return an error instead?
            }
            rb = Response.ok(String.format("Successfully deleted examples in training set '%s'.\n",
                trainingSet.getName()));
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    /**
     * Simple RDF / SKOS importer that loads the complete model in memory for easy parsing and then does graph
     * introspection to find the concepts to load into the model.
     * 
     * If a scalable implementation is required, one should probably use a transient triple store and pass it
     * the raw RDF stream instead of using the naive GraphReader JAX-RS provider.
     */
    @POST
    @Consumes(MediaType.WILDCARD)
    public Response importConceptsFromRDF(@QueryParam(value = "concept_class") String conceptClassUri,
                                          @QueryParam(value = "broader_property") String broaderPropertyUri,
                                          Graph graph,
                                          @Context HttpHeaders headers) throws ClassifierException {
        UriRef conceptClass = OntologicalClasses.SKOS_CONCEPT;
        UriRef broaderProperty = Properties.SKOS_BROADER;
        if (conceptClassUri != null && !conceptClassUri.isEmpty()) {
            conceptClass = new UriRef(conceptClassUri);
        }
        if (broaderPropertyUri != null && !broaderPropertyUri.isEmpty()) {
            broaderProperty = new UriRef(broaderPropertyUri);
        }
        int imported = classifier.importConceptsFromGraph(graph, conceptClass, broaderProperty);
        ResponseBuilder rb;
        if (imported == 0) {
            rb = Response.status(Response.Status.BAD_REQUEST).entity(
                String.format("Could not find any instances of '%s' in payload.\n",
                    conceptClass.getUnicodeString()));
        } else {
            rb = Response.ok(String.format("Imported %d instance of '%s'.\n", imported,
                conceptClass.getUnicodeString()));
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
}
