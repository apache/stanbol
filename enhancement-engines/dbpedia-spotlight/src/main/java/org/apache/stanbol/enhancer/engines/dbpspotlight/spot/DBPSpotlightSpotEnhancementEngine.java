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
package org.apache.stanbol.enhancer.engines.dbpspotlight.spot;

import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_SPOTTER;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_URL_KEY;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.UTF8;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.utils.XMLParser.loadXMLFromInputStream;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.SurfaceForm;
import org.apache.stanbol.enhancer.engines.dbpspotlight.utils.SpotlightEngineUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * {@link DBPSpotlightSpotEnhancementEngine} provides functionality to enhance
 * document with their language.
 * 
 * @author Iavor Jelev, Babelmonkeys (GzEvD)
 */
@Component(metatype = true, immediate = true, 
	label = "%stanbol.DBPSpotlightSpotEnhancementEngine.name", 
	description = "%stanbol.DBPSpotlightSpotEnhancementEngine.description")
@Service
@Properties(value = { 
		@Property(name = EnhancementEngine.PROPERTY_NAME, value = "dbpspotlightspot"),
		@Property(name = PARAM_URL_KEY, value = "http://spotlight.dbpedia.org/rest/spot"),
		@Property(name = PARAM_SPOTTER)
})
public class DBPSpotlightSpotEnhancementEngine extends
		AbstractEnhancementEngine<IOException, RuntimeException> implements
		EnhancementEngine, ServiceProperties {

	/**
	 * Ensures this engine is deactivated in {@link OfflineMode}
	 */
	@SuppressWarnings("unused")
	@Reference
	private OnlineMode onlineMode;
	
	/**
	 * The default value for the Execution of this Engine. Currently set to
	 * <code>{@link ServiceProperties#ORDERING_CONTENT_EXTRACTION} - 29</code>
	 */
	public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION - 29;


	/** holds the logger. */
	private static final Logger log = LoggerFactory
			.getLogger(DBPSpotlightSpotEnhancementEngine.class);

	/** holds the url of the Spotlight REST endpoint */
	private URL spotlightUrl;
	/** holds the chosen of spotter to be used */
	private String spotlightSpotter;

    private int connectionTimeout;

	/**
	 * Default constructor used by OSGI
	 */
	public DBPSpotlightSpotEnhancementEngine(){}
	
	protected DBPSpotlightSpotEnhancementEngine(URL spotlightUrl, String spotlightSpotter, int connectionTimeout){
		this.spotlightUrl = spotlightUrl;
		this.spotlightSpotter = spotlightSpotter;
		this.connectionTimeout = connectionTimeout;
	}
	
	/**
	 * Initialize all parameters from the configuration panel, or with their
	 * default values
	 * 
	 * @param ce
	 *            the {@link ComponentContext}
	 */
	@SuppressWarnings("unchecked")
	protected void activate(ComponentContext ce) throws ConfigurationException,
			IOException {

		super.activate(ce);

		Dictionary<String, Object> properties = ce.getProperties();
		spotlightUrl = SpotlightEngineUtils.parseSpotlightServiceURL(properties);
        connectionTimeout = SpotlightEngineUtils.getConnectionTimeout(properties);

		//also set the spotter to null if an empty string is parsed
		Object spotterConfig = properties.get(PARAM_SPOTTER);
		spotlightSpotter = spotterConfig != null && !spotterConfig.toString().isEmpty() ?
				spotterConfig.toString() : null;
	}

	/**
	 * Check if the content can be enhanced
	 * 
	 * @param ci
	 *            the {@link ContentItem}
	 */
	public int canEnhance(ContentItem ci) throws EngineException {
		return SpotlightEngineUtils.canProcess(ci) ?
				ENHANCE_ASYNC : CANNOT_ENHANCE;
	}

	/**
	 * Calculate the enhancements by doing a POST request to the DBpedia
	 * Spotlight endpoint and processing the results
	 * 
	 * @param ci
	 *            the {@link ContentItem}
	 */
	public void computeEnhancements(ContentItem ci) throws EngineException {
		Language language = SpotlightEngineUtils.getContentLanguage(ci);
		String text = SpotlightEngineUtils.getPlainContent(ci);

		Collection<SurfaceForm> dbpslGraph = doPostRequest(text,ci.getUri());
		if (dbpslGraph != null) {
			// Acquire a write lock on the ContentItem when adding the
			// enhancements
			ci.getLock().writeLock().lock();
			try {
				createEnhancements(dbpslGraph, ci,text,language);
				if (log.isDebugEnabled()) {
					Serializer serializer = Serializer.getInstance();
					ByteArrayOutputStream debugStream = new ByteArrayOutputStream();
					serializer.serialize(debugStream, ci.getMetadata(),
							"application/rdf+xml");
					try {
						log.debug("DBpedia Spotlight Spot Enhancements:\n{}",
								debugStream.toString("UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			} finally {
				ci.getLock().writeLock().unlock();
			}
		}
	}

	/**
	 * The method adds the returned DBpedia Spotlight surface forms to the
	 * content item's metadata. For each one an TextAnnotation is created.
	 * 
	 * @param occs
	 *            a Collection of entity information
	 * @param ci
	 *            the content item
	 */
	protected void createEnhancements(Collection<SurfaceForm> occs,
			ContentItem ci,  String content, Language lang) {

		HashMap<String, IRI> entityAnnotationMap = new HashMap<String, IRI>();

		Graph model = ci.getMetadata();
		for (SurfaceForm occ : occs) {
			IRI textAnnotation = SpotlightEngineUtils.createTextEnhancement(
					occ, this, ci, content, lang);
			if (entityAnnotationMap.containsKey(occ.name)) {
				model.add(new TripleImpl(entityAnnotationMap.get(occ.name),
						DC_RELATION, textAnnotation));
			} else {
				entityAnnotationMap.put(occ.name, textAnnotation);
			}
		}
	}


	/**
	 * Sends a POST request to the DBpediaSpotlight url.
	 * 
	 * @param text
	 *            a <code>String</code> with the text to be analyzed
	 * @param contentItemUri
	 *            the URI of the ContentItem (only used for logging)
	 * @return a <code>String</code> with the server response
	 * @throws EngineException
	 *             if the request cannot be sent
	 */
	protected Collection<SurfaceForm> doPostRequest(String text,IRI contentItemUri)
			throws EngineException {
		//rwesten: reimplemented this so that the request
		//         is directly written to the request instead
		//         of storing the data in an in-memory StringBuilder
		HttpURLConnection connection = null;
		BufferedWriter wr = null;
		try {
			connection = (HttpURLConnection) spotlightUrl.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			connection.setRequestProperty("Accept", "text/xml");

            //set ConnectionTimeout (if configured)
            if(connectionTimeout > 0){
                connection.setConnectTimeout(connectionTimeout*1000);
                connection.setReadTimeout(connectionTimeout*1000);
            }

            connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			wr = new BufferedWriter(new OutputStreamWriter(
					connection.getOutputStream(),UTF8));
		} catch (IOException e) {
			IOUtils.closeQuietly(wr);
			throw new EngineException("Unable to open connection to "+
					spotlightUrl,e);
		}
		try {
			if (spotlightSpotter != null && !spotlightSpotter.isEmpty()) {
				wr.write("spotter=");
				wr.write(URLEncoder.encode(spotlightSpotter, UTF8.name()));
				wr.write('&');
			}
			wr.write("text=");
			//now append the URL encoded text
			//TODO: This will load the URLEncoded variant in-memory.
			//      One could avoid that by encoding the data in smaller
			//      pieces, but using URLEncoding for big data is anyway
			//      very inefficient. So instead of fixing this issue here
			//      DBpedia Spotlight should support "multipart/from-data"
			//      instead.
			//      As soon as this is supported this should be re-implemented
			//      to support streaming.
			wr.write(URLEncoder.encode(text, UTF8.name()));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(
					"The platform does not support encoding " + UTF8.name(),e);
		} catch (IOException e) {
			throw new EngineException("Unable to write 'plain/text' content "
					+ "for ContentItem "+contentItemUri+" to "
					+ spotlightUrl,e);
		} finally {
			IOUtils.closeQuietly(wr);
		}
		// rwesten: reimplemented this to read the XML
		// Document directly form the response
		InputStream is = null;
		Document xmlDoc;
		try {
			// Get Response
			 is = connection.getInputStream();
			xmlDoc = loadXMLFromInputStream(is);
		} catch (IOException e) {
			throw new EngineException("Unable to spot Entities with"
					+ "Dbpedia Spotlight Spot RESTful Serice running at "
					+ spotlightUrl,e);
		} catch(SAXException e) {
			throw new EngineException("Unable to parse Response from "
					+ "Dbpedia Spotlight Spot RESTful Serice running at "
					+ spotlightUrl,e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		//rwesten: commented the disconnect to allow keep-alive
		//connection.disconnect();
	    return SurfaceForm.parseSurfaceForm(xmlDoc);
	}



	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(
				ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}
}
