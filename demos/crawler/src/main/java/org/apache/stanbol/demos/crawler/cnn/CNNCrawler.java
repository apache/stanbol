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

package org.apache.stanbol.demos.crawler.cnn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.demos.crawler.web.model.NewsSummary;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

/**
 * 
 * @author cihan
 * @author anil.sinaci
 * 
 */
@Component(metatype = true)
@Service(value=CNNCrawler.class)
public class CNNCrawler {

	private static final Logger logger = LoggerFactory
			.getLogger(CNNCrawler.class);

	private static final String CNN_URL = "http://topics.cnn.com/topics/";
	private static final String TEXT_CLASS = "cnn_strycntntlft";

	@Reference
	private SolrStore solrStore;

	@Activate
	public void activate(ComponentContext cc) {
		if (solrStore == null) {
			logger.error("Cannot activate CNNImporter. There is no SolrStore to be binded.");
		}
	}

	/**
	 * 
	 * @param topic
	 *            The topic which will be crawled.
	 * @param maxNumber
	 *            Max number of news to be retrieved from CNN about the
	 *            {@link topic}
	 * @param fullNews
	 *            If {@code true}, the topic will be crawled in detail to
	 *            retrieve all information from CNN about the {@link topic}. If
	 *            {@code false}, only summary of the news will be crawled and
	 *            imported.
	 * @return A map which includes the URI of the related topic and the news
	 *         content. If {@link fullNews} is {@code true}, the news content is
	 *         the full news; if not, it is the summary of the news.
	 */
	public Map<URI, String> importCNNNews(String topic, int maxNumber,
			boolean fullNews) {
		return importCNNNews(topic, maxNumber, fullNews, null);
	}

	/**
	 * 
	 * @param topic
	 *            The topic which will be crawled.
	 * @param maxNumber
	 *            Max number of news to be retrieved from CNN about the
	 *            {@link topic}
	 * @param fullNews
	 *            If {@code true}, the topic will be crawled in detail to
	 *            retrieve all information from CNN about the {@link topic}. If
	 *            {@code false}, only summary of the news will be crawled and
	 *            imported.
	 * 
	 * @param indexName
	 *            Name of the LDPath program (name of the Solr core/index) to be
	 *            used while storing this content item. LDPath programs can be
	 *            managed through {@link SemanticIndexManagerResource} or
	 *            {@link SemanticIndexManager}
	 * 
	 * @return A map which includes the URI of the related topic and the news
	 *         content. If {@link fullNews} is {@code true}, the news content is
	 *         the full news; if not, it is the summary of the news.
	 */
	public Map<URI, String> importCNNNews(String topic, int maxNumber,
			boolean fullNews, String indexName) {
		List<NewsSummary> summaries = getRelatedNews(topic, maxNumber);
		Map<URI, String> newsInfo = new HashMap<URI, String>();
		if (fullNews) {
			for (NewsSummary summary : summaries) {
				String realContent = getNewsContent(summary.getNewsURI());
				if (realContent != null && !realContent.isEmpty()) {
					summary.setContent(realContent);
				}
			}
		}

		for (NewsSummary summary : summaries) {
			try {
				SolrContentItem sci = solrStore.create(summary.getContent()
						.getBytes(), null, summary.getTitle(), "text/plain",
						null);
				URI uri = new URI(solrStore.enhanceAndPut(sci, indexName));
				String title = summary.getTitle();
				if (uri != null) {
					newsInfo.put(uri, title);
				}
			} catch (Exception e) {
				logger.error("Error storing content {}. Skipping ...",
						summary.getContent(), e);
			}
		}
		return newsInfo;
	}

	private String getNewsContent(URI newsURI) {
		try {
			URL newsURL = newsURI.toURL();
			HtmlCleaner cleaner = new HtmlCleaner();
			TagNode root = cleaner.clean(newsURL);
			Object[] text = root.evaluateXPath("//div[@class='" + TEXT_CLASS
					+ "']");
			StringBuilder realContent = new StringBuilder();
			for (Object storyPart : text) {

				try {
					TagNode storyFragment = (TagNode) storyPart;
					for (TagNode child : storyFragment.getChildTags()) {
						if (child.getName().equals("p")) {
							realContent.append(child.getText().toString());
						}
					}
				} catch (ClassCastException e) {
					logger.debug("Can not cast {} to TagNode",
							storyPart.getClass());
				}
			}
			return realContent.toString();
		} catch (Exception e) {
			logger.warn("Unable to get real content of the news {}",
					newsURI.toString());
		}
		return null;

	}

	private List<NewsSummary> getRelatedNews(String topic, int maxNumber) {
		List<NewsSummary> summaries = new ArrayList<NewsSummary>();
		try {
			URL topicURL = new URL(CNN_URL
					+ topic.toLowerCase().replaceAll(" ", "_"));
			Tidy tidy = new Tidy();
			Document doc = tidy.parseDOM(topicURL.openStream(),
					new ByteArrayOutputStream());
			NodeList nodes = doc.getDocumentElement().getElementsByTagName(
					"div");
			for (int i = 0; i < nodes.getLength(); i++) {
				Node current = nodes.item(i);
				NamedNodeMap atts = current.getAttributes();
				Node classAtt = atts.getNamedItem("class");
				if (classAtt != null
						&& classAtt
								.getNodeValue()
								.equals("cnnRelatedArticle archive-item story cnn_skn_spccovstrylst")) {
					NewsSummary summary = createSummary((Element) current);
					if (summary != null) {
						summaries.add(summary);
					}
				}
				if (summaries.size() >= maxNumber) {
					break;
				}
			}
		} catch (MalformedURLException e) {
			logger.warn("Topic {} results in malformed url.", topic);
		} catch (IOException e) {
			logger.warn("Can get content of topic {}.", topic);
		}
		return summaries;
	}

	private NewsSummary createSummary(Element current) {
		NewsSummary newsSummary = null;
		try {
			String summary = current.getElementsByTagName("p").item(0)
					.getFirstChild().getNodeValue();
			String uri = ((Element) current.getElementsByTagName("a").item(0))
					.getAttribute("href");
			String title = current.getElementsByTagName("a").item(0)
					.getFirstChild().getNodeValue();
			newsSummary = new NewsSummary();
			newsSummary.setNewsURI(new URI(uri));
			newsSummary.setTitle(title);
			newsSummary.setContent(summary);
		} catch (Exception e) {
			newsSummary = null;
			logger.warn("Error creating summary from node {}", current);
		}
		return newsSummary;
	}

}
