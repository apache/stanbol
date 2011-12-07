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

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.helper.cnn.CNNImporter;
import org.apache.stanbol.contenthub.web.search.model.TopicNews;

import com.sun.jersey.api.view.Viewable;

/**
 * 
 * @author cihan
 * 
 */
@Path("/contenthub/import/cnn")
public class CNNImporterResource extends BaseStanbolResource {

    private CNNImporter cnnImporter;
    private Object templateData = null;

    public CNNImporterResource(@Context ServletContext context) {
        cnnImporter = ContextHelper.getServiceFromContext(CNNImporter.class, context);
    }

    private TopicNews importCNNNews(String topic, Integer max, Boolean full) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }
        if (max == null) {
            max = 10;
        }
        if (full == null) {
            full = false;
        }

        Map<URI,String> newsInfo = cnnImporter.importCNNNews(topic, max, full);
        TopicNews tn = new TopicNews();
        tn.setTopic(topic);
        tn.setUris(new ArrayList<URI>(newsInfo.keySet()));
        tn.setTitles(new ArrayList<String>(newsInfo.values()));
        return tn;
    }

    @GET
    @Produces(TEXT_HTML)
    public Response importCNNNewsHTML() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

    @POST
    @Produces(TEXT_HTML)
    public Response importCNNNewsHTMLPOST(@FormParam("topic") String topic,
                                          @FormParam("max") Integer max,
                                          @FormParam("full") Boolean full) {
        this.templateData = importCNNNews(topic, max, full);
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

    public Object getTemplateData() {
        return templateData;
    }
}
