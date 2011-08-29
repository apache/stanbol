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
package org.apache.stanbol.factstore.web.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdParser;
import org.apache.stanbol.commons.jsonld.JsonLdProfile;
import org.apache.stanbol.commons.jsonld.JsonLdProfileParser;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.factstore.api.FactStore;
import org.apache.stanbol.factstore.model.Fact;
import org.apache.stanbol.factstore.model.FactSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

@Path("/factstore/facts")
public class FactsResource extends BaseStanbolResource {

    private static Logger logger = LoggerFactory.getLogger(FactsResource.class);

    private final FactStore factStore;

    public FactsResource(@Context ServletContext context) {
        this.factStore = ContextHelper.getServiceFromContext(FactStore.class, context);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

    @GET
    @Path("/{factSchemaURN}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFactSchema(@PathParam("factSchemaURN") String factSchemaURN) {
        Response validationResponse = standardValidation(factSchemaURN);
        if (validationResponse != null) {
            return validationResponse;
        }

        logger.info("Request for getting existing fact schema {}", factSchemaURN);

        FactSchema factSchema = this.factStore.getFactSchema(factSchemaURN);
        if (factSchema == null) {
            return Response.status(Status.NOT_FOUND).entity("Could not find fact schema " + factSchemaURN)
                    .build();
        }

        return Response.ok(factSchema.toJsonLdProfile().toString()).build();
    }

    @PUT
    @Path("/{factSchemaURN}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putFactSchema(String jsonLdProfileString, @PathParam("factSchemaURN") String factSchemaURN) {
        Response validationResponse = standardValidation(factSchemaURN);
        if (validationResponse != null) {
            return validationResponse;
        }

        logger.info("Request for putting new fact schema {}", factSchemaURN);

        JsonLdProfile profile = null;
        try {
            profile = JsonLdProfileParser.parseProfile(jsonLdProfileString);
        } catch (Exception e) { /* ignore this exception here - it was logged by the parser */}

        if (profile == null) {
            return Response.status(Status.BAD_REQUEST).entity(
                "Could not parse provided JSON-LD Profile structure.").build();
        }

        try {
            if (this.factStore.existsFactSchema(factSchemaURN)) {
                return Response.status(Status.CONFLICT).entity(
                    "The fact schema " + factSchemaURN + " already exists.").build();
            }
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                "Error while checking existence of fact schema " + factSchemaURN).build();
        }

        try {
            this.factStore.createFactSchema(FactSchema.fromJsonLdProfile(factSchemaURN, profile));
        } catch (Exception e) {
            logger.error("Error creating new fact schema {}", factSchemaURN, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                "Error while creating new fact in database.").build();
        }

        return Response.status(Status.CREATED).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postFacts(String jsonLdFacts) {
        JsonLd jsonLd = null;
        try {
            jsonLd = JsonLdParser.parse(jsonLdFacts);
        } catch (Exception e) {
            /* ignore here */
        }

        if (jsonLd == null) {
            return Response.status(Status.BAD_REQUEST).entity("Could not parse provided JSON-LD structure.")
                    .build();
        }

        if (jsonLd.getResourceSubjects().size() < 2) {
            // post a single fact
            Fact fact = Fact.factFromJsonLd(jsonLd);
            if (fact != null) {
                logger.info("Request for posting new fact for {}", fact.getFactSchemaURN());
                try {
                    this.factStore.addFact(fact);
                } catch (Exception e) {
                    logger.error("Error adding new fact", e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                        e.getMessage()).build();
                }
            } else {
                return Response.status(Status.BAD_REQUEST).entity(
                    "Could not extract fact from JSON-LD input.").build();
            }
        } else {
            // post multiple facts
            Set<Fact> facts = Fact.factsFromJsonLd(jsonLd);
            if (facts != null) {
                logger.info("Request for posting a set of new facts");
                try {
                    this.factStore.addFacts(facts);
                } catch (Exception e) {
                    logger.error("Error adding new facts", e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                        e.getMessage()).build();
                }
            } else {
                return Response.status(Status.BAD_REQUEST).entity(
                    "Could not extract facts from JSON-LD input.").build();
            }
        }

        return Response.status(Status.OK).build();
    }

    private Response standardValidation(String factSchemaURN) {
        if (this.factStore == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                "The FactStore is not configured properly").build();
        }

        if (factSchemaURN == null || factSchemaURN.isEmpty()) {
            return Response.status(Status.BAD_REQUEST).entity("No fact schema URN specified.").build();
        }

        if (factSchemaURN.length() > this.factStore.getMaxFactSchemaURNLength()) {
            return Response.status(Status.BAD_REQUEST).entity(
                "The fact schema URN " + factSchemaURN + " is too long. A maximum of "
                        + this.factStore.getMaxFactSchemaURNLength() + " characters is allowed").build();
        }

        return null;
    }
}
