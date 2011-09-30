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
package org.apache.stanbol.commons.jsonld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JsonLdParser can be used to parse a given JSON-LD String representation
 * into a JSON-LD data structure.
 * 
 * @author Fabian Christ
 */
public class JsonLdParser extends JsonLdParserCommon {

	private static Logger logger = LoggerFactory.getLogger(JsonLdParser.class);

	/**
	 * Parse the given String into a JSON-LD data structure.
	 * 
	 * @param jsonLdString
	 *            A JSON-LD String.
	 * @return JSON-LD data structure.
	 */
	public static JsonLd parse(String jsonLdString) throws Exception {
		JsonLd jld = null;

		JSONObject jo = parseJson(jsonLdString);
		if (jo != null) {
			jld = new JsonLd();
			parseSubject(jo, jld, 1, null);
		}

		return jld;
	}

	/**
	 * Parses a single subject.
	 * 
	 * @param jo
	 *            JSON object that holds the subject's data.
	 * @param jld
	 *            JsonLd object to add the created subject resource.
	 */
	private static void parseSubject(JSONObject jo, JsonLd jld, int bnodeCount,
			String profile) {

		// The root subject is used for cases where no explicit subject is
		// specified. We need
		// at least one dummy subject (bnode) to support type coercion because
		// types are assigned to
		// subjects.
		JsonLdResource subject = new JsonLdResource();

		try {
			if (jo.has(JsonLdCommon.CONTEXT)) {
				JSONObject context = jo.getJSONObject(JsonLdCommon.CONTEXT);
				for (int i = 0; i < context.names().length(); i++) {
					String name = context.names().getString(i).toLowerCase();
					if (name.equals(JsonLdCommon.COERCE)) {
						JSONObject typeObject = context.getJSONObject(name);
						for (int j = 0; j < typeObject.names().length(); j++) {
							String property = typeObject.names().getString(j);
							String type = typeObject.getString(property);
							subject.putPropertyType(property, type);
						}
					} else {
						jld.addNamespacePrefix(context.getString(name), name);
					}
				}

				jo.remove(JsonLdCommon.CONTEXT);
			}

			// If there is a local profile specified for this subject, we
			// use that one. Otherwise we assign the profile given by the
			// parameter.
			if (jo.has(JsonLdCommon.PROFILE)) {
				String localProfile = unCURIE(jo
						.getString(JsonLdCommon.PROFILE), jld
						.getNamespacePrefixMap());
				profile = localProfile;
				jo.remove(JsonLdCommon.PROFILE);
			}
			subject.setProfile(profile);

			if (jo.has(JsonLdCommon.SUBJECT)) {
				// Check for N subjects
				Object subjectObject = jo.get(JsonLdCommon.SUBJECT);
				if (subjectObject instanceof JSONArray) {
					// There is an array of subjects. We create all subjects
					// in sequence.
					JSONArray subjects = (JSONArray) subjectObject;
					for (int i = 0; i < subjects.length(); i++) {
						parseSubject(subjects.getJSONObject(i), jld,
								bnodeCount++, profile);
					}
				} else {
					String subjectName = unCURIE(jo
							.getString(JsonLdCommon.SUBJECT), jld
							.getNamespacePrefixMap());
					subject.setSubject(subjectName);
				}
				jo.remove(JsonLdCommon.SUBJECT);
			} else {
				// No subject specified. We create a dummy bnode
				// and add this subject.
				subject.setSubject("_:bnode" + bnodeCount);
				jld.put(subject.getSubject(), subject);
			}

			// Iterate through the rest of properties and unCURIE property
			// values
			// depending on their type
			if (jo.names() != null && jo.names().length() > 0) {
				for (int i = 0; i < jo.names().length(); i++) {
					String property = jo.names().getString(i);
					Object valueObject = jo.get(property);
					handleProperty(jld, subject, property, valueObject);
				}
			}

		} catch (JSONException e) {
			logger.error(
					"There were JSON problems when parsing the JSON-LD String",
					e);
			e.printStackTrace();
		}
	}

