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
package org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl;

import static org.apache.stanbol.enhancer.engines.celi.utils.Utils.getSelectionContext;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.xml.soap.SOAPException;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.CeliMorphoFeatures;
import org.apache.stanbol.enhancer.engines.celi.CeliTagSetRegistry;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.morpho.Case;
import org.apache.stanbol.enhancer.nlp.morpho.Gender;
import org.apache.stanbol.enhancer.nlp.morpho.NumberFeature;
import org.apache.stanbol.enhancer.nlp.morpho.Person;
import org.apache.stanbol.enhancer.nlp.morpho.Tense;
import org.apache.stanbol.enhancer.nlp.morpho.TenseTag;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMood;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service
@Properties(value = { 
		@Property(name = EnhancementEngine.PROPERTY_NAME, value = "celiLemmatizer"), 
		@Property(name = CeliConstants.CELI_LICENSE), 
		@Property(name = CeliConstants.CELI_TEST_ACCOUNT, boolValue = false),
	    @Property(name = CeliConstants.CELI_CONNECTION_TIMEOUT, intValue=CeliConstants.DEFAULT_CONECTION_TIMEOUT) 
})
public class CeliLemmatizerEnhancementEngine extends AbstractEnhancementEngine<IOException, RuntimeException> implements EnhancementEngine, ServiceProperties {
	// TODO: check if it is OK to define new properties in the FISE namespace
	public static final IRI hasLemmaForm = new IRI("http://fise.iks-project.eu/ontology/hasLemmaForm");

    /**
     * This ensures that no connections to external services are made if Stanbol is started in offline mode as the OnlineMode service will only be available if OfflineMode is deactivated.
     */
    @SuppressWarnings("unused")
    @Reference
	private OnlineMode onlineMode;

	private static List<String> supportedLangs = new Vector<String>();
	static {
		supportedLangs.add("it");
		supportedLangs.add("da");
		supportedLangs.add("de");
		supportedLangs.add("ru");
		supportedLangs.add("ro");
	}

	/**
	 * The literal representing the LangIDEngine as creator.
	 */
	public static final Literal LANG_ID_ENGINE_NAME = LiteralFactory.getInstance().createTypedLiteral("org.apache.stanbol.enhancer.engines.celi.langid.impl.CeliLanguageIdentifierEnhancementEngine");

	/**
	 * The default value for the Execution of this Engine. Currently set to {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
	 */
	public static final Integer defaultOrder = ServiceProperties.ORDERING_CONTENT_EXTRACTION;

	private Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * This contains the only MIME type directly supported by this enhancement engine.
	 */
	private static final String TEXT_PLAIN_MIMETYPE = "text/plain";

