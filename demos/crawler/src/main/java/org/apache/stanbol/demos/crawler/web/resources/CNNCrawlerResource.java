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

package org.apache.stanbol.demos.crawler.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.demos.crawler.cnn.CNNCrawler;
import org.apache.stanbol.demos.crawler.web.model.TopicNews;

import org.apache.stanbol.commons.ldviewable.Viewable;

/**
 * This is the web resource for CNN Crawler.
 * 
 * @author cihan
 * 
 */
@Path("/crawler/cnn/{index}")
public class CNNCrawlerResource extends BaseStanbolResource {

	private CNNCrawler cnnCrawler;
	private Object templateData = null;
	private String indexName;

	public CNNCrawlerResource(@Context ServletContext context,
			@PathParam(value = "index") String indexName) {
		this.indexName = indexName;
		this.cnnCrawler = ContextHelper.getServiceFromContext(CNNCrawler.class, context);
	}

	private TopicNews importCNNNews(String topic, Integer max, Boolean full) {
		if (topic == null || topic.isEmpty()) {
			return null;
		}
		if (max == null) {
			max = 10;
		}

		Map<URI, String> newsInfo = cnnCrawler.importCNNNews(topic, max, full,
				indexName);
		TopicNews tn = new TopicNews();
		tn.setTopic(topic);
		tn.setUris(new ArrayList<URI>(newsInfo.keySet()));
		tn.setTitles(new ArrayList<String>(newsInfo.values()));
		return tn;
	}

	/**
	 * For HTML view only.
	 * 
	 * @return Returns the HTML view for CNN News Crawler.
	 */
	@GET
	@Produces(TEXT_HTML)
	public Response importCNNNewsHTML() {
		return Response.ok(new Viewable("index", this), TEXT_HTML).build();
	}

	/**
	 * 
	 * @param topic
	 *            The topic which will be crawled.
	 * @param max
	 *            Maximum number of news to be retrieved from CNN about the
	 *            {@link topic}
	 * @param full
	 *            If {@code yes}, the topic will be crawled in detail to
	 *            retrieve all information from CNN about the {@link topic}. If
	 *            {@code no}, only summary of the news will be crawled and
	 *            imported.
	 * @return Returns the HTML view as the result of importing news from CNN.
	 */
	@POST
	@Produces(TEXT_HTML)
	public Response importCNNNewsHTMLPOST(@FormParam("topic") String topic,
			@FormParam("max") Integer max, @FormParam("full") @DefaultValue("no") String full) {
		this.templateData = importCNNNews(topic, max, full.equals("checked"));
		return Response.ok(new Viewable("index", this), TEXT_HTML).build();
	}

	public Object getTemplateData() {
		return templateData;
	}

	public String getIndexName() {
		return this.indexName;
	}
}