	private static void handleProperty(JsonLd jld, JsonLdResource subject,
			String property, Object valueObject) {
		if (valueObject instanceof JSONObject) {
			JSONObject jsonValue = (JSONObject) valueObject;
			subject.putProperty(property, convertToMapAndList(jsonValue, jld
					.getNamespacePrefixMap()));
		} else if (valueObject instanceof JSONArray) {
			JSONArray arrayValue = (JSONArray) valueObject;
			subject.putProperty(property, convertToMapAndList(arrayValue, jld
					.getNamespacePrefixMap()));
		} else if (valueObject instanceof String) {
			String stringValue = (String) valueObject;
			subject.putProperty(property, unCURIE(stringValue, jld
					.getNamespacePrefixMap()));
		} else {
			subject.putProperty(property, valueObject);
		}
	}

	/**
	 * Converts a JSON object into a Map or List data structure.
	 * 
	 * <p>
	 * The JSON-LD implementation is based on Map and List data types. If the
	 * input is a JSONObject, it will be converted into a Map&lt;String,
	 * Object>. If the input is a JSONArray, it will be converted into a
	 * List&lt;Object>. Otherwise the input will be returned untouched.
	 * 
	 * @param input
	 *            Object that will be converted.
	 * @return
	 */
	private static Object convertToMapAndList(Object input,
			Map<String, String> namespacePrefixMap) {
		if (input instanceof JSONObject) {
			JSONObject jo = (JSONObject) input;

			// Handle IRIs
			if (jo.has(JsonLdCommon.IRI)) {
				try {
					return new JsonLdIRI(unCURIE(
							jo.getString(JsonLdCommon.IRI), namespacePrefixMap));
				} catch (JSONException e) {
					return null;
				}
			} else {
				// Handle arbitrary JSON
				return convertToMap(jo, namespacePrefixMap);
			}
		} else if (input instanceof JSONArray) {
			JSONArray ao = (JSONArray) input;
			return convertToList(ao, namespacePrefixMap);
		} else if (input instanceof String) {
			return unCURIE((String) input, namespacePrefixMap);
		} else {
			return input;
		}
	}

	/**
	 * Converts a JSONOBject into a Map&lt;String, Object>.
	 * 
	 * @param jo
	 *            JSONOBject to be converted.
	 * @return A Map that represents the same information as the JSONOBject.
	 */
	private static Map<String, Object> convertToMap(JSONObject jo,
			Map<String, String> namespacePrefixMap) {
		Map<String, Object> jsonMap = null;
		try {
			if (jo.names() != null && jo.names().length() > 0) {
				jsonMap = new HashMap<String, Object>();
				for (int i = 0; i < jo.names().length(); i++) {
					String name = jo.names().getString(i);
					jsonMap.put(name, convertToMapAndList(jo.get(name),
							namespacePrefixMap));
				}
			}
		} catch (JSONException e) { /* ignored */
		}
		return jsonMap;
	}

	private static List<Object> convertToList(JSONArray arrayValue,
			Map<String, String> namespacePrefixMap) {

		List<Object> values = new ArrayList<Object>();
		for (int i=0; i<arrayValue.length(); i++) {
			try {
				values.add(convertToMapAndList(arrayValue.get(i), namespacePrefixMap));
			} catch (JSONException e) {
				logger.error("Error converting JSONArray to list", e);
			}
		}
		
		return values;
	}
	
	/**
	 * Replaces the CURIE prefixes with the namespace to create full qualified
	 * IRIs.
	 * 
	 * @param curie
	 *            The CURIE to create an IRI from.
	 * @param namespacePrefixMap
	 *            A Map with known namespaces.
	 * @return
	 */
	private static String unCURIE(String curie,
			Map<String, String> namespacePrefixMap) {
		for (String namespace : namespacePrefixMap.keySet()) {
			String prefix = namespacePrefixMap.get(namespace) + ":";
			curie = curie.replaceAll(prefix, namespace);
		}
		return curie;
	}
}
