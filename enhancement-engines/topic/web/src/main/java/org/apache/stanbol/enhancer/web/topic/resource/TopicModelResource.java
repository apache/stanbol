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

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.viewable.Viewable;
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
import org.osgi.service.component.ComponentContext;

/**
 * RESTful interface for classification models: register concept hierarchies,
 * introspect model state and trigger training if a training set is provided.
 *
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/topic/model")
public final class TopicModelResource extends BaseStanbolResource {

    private BundleContext bundleContext;

    @Activate
    protected void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
    }
    
    
    @Path("{classifier}")
    public ClassifierResource getClassifier(@PathParam(value = "classifier") String classifierName,
            @Context UriInfo uriInfo) throws InvalidSyntaxException {
        this.uriInfo = uriInfo;
        ServiceReference[] references = bundleContext.getServiceReferences(TopicClassifier.class.getName(),
                String.format("(%s=%s)", EnhancementEngine.PROPERTY_NAME, classifierName));
        if (references == null || references.length == 0) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return new ClassifierResource((TopicClassifier) bundleContext.getService(references[0]));
    }

    public class ClassifierResource extends ResultData {
        final TopicClassifier classifier;

        public ClassifierResource(TopicClassifier classifier) {
            this.classifier = classifier;
        }
        
        
        public TopicClassifier getClassifier() {
            return classifier;
        }

        @GET
        @Produces(TEXT_HTML)
        public Response get(@Context HttpHeaders headers) {
            ResponseBuilder rb = Response.ok(new Viewable("index", this));
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
            return rb.build();
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
            return rb.build();
        }

        @OPTIONS
        @Path("performance")
        public Response handleCorsPreflightOnPerformance(@Context HttpHeaders headers) {
            ResponseBuilder res = Response.ok();
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
            return rb.build();
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
            return rb.build();
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
            return rb.build();
        }

        /**
         * Simple RDF / SKOS importer that loads the complete model in memory
         * for easy parsing and then does graph introspection to find the
         * concepts to load into the model.
         *
         * If a scalable implementation is required, one should probably use a
         * transient triple store and pass it the raw RDF stream instead of
         * using the naive GraphReader JAX-RS provider.
         */
        @POST
        @Consumes(MediaType.WILDCARD)
        public Response importConceptsFromRDF(@QueryParam(value = "concept_class") String conceptClassUri,
                @QueryParam(value = "broader_property") String broaderPropertyUri,
                ImmutableGraph graph,
                @Context HttpHeaders headers) throws ClassifierException {
            IRI conceptClass = OntologicalClasses.SKOS_CONCEPT;
            IRI broaderProperty = Properties.SKOS_BROADER;
            if (conceptClassUri != null && !conceptClassUri.isEmpty()) {
                conceptClass = new IRI(conceptClassUri);
            }
            if (broaderPropertyUri != null && !broaderPropertyUri.isEmpty()) {
                broaderProperty = new IRI(broaderPropertyUri);
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
            return rb.build();
        }
    }
}
