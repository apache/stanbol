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
package org.apache.stanbol.contenthub.web.resources;

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.index.solr.SolrSemanticIndex;
import org.apache.stanbol.contenthub.index.solr.SolrSemanticIndexFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * This class the the web resource to handle the RESTful requests and HTML view of the LDProgram management
 * facilities within Contenthub.
 * 
 * @author anil.pacaci
 * @author anil.sinaci
 * 
 */
@Path("/contenthub/index/ldpath")
public class LDPathSemanticIndexResource extends BaseStanbolResource {

    private static final Logger logger = LoggerFactory.getLogger(LDPathSemanticIndexResource.class);

    private SolrSemanticIndexFactory solrSemanticIndexFactory;

    public LDPathSemanticIndexResource(@Context ServletContext context) {
        solrSemanticIndexFactory = ContextHelper.getServiceFromContext(SolrSemanticIndexFactory.class,
            context);
        if (solrSemanticIndexFactory == null) {
            logger.error("Missing SolrSemanticIndexFactory");
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Missing SolrSemanticIndexFactory\n").build());
        }
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/{pid}")
    public Response handleCorsPreflightDeleteProgram(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers, DELETE, OPTIONS);
        return res.build();
    }

    /**
     * Services to draw HTML view
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response retrieveAllPrograms() {
        return Response.ok(new Viewable("index", this), MediaType.TEXT_HTML).build();
    }

    /**
     * HTTP POST method which creates a {@link SolrSemanticIndex} in the scope of Contenthub.
     * 
     * @param name
     *            The name identifying the index
     * @param description
     *            Description of the index
     * @param program
     *            LDPath program that will be used as a source to create the semantic index. Index fields and
     *            Solr specific configurations regarding those index fields are given in this parameter.
     * @param indexContent
     *            If this configuration is true plain text content of the {@link ContentItem} is also indexed
     *            to be used in the full text search
     * @param batchsize
     *            Maximum number of changes to be returned
     * @param indexingSourceCheckperiod
     *            Time to check changes in the {@link IndexingSource} in second units
     * @param solrchecktime
     *            Maximum time in seconds to wait for the availability of the Solr Server
     * @param ranking
     *            To be able to use other SemanticIndex implementations rather than this, Service Ranking
     *            property of other implementations should be set higher than of this one
     * @param headers
     *            HTTP Headers
     * @return
     * @throws IndexManagementException
     * @throws InterruptedException
     * @throws IndexException
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response submitIndex(@FormParam("name") String name,
                                @FormParam("description") String description,
                                @FormParam("program") String program,
                                @FormParam("indexContent") boolean indexContent,
                                @FormParam("batchSize") @DefaultValue("10") int batchsize,
                                @FormParam("indexingSourceName") @DefaultValue("contenthubFileStore") String indexingSourceName,
                                @FormParam("indexingSourceCheckPeriod") @DefaultValue("10") int indexingSourceCheckperiod,
                                @FormParam("solrCheckTime") @DefaultValue("5") int solrchecktime,
                                @FormParam("ranking") @DefaultValue("0") int ranking,
                                @Context ServletContext context,
                                @Context HttpHeaders headers) throws IndexManagementException,
                                                             InterruptedException,
                                                             IndexException {

        Properties parameters = new Properties();
        parameters.put(SolrSemanticIndex.PROP_NAME, name);
        parameters.put(SolrSemanticIndex.PROP_DESCRIPTION, description);
        parameters.put(SolrSemanticIndex.PROP_LD_PATH_PROGRAM, program);
        parameters.put(SolrSemanticIndex.PROP_INDEX_CONTENT, indexContent);
        parameters.put(SolrSemanticIndex.PROP_BATCH_SIZE, batchsize);
        parameters.put(SolrSemanticIndex.PROP_INDEXING_SOURCE_NAME, indexingSourceName);
        parameters.put(SolrSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD, indexingSourceCheckperiod);
        parameters.put(SolrSemanticIndex.PROP_SOLR_CHECK_TIME, solrchecktime);
        parameters.put(Constants.SERVICE_RANKING, ranking);
        String pid = solrSemanticIndexFactory.createIndex(parameters);
        ResponseBuilder rb = Response.ok(pid);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * HTTP DELETE method to delete an {@link SolrSemanticIndex}.
     * 
     * @param pid
     *            The persistent identifier (pid) of the SolrSemanticIndex to be deleted
     * @param headers
     *            HTTP headers
     * @return
     * @throws IndexManagementException
     */
    @DELETE
    @Path("/{pid}")
    public Response deleteIndex(@PathParam(value = "pid") String pid, @Context HttpHeaders headers) throws IndexManagementException {
        if (!solrSemanticIndexFactory.getSemanticIndexMetadataManager().isConfigured(pid)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        solrSemanticIndexFactory.removeIndex(pid);
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    // Helper methods for HTML view
    public Map<String,List<String>> getLdPrograms() {
        Map<String,Properties> allIndexMetadata = solrSemanticIndexFactory.getSemanticIndexMetadataManager()
                .getAllIndexMetadata();
        Map<String,List<String>> indexMetadataMap = new HashMap<String,List<String>>();
        for (Entry<String,Properties> indexMetadata : allIndexMetadata.entrySet()) {
            List<String> indexMetadataMapValues = new ArrayList<String>();
            Properties properties = indexMetadata.getValue();
            indexMetadataMapValues.add(properties.get(SolrSemanticIndex.PROP_NAME).toString());
            indexMetadataMapValues.add(properties.get(SolrSemanticIndex.PROP_DESCRIPTION).toString());
            indexMetadataMapValues.add(properties.get(SolrSemanticIndex.PROP_INDEX_CONTENT).toString());
            indexMetadataMapValues.add(properties.get(SolrSemanticIndex.PROP_BATCH_SIZE).toString());
            indexMetadataMapValues
                    .add(properties.get(SolrSemanticIndex.PROP_INDEXING_SOURCE_NAME).toString());
            indexMetadataMapValues.add(properties.get(SolrSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD)
                    .toString());
            indexMetadataMapValues.add(properties.get(SolrSemanticIndex.PROP_SOLR_CHECK_TIME).toString());
            indexMetadataMapValues.add(properties.get(SolrSemanticIndex.PROP_LD_PATH_PROGRAM).toString());
            indexMetadataMap.put(indexMetadata.getKey(), indexMetadataMapValues);
        }
        return indexMetadataMap;
    }

}
