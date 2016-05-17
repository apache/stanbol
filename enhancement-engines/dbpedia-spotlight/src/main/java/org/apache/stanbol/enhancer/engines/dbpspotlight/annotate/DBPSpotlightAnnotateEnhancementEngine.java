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
package org.apache.stanbol.enhancer.engines.dbpspotlight.annotate;

import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_CONFIDENCE;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_DISAMBIGUATOR;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_RESTRICTION;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_SPARQL;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_SPOTTER;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_SUPPORT;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_URL_KEY;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.UTF8;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.utils.SpotlightEngineUtils.getConnectionTimeout;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.utils.XMLParser.loadXMLFromInputStream;

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
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.Annotation;
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
 * {@link DBPSpotlightAnnotateEnhancementEngine} provides functionality to
 * enhance a document using the DBpedia Spotlight /annotate REST endpoint
 * 
 * @author Iavor Jelev, Babelmonkeys (GzEvD)
 */
@Component(metatype = true, immediate = true, 
    label = "%stanbol.DBPSpotlightAnnotateEnhancementEngine.name", 
    description = "%stanbol.DBPSpotlightAnnotateEnhancementEngine.description")
@Service
@Properties(value = { 
		@Property(name = EnhancementEngine.PROPERTY_NAME, value = "dbpspotlightannotate"),
		@Property(name = PARAM_URL_KEY, value = "http://spotlight.dbpedia.org/rest/annotate"),
		@Property(name = PARAM_SPOTTER),
		@Property(name = PARAM_DISAMBIGUATOR),
		@Property(name = PARAM_RESTRICTION),
		@Property(name = PARAM_SPARQL),
		@Property(name = PARAM_SUPPORT),
		@Property(name = PARAM_CONFIDENCE)
})
public class DBPSpotlightAnnotateEnhancementEngine extends
		AbstractEnhancementEngine<IOException, RuntimeException> implements
		EnhancementEngine, ServiceProperties {

	/**
	 * Ensures this engine is deactivated in {@link OfflineMode}
	 */
	@SuppressWarnings("unused")
	@Reference
	private OnlineMode onlineMode;

	/**
	 * The default value for the Execution of this Engine.
	 */
	public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION - 27;

	/** holds the logger. */
	private static final Logger log = LoggerFactory
			.getLogger(DBPSpotlightAnnotateEnhancementEngine.class);

	/** holds the url of the Spotlight REST endpoint */
	private URL spotlightUrl;
	/** holds the chosen of spotter to be used */
	private String spotlightSpotter;
	/** holds the chosen of disambiguator to be used */
	private String spotlightDisambiguator;
	/** holds the type restriction for the results, if the user wishes one */
	private String spotlightTypesRestriction;
	/** holds the chosen minimal support value */
	private String spotlightSupport;
	/** holds the chosen minimal confidence value */
	private String spotlightConfidence;
	/** holds the sparql restriction for the results, if the user wishes one */
	private String spotlightSparql;

    private int connectionTimeout;

	/**
	 * Default constructor used by OSGI. Expects {@link #activate(ComponentContext)}
	 * to be called before the instance is used.
	 */
	public DBPSpotlightAnnotateEnhancementEngine(){}
	
	/**
	 * Constructor intended to be used by unit tests
	 * @param spotlightUrl
	 */
	protected DBPSpotlightAnnotateEnhancementEngine(URL spotlightUrl, int connectionTimeout){
		this.spotlightUrl = spotlightUrl;
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
        connectionTimeout = getConnectionTimeout(properties);
		spotlightSpotter = properties.get(PARAM_SPOTTER) == null ? null
				: (String) properties.get(PARAM_SPOTTER);
		spotlightDisambiguator = properties.get(PARAM_DISAMBIGUATOR) == null ? null
				: (String) properties.get(PARAM_DISAMBIGUATOR);
		spotlightTypesRestriction = properties.get(PARAM_RESTRICTION) == null ? null
				: (String) properties.get(PARAM_RESTRICTION);
		spotlightSparql = properties.get(PARAM_SPARQL) == null ? null
				: (String) properties.get(PARAM_SPARQL);
		spotlightSupport = properties.get(PARAM_SUPPORT) == null ? null
				: (String) properties.get(PARAM_SUPPORT);
		spotlightConfidence = properties.get(PARAM_CONFIDENCE) == null ? null
				: (String) properties.get(PARAM_CONFIDENCE);
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

		Collection<Annotation> dbpslGraph = doPostRequest(text,ci.getUri());
		Map<SurfaceForm,IRI> surfaceForm2TextAnnotation = new HashMap<SurfaceForm,IRI>();
		if (dbpslGraph != null) {
			// Acquire a write lock on the ContentItem when adding the
			// enhancements
			ci.getLock().writeLock().lock();
			try {
				createEnhancements(dbpslGraph, ci, text, language, surfaceForm2TextAnnotation);
				if (log.isDebugEnabled()) {
					Serializer serializer = Serializer.getInstance();
					ByteArrayOutputStream debugStream = new ByteArrayOutputStream();
					serializer.serialize(debugStream, ci.getMetadata(),
							"application/rdf+xml");
					try {
						log.debug("DBPedia Spotlight Enhancements:\n{}",
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
	 * This generates enhancement structures for the entities from DBPedia
	 * Spotlight and adds them to the content item's metadata. For each entity a
	 * TextAnnotation and an EntityAnnotation are created. An EntityAnnotation
	 * can relate to several TextAnnotations.
	 * 
	 * @param occs
	 *            a Collection of entity information
	 * @param ci
	 *            the content item
	 */
	protected void createEnhancements(Collection<Annotation> occs,
			ContentItem ci, String text, Language language,
			Map<SurfaceForm,IRI> surfaceForm2TextAnnotation) {
		for (Annotation occ : occs) {
			IRI textAnnotation = surfaceForm2TextAnnotation.get(occ.surfaceForm);
			if(textAnnotation == null){ //not yet written ... create a new
    			textAnnotation = SpotlightEngineUtils.createTextEnhancement(
    					occ.surfaceForm, this, ci, text, language);
    			surfaceForm2TextAnnotation.put(occ.surfaceForm,textAnnotation);
			}
			SpotlightEngineUtils.createEntityAnnotation(occ, this, ci, textAnnotation, language);
		}
	}


	/**
	 * Sends a POST request to the DBpediaSpotlight endpoint.
	 * 
	 * @param text
	 *            a <code>String</code> with the text to be analyzed
	 * @param contentItemUri
	 *            the URI of the content item (only used for logging)
	 * @return a <code>Collection<DBPSLAnnotation></code> with the server
	 *         response
	 * @throws EngineException
	 *             if the request cannot be sent
	 */
	protected Collection<Annotation> doPostRequest(String text, IRI contentItemUri)
			throws EngineException {
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
			if (spotlightSpotter != null && !spotlightSpotter.isEmpty()){
				wr.write("spotter=");
				wr.write(URLEncoder.encode(spotlightSpotter, "UTF-8"));
				wr.write('&');
			}
			if (spotlightDisambiguator != null
					&& !spotlightDisambiguator.isEmpty()){
				wr.write("disambiguator=");
				wr.write(URLEncoder.encode(spotlightDisambiguator, "UTF-8"));
				wr.write('&');
			}
			if (spotlightTypesRestriction != null
					&& !spotlightTypesRestriction.isEmpty()) {
				wr.write("types=");
				wr.write(URLEncoder.encode(spotlightTypesRestriction, "UTF-8"));
				wr.write('&');
			}
			if (spotlightSupport != null && !spotlightSupport.isEmpty()) {
				wr.write("support=");
				wr.write(URLEncoder.encode(spotlightSupport, "UTF-8"));
				wr.write('&');
			}
			if (spotlightConfidence != null && !spotlightConfidence.isEmpty()) {
				wr.write("confidence=");
				wr.write(URLEncoder.encode(spotlightConfidence, "UTF-8"));
				wr.write('&');
			}
			if (spotlightSparql != null && !spotlightSparql.isEmpty()
					&& spotlightTypesRestriction == null) {
				wr.write("sparql=");
				wr.write(URLEncoder.encode(spotlightSparql, "UTF-8"));
				wr.write('&');
			}
			wr.write("text=");
			wr.write(URLEncoder.encode(text, "UTF-8"));
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
		InputStream is = null;
		Document xmlDoc;
		try {
			// Get Response
			 is = connection.getInputStream();
			xmlDoc = loadXMLFromInputStream(is);
		} catch (IOException e) {
			throw new EngineException("Unable to spot Entities with"
					+ "Dbpedia Spotlight Annotate RESTful Serice running at "
					+ spotlightUrl,e);
		} catch(SAXException e) {
			throw new EngineException("Unable to parse Response from "
					+ "Dbpedia Spotlight Annotate RESTful Serice running at "
					+ spotlightUrl,e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return Annotation.parseAnnotations(xmlDoc);
	}



	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(
				ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}
}