	/**
	 * Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
	 */
	private static final Set<String> SUPPORTED_MIMTYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);

	@Property(value = "http://linguagrid.org/LSGrid/ws/morpho-analyser")
	public static final String SERVICE_URL = "org.apache.stanbol.enhancer.engines.celi.lemmatizer.url";

	@Property(boolValue = false)
	public static final String MORPHOLOGICAL_ANALYSIS = "org.apache.stanbol.enhancer.engines.celi.lemmatizer.morphoAnalysis";

	private String licenseKey;
	private URL serviceURL;
	private boolean completeMorphoAnalysis;

	private LemmatizerClientHTTP client;

	@Override
	@Activate
	protected void activate(ComponentContext ctx) throws IOException, ConfigurationException {
		super.activate(ctx);
		Dictionary<String, Object> properties = ctx.getProperties();
		this.licenseKey = Utils.getLicenseKey(properties, ctx.getBundleContext());
		String url = (String) properties.get(SERVICE_URL);
		if (url == null || url.isEmpty()) {
			throw new ConfigurationException(SERVICE_URL, String.format("%s : please configure the URL of the CELI Web Service (e.g. by" + "using the 'Configuration' tab of the Apache Felix Web Console).", getClass().getSimpleName()));
		}
		this.serviceURL = new URL(url);

		try {
			this.completeMorphoAnalysis = (Boolean) properties.get(MORPHOLOGICAL_ANALYSIS);
		} catch (Exception e) {
			this.completeMorphoAnalysis = false;
		}
		int conTimeout = Utils.getConnectionTimeout(properties, ctx.getBundleContext());
		this.client = new LemmatizerClientHTTP(this.serviceURL, this.licenseKey, conTimeout);
	}

	@Override
	@Deactivate
	protected void deactivate(ComponentContext ce) {
		super.deactivate(ce);
	}

	@Override
	public int canEnhance(ContentItem ci) throws EngineException {
		String language = EnhancementEngineHelper.getLanguage(ci);
		if (language == null) {
			log.warn("Unable to enhance ContentItem {} because language of the Content is unknown." + "Please check that a language identification engine is active in this EnhancementChain).", ci.getUri());
		}
		if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES) != null && this.isLangSupported(language))
			return ENHANCE_ASYNC;
		else
			return CANNOT_ENHANCE;
	}

	@Override
	public void computeEnhancements(ContentItem ci) throws EngineException {
		String language = EnhancementEngineHelper.getLanguage(ci);
		if (!isLangSupported(language)) {
			throw new IllegalStateException("Call to computeEnhancement with unsupported language '" + language + " for ContentItem " + ci.getUri() + ": This is also checked " + "in the canEnhance method! -> This indicated an Bug in the "
					+ "implementation of the " + "EnhancementJobManager!");
		}

		Entry<IRI, Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
		if (contentPart == null) {
			throw new IllegalStateException("No ContentPart with Mimetype '" + TEXT_PLAIN_MIMETYPE + "' found for ContentItem " + ci.getUri() + ": This is also checked in the canEnhance method! -> This "
					+ "indicated an Bug in the implementation of the " + "EnhancementJobManager!");
		}
		String text;
		try {
			text = ContentItemHelper.getText(contentPart.getValue());
		} catch (IOException e) {
			throw new InvalidContentException(this, ci, e);
		}
		if (text.trim().length() == 0) {
			log.info("No text contained in ContentPart {" + contentPart.getKey() + "} of ContentItem {" + ci.getUri() + "}");
			return;
		}

		Graph graph = ci.getMetadata();

		if (this.completeMorphoAnalysis) {
			this.addMorphoAnalysisEnhancement(ci, text, language, graph);
		} else {
			this.addLemmatizationEnhancement(ci, text, language, graph);
		}
	}

	private void addMorphoAnalysisEnhancement(ContentItem ci, String text, String language, Graph g) throws EngineException {
		Language lang = new Language(language); // clerezza language for PlainLiterals
		List<LexicalEntry> terms;
		try {
			terms = this.client.performMorfologicalAnalysis(text, language);
		} catch (IOException e) {
			throw new EngineException("Error while calling the CELI Lemmatizer" + " service (configured URL: " + serviceURL + ")!", e);
		} catch (SOAPException e) {
			throw new EngineException("Error wile encoding/decoding the request/" + "response to the CELI lemmatizer service!", e);
		}
		// get a write lock before writing the enhancements
		ci.getLock().writeLock().lock();
		try {
			LiteralFactory literalFactory = LiteralFactory.getInstance();
			for (LexicalEntry le : terms) {

				List<CeliMorphoFeatures> mFeatures = this.convertLexicalEntryToMorphFeatures(le, language);
				for (CeliMorphoFeatures feat : mFeatures) {
					// Create a text annotation for each interpretation produced by the morphological analyzer
					IRI textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
					g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, new PlainLiteralImpl(le.getWordForm(), lang)));
					if (le.from >= 0 && le.to > 0) {
						g.add(new TripleImpl(textAnnotation, ENHANCER_START, literalFactory.createTypedLiteral(le.from)));
						g.add(new TripleImpl(textAnnotation, ENHANCER_END, literalFactory.createTypedLiteral(le.to)));
						g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_CONTEXT, new PlainLiteralImpl(getSelectionContext(text, le.getWordForm(), le.from), lang)));
					}
					g.addAll(feat.featuresAsTriples(textAnnotation, lang));
				}
			}
		} finally {
			ci.getLock().writeLock().unlock();
		}
	}

	private void addLemmatizationEnhancement(ContentItem ci, String text, String language, Graph g) throws EngineException {
		Language lang = new Language(language); // clerezza language for PlainLiterals
		String lemmatizedContents;
		try {
			lemmatizedContents = this.client.lemmatizeContents(text, language);
		} catch (IOException e) {
			throw new EngineException("Error while calling the CELI Lemmatizer" + " service (configured URL: " + serviceURL + ")!", e);
		} catch (SOAPException e) {
			throw new EngineException("Error wile encoding/decoding the request/" + "response to the CELI lemmatizer service!", e);
		}
		// get a write lock before writing the enhancements
		ci.getLock().writeLock().lock();
		try {
			IRI textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
			g.add(new TripleImpl(textEnhancement, CeliLemmatizerEnhancementEngine.hasLemmaForm, new PlainLiteralImpl(lemmatizedContents, lang)));
		} finally {
			ci.getLock().writeLock().unlock();
		}
	}

	private List<CeliMorphoFeatures> convertLexicalEntryToMorphFeatures(LexicalEntry le, String lang) {
		List<CeliMorphoFeatures> result = new Vector<CeliMorphoFeatures>();
		if (!le.termReadings.isEmpty()) {
			for (Reading r : le.termReadings) {
				CeliMorphoFeatures morphoFeature = CeliMorphoFeatures.parseFrom(r, lang);
				if(morphoFeature != null){
				    result.add(morphoFeature);
				}
			}
		}
		return result;
	}

	private boolean isLangSupported(String language) {
		if (supportedLangs.contains(language))
			return true;
		else
			return false;
	}

	@Override
	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}

}
