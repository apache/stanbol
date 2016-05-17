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
package org.apache.stanbol.enhancer.engines.dbpspotlight.candidates;

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
import java.util.Iterator;
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
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.CandidateResource;
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
 * {@link DBPSpotlightCandidatesEnhancementEngine} provides functionality to
 * enhance document with their language.
 * 
 * @author Iavor Jelev, Babelmonkeys (GzEvD)
 */
@Component(metatype = true, immediate = true, label = "%stanbol.DBPSpotlightCandidatesEnhancementEngine.name", description = "%stanbol.DBPSpotlightCandidatesEnhancementEngine.description")
@Service
@Properties(value = { 
		@Property(name = EnhancementEngine.PROPERTY_NAME, value = "dbpspotlightcandidates"),
		@Property(name = PARAM_URL_KEY, value = "http://spotlight.dbpedia.org/rest/candidates"),
		@Property(name = PARAM_SPOTTER),
		@Property(name = PARAM_DISAMBIGUATOR),
		@Property(name = PARAM_RESTRICTION),
		@Property(name = PARAM_SPARQL),
		@Property(name = PARAM_SUPPORT),
		@Property(name = PARAM_CONFIDENCE)
})
public class DBPSpotlightCandidatesEnhancementEngine extends
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
	 * <code>{@link ServiceProperties#ORDERING_CONTENT_EXTRACTION} -35</code>
	 */
	public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION - 35;


	/** This contains the logger. */
	private static final Logger log = LoggerFactory
			.getLogger(DBPSpotlightCandidatesEnhancementEngine.class);
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
	 * Used by OSGI to instantiate the engine. Expects 
	 * {@link #activate(ComponentContext)} to be called before usage
	 */
	public DBPSpotlightCandidatesEnhancementEngine(){}
	
	/**
	 * Used by unit tests
	 * @param spotlightUrl
	 */
	protected DBPSpotlightCandidatesEnhancementEngine(URL spotlightUrl,int connectionTimeout){
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

		// TODO initialize Extractor
		Dictionary<String, Object> properties = ce.getProperties();
		//parse the URL of the RESTful service
		spotlightUrl = SpotlightEngineUtils.parseSpotlightServiceURL(properties);
        connectionTimeout = SpotlightEngineUtils.getConnectionTimeout(properties);
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
	 * This generates enhancement structures for the entities from DBPedia
	 * Spotlight and adds them to the content item's metadata. For each surface
	 * form a TextAnnotation and the according EntityAnnotations are created.
	 * 
	 * @param occs
	 *            a Collection of entity information
	 * @param ci
	 *            the content item
	 */
	protected void createEnhancements(Collection<SurfaceForm> occs,
			ContentItem ci, String text, Language language) {

		// TODO create TextEnhancement (form, start, end, type?)
		HashMap<String, IRI> entityAnnotationMap = new HashMap<String, IRI>();

		Graph model = ci.getMetadata();
		for (SurfaceForm occ : occs) {
			IRI textAnnotation = SpotlightEngineUtils.createTextEnhancement(
					occ, this, ci, text, language);
			Iterator<CandidateResource> resources = occ.resources.iterator();
			while (resources.hasNext()) {
				CandidateResource resource = resources.next();
				IRI entityAnnotation = SpotlightEngineUtils.createEntityAnnotation(
						resource, this, ci, textAnnotation);
				entityAnnotationMap.put(resource.localName, entityAnnotation);
			}
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
	 *            Just used for logging 
	 * @return a <code>String</code> with the server response
	 * @throws EngineException
	 *             if the request cannot be sent
	 */
	protected Collection<SurfaceForm> doPostRequest(String text,IRI contentItemUri)
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
			if (spotlightSpotter != null && !spotlightSpotter.isEmpty()) {
				wr.write("spotter=");
				wr.write(URLEncoder.encode(spotlightSpotter, "UTF-8"));
				wr.write('&');
			}
			if (spotlightDisambiguator != null
					&& !spotlightDisambiguator.isEmpty()) {
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
			if (spotlightSupport != null && !spotlightSupport.isEmpty()){
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
					+ "Dbpedia Spotlight Spot RESTful Serice running at "
					+ spotlightUrl,e);
		} catch(SAXException e) {
			throw new EngineException("Unable to parse Response from "
					+ "Dbpedia Spotlight Spot RESTful Serice running at "
					+ spotlightUrl,e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return CandidateResource.parseCandidates(xmlDoc);
	}

	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(
				ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}


}
