package org.apache.stanbol.enhancer.engines.dbpspotlight;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Property;

/**
 * Defines Properties used for the configuration of the different Engines
 */
public interface Constants {
	
	String PARAM_URL_KEY = "dbpedia.spotlight.url";

	String PARAM_SPOTTER = "dbpedia.spotlight.spotter";

	String PARAM_DISAMBIGUATOR = "dbpedia.spotlight.disambiguator";

	String PARAM_RESTRICTION = "dbpedia.spotlight.types";

	String PARAM_SPARQL = "dbpedia.spotlight.sparql";

	String PARAM_SUPPORT = "dbpedia.spotlight.support";

	String PARAM_CONFIDENCE = "dbpedia.spotlight.confidence";
	/**
	 * Cab be used to set both connection AND read timeout for Http requests
	 * to the configured DBpedia Spotlight services.<br>
	 * Supported by all DBpedia Spotlight Engines.
	 */
	String PARAM_CONNECTION_TIMEOUT = "dbpedia.spotlight.connection.timeout";
	
	/**
	 * The namespace used by DBpedia Spotlight specific properties 
	 */
	String SPOTLIGHT_NAME_SPACE = "http://spotlight.dbpedia.org/ns/";
	
	
	/*
	 * Definition of some Spotlight specific properties added to
	 * fise:EntityAnnotations created by this Engine
	 */
	UriRef PROPERTY_CONTEXTUAL_SCORE = new UriRef(
			SPOTLIGHT_NAME_SPACE + "contextualScore");
	UriRef PROPERTY_PERCENTAGE_OF_SECOND_RANK = new UriRef(
			SPOTLIGHT_NAME_SPACE + "percentageOfSecondRank");
	UriRef PROPERTY_SUPPORT = new UriRef(
			SPOTLIGHT_NAME_SPACE + "support");
	UriRef PROPERTY_PRIOR_SCORE = new UriRef(
			SPOTLIGHT_NAME_SPACE + "priorScore");
	UriRef PROPERTY_FINAL_SCORE = new UriRef(
			SPOTLIGHT_NAME_SPACE + "finalScore");
	UriRef PROPERTY_SIMILARITY_SCORE = new UriRef(
			SPOTLIGHT_NAME_SPACE + "similarityScore");
	
	Charset UTF8 = Charset.forName("UTF-8");
	/**
	 * This contains the only MIME type directly supported by this enhancement
	 * engine.
	 */
	String TEXT_PLAIN_MIMETYPE = "text/plain";
	/**
	 * This contains a list of languages supported by DBpedia Spotlight. If the
	 * metadata doesn't contain a value for the language as the value of the
	 * {@link Property.DC_LANG property} the content can't be processed.
	 */
	Set<String> SUPPORTED_LANGUAGES = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList("en")));

	
	/** Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE} */
	Set<String> SUPPORTED_MIMTYPES = Collections
			.singleton(TEXT_PLAIN_MIMETYPE);

}
