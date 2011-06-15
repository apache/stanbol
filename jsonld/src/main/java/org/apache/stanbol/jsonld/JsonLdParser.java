package org.apache.stanbol.jsonld;

import java.util.HashMap;
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
public class JsonLdParser {

    private static Logger logger = LoggerFactory.getLogger(JsonLdParser.class);

    /**
     * Parse the given String into a JSON-LD data structure.
     * 
     * @param jsonLdString A JSON-LD String.
     * @return JSON-LD data structure.
     */
    public static JsonLd parse(String jsonLdString) {
        JsonLd jld = null;

        JSONObject jo = parseJson(jsonLdString);
        if (jo != null) {
            if (jo.has(JsonLd.CONTEXT)) {
                try {
                    JSONObject ctx = jo.getJSONObject(JsonLd.CONTEXT);
                    if (ctx.has(JsonLd.TYPES)) {
                        jld = new JsonLd(true);
                    }
                } catch (JSONException e) { /* ignore */}
            }
            if (jld == null) {
                jld = new JsonLd();
            }
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
    private static void parseSubject(JSONObject jo, JsonLd jld, int bnodeCount, String profile) {

        // The root subject is used for cases where no explicit subject is specified. We need
        // at least one dummy subject (bnode) to support type coercion because types are assigned to
        // subjects.
        JsonLdResource subject = new JsonLdResource();

        try {
            if (jo.has(JsonLd.CONTEXT)) {
                JSONObject context = jo.getJSONObject(JsonLd.CONTEXT);
                for (int i = 0; i < context.names().length(); i++) {
                    String name = context.names().getString(i).toLowerCase();
                    if (name.equals(JsonLd.TYPES)) {
                        JSONObject typeObject = context.getJSONObject(name);
                        for (int j = 0; j < typeObject.names().length(); j++) {
                            String property = typeObject.names().getString(j);
                            String type = typeObject.getString(property);
                            subject.putCoercionType(property, type);
                        }
                    } else {
                        jld.addNamespacePrefix(context.getString(name), name);
                    }
                }

                jo.remove(JsonLd.CONTEXT);
            }

            // If there is a local profile specified for this subject, we
            // use that one. Otherwise we assign the profile given by the parameter.
            if (jo.has(JsonLd.PROFILE)) {
                String localProfile = unCURIE(jo.getString(JsonLd.PROFILE), jld.getNamespacePrefixMap());
                profile = localProfile;
                jo.remove(JsonLd.PROFILE);
            }
            subject.setProfile(profile);

            if (jo.has(JsonLd.SUBJECT)) {
                // Check for N subjects
                Object subjectObject = jo.get(JsonLd.SUBJECT);
                if (subjectObject instanceof JSONArray) {
                    // There is an array of subjects. We create all subjects
                    // in sequence.
                    JSONArray subjects = (JSONArray) subjectObject;
                    for (int i = 0; i < subjects.length(); i++) {
                        parseSubject(subjects.getJSONObject(i), jld, bnodeCount++, profile);
                    }
                } else {
                    String subjectName = unCURIE(jo.getString(JsonLd.SUBJECT), jld.getNamespacePrefixMap());
                    subject.setSubject(subjectName);
                }
                jo.remove(JsonLd.SUBJECT);
            } else {
                // No subject specified. We create a dummy bnode
                // and add this subject.
                subject.setSubject("_:bnode" + bnodeCount);
                jld.put(subject.getSubject(), subject);
            }

            // Iterate through the rest of properties and unCURIE property values
            // depending on their type
            if (jo.names() != null && jo.names().length() > 0) {
                for (int i = 0; i < jo.names().length(); i++) {
                    String property = jo.names().getString(i);
                    Object valueObject = jo.get(property);
                    if (valueObject instanceof JSONObject) {
                        JSONObject jsonValue = (JSONObject) valueObject;
                        subject.putProperty(property, convertToMapAndList(jsonValue, jld
                                .getNamespacePrefixMap()));
                    } else if (valueObject instanceof String) {
                        String stringValue = (String) valueObject;
                        subject.putProperty(property, unCURIE(stringValue, jld.getNamespacePrefixMap()));
                    } else {
                        subject.putProperty(property, valueObject);
                    }
                }
            }

        } catch (JSONException e) {
            logger.error("There were JSON problems when parsing the JSON-LD String", e);
            e.printStackTrace();
        }
    }

    /**
     * Converts a JSON object into a Map or List data structure.
     * 
     * <p>
     * The JSON-LD implementation is based on Map and List data types. If the input is a JSONObject, it will
     * be converted into a Map&lt;String, Object>. If the input is a JSONArray, it will be converted into a
     * List&lt;Object>. Otherwise the input will be returned untouched.
     * 
     * @param input
     *            Object that will be converted.
     * @return
     */
    private static Object convertToMapAndList(Object input, Map<String,String> namespacePrefixMap) {
        if (input instanceof JSONObject) {
            JSONObject jo = (JSONObject) input;

            // Handle IRIs
            if (jo.has(JsonLd.IRI)) {
                try {
                    return new JsonLdIRI(unCURIE(jo.getString(JsonLd.IRI), namespacePrefixMap));
                } catch (JSONException e) {
                    return null;
                }
            }
            else {
                // Handle arbitrary JSON
                return convertToMap(jo, namespacePrefixMap);
            }
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
    private static Map<String,Object> convertToMap(JSONObject jo, Map<String,String> namespacePrefixMap) {
        Map<String,Object> jsonMap = null;
        try {
            if (jo.names() != null && jo.names().length() > 0) {
                jsonMap = new HashMap<String,Object>();
                for (int i = 0; i < jo.names().length(); i++) {
                    String name = jo.names().getString(i);
                    jsonMap.put(name, convertToMapAndList(jo.get(name), namespacePrefixMap));
                }
            }
        } catch (JSONException e) { /* ignored */}
        return jsonMap;
    }

    /**
     * Uses the underlying Jettison to parse a JSON object.
     * 
     * @param jsonString
     *            JSON String representation.
     * @return
     */
    private static JSONObject parseJson(String jsonString) {
        JSONObject jo = null;
        try {
            jo = new JSONObject(jsonString);
        } catch (JSONException e) {
            logger.debug("Could not parse JSON string: {}", jsonString, e);
        }

        return jo;
    }

    /**
     * Replaces the CURIE prefixes with the namespace to create full qualified IRIs.
     * 
     * @param curie
     *            The CURIE to create an IRI from.
     * @param namespacePrefixMap
     *            A Map with known namespaces.
     * @return
     */
    private static String unCURIE(String curie, Map<String,String> namespacePrefixMap) {
        for (String namespace : namespacePrefixMap.keySet()) {
            String prefix = namespacePrefixMap.get(namespace) + ":";
            curie = curie.replaceAll(prefix, namespace);
        }
        return curie;
    }
}
