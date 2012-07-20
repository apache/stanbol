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

import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.servicesapi.index.EndpointType;
import org.apache.stanbol.contenthub.servicesapi.index.IndexException;
import org.apache.stanbol.contenthub.servicesapi.index.IndexManagementException;
import org.apache.stanbol.contenthub.servicesapi.index.SemanticIndex;
import org.apache.stanbol.contenthub.servicesapi.index.SemanticIndexManager;
import org.apache.stanbol.contenthub.web.util.JSONUtils;
import org.apache.stanbol.contenthub.web.util.RestUtil;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

@Path("/contenthub/index")
public class SemanticIndexManagerResource extends BaseStanbolResource {

    private final Logger logger = LoggerFactory.getLogger(SemanticIndexManagerResource.class);

    private SemanticIndexManager semanticIndexManager;

    public SemanticIndexManagerResource(@Context ServletContext context, @Context UriInfo uriInfo) {
        this.semanticIndexManager = ContextHelper.getServiceFromContext(SemanticIndexManager.class, context);
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    /**
     * This method returns the {@link SemanticIndex} representations according to the given parameters in
     * <b>application/json</b> format or HTML view.
     * 
     * @param name
     *            Name of the indexes to be returned
     * @param endpointType
     *            String representation of the {@link EndpointType} of indexes to be returned
     * @param multiple
     *            If this parameter is set to {@code true} it returns one or more indexes matching the given
     *            conditions, otherwise it returns only one index representation it there is any satisfying
     *            the conditions.
     * @return
     * @throws IndexException
     */
    @GET
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    public Response getIndexes(@QueryParam(value = "name") String name,
                               @QueryParam(value = "endpointType") String endpointType,
                               @QueryParam(value = "multiple") @DefaultValue("true") boolean multiple,
                               @Context HttpHeaders headers) throws IndexManagementException {
        MediaType acceptedHeader = RestUtil.getAcceptedMediaType(headers);
        if (acceptedHeader.isCompatible(MediaType.TEXT_HTML_TYPE)) {
            return Response.ok(new Viewable("index", this), MediaType.TEXT_HTML).build();
        } else {
            // if (name == null && endpointType == null) {
            // throw new IllegalArgumentException("At least an index name or an endpoint type must be given");
            // }
            EndpointType epType = null;
            if (endpointType != null) {
                epType = EndpointType.valueOf(endpointType);
            }
            List<SemanticIndex> semanticIndexes = new ArrayList<SemanticIndex>();
            if (multiple) {
                semanticIndexes = semanticIndexManager.getIndexes(name, epType);
            } else {
                SemanticIndex semanticIndex = semanticIndexManager.getIndex(name, epType);
                semanticIndexes.add(semanticIndex);
            }
            ResponseBuilder rb = null;
            try {
                rb = Response.ok(JSONUtils.createJSONString(semanticIndexes), MediaType.APPLICATION_JSON);
            } catch (IndexException e) {
                logger.error("Failed to get field names and field properties from index");
                throw new IndexManagementException(
                        "Failed to get field names and field properties from index");
            } catch (JSONException e) {
                logger.error("Index properties cannot be put to JSON Object");
                throw new IndexManagementException("Index properties cannot be put to JSON Object");
            }
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }

    //
    // HTML view
    //

    public class IndexView {
        private String name;
        private String description;
        private String state;
        private long revision;
        private Map<String,String> endpoints;

        public IndexView(String name,
                         String description,
                         String state,
                         long revision,
                         Map<String,String> endpoints) {
            this.name = name;
            this.description = description;
            this.state = state;
            this.revision = revision;
            this.endpoints = endpoints;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getState() {
            return state;
        }

        public long getRevision() {
            return revision;
        }

        public Map<String,String> getEndpoints() {
            return endpoints;
        }
    }

    public List<IndexView> getSemanticIndexes() throws IndexManagementException {
        List<IndexView> indexView = new ArrayList<IndexView>();
        List<SemanticIndex> indexes = semanticIndexManager.getIndexes(null, null);
        for (SemanticIndex index : indexes) {
            Map<EndpointType,String> restEndpoints = index.getRESTSearchEndpoints();
            Map<String,String> endpoints = new HashMap<String,String>();
            for (Entry<EndpointType,String> restEndpoint : restEndpoints.entrySet()) {
                endpoints.put(restEndpoint.getKey().name(), restEndpoint.getValue());
            }
            indexView.add(new IndexView(index.getName(), index.getDescription(), index.getState().name(),
                    index.getRevision(), endpoints));
        }
        return indexView;
    }

}
