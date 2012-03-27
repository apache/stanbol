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
package org.apache.stanbol.contenthub.web.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDProgramCollection;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.featured.DocumentResult;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.sinaci
 * @author meric
 * 
 */
public class JSONUtils {

    private static final Logger logger = LoggerFactory.getLogger(JSONUtils.class);

    /**
     * This function only operates on one-level JSON objects. Nested constraints cannot be processed.
     * 
     * @param jsonFields
     * @return
     */
    public static Map<String,List<Object>> convertToMap(String jsonFields) {
        Map<String,List<Object>> fieldMap = new HashMap<String,List<Object>>();
        if (jsonFields == null) return fieldMap;
        try {
            JSONObject jObject = new JSONObject(jsonFields);

            @SuppressWarnings("unchecked")
            Iterator<String> itr = jObject.keys();
            while (itr.hasNext()) {
                List<Object> valueSet = new ArrayList<Object>();
                String jFieldKey = itr.next();
                Object jFieldValue = jObject.get(jFieldKey);
                if (jFieldValue instanceof JSONArray) {
                    JSONArray jArray = (JSONArray) jFieldValue;
                    for (int i = 0; i < jArray.length(); i++) {
                        if (!jArray.get(i).equals(null)) {
                            valueSet.add(jArray.get(i));
                        }
                    }
                } else {
                    if (!jFieldValue.equals(null)) {
                        valueSet.add(jFieldValue);
                    }
                }

                if (jFieldKey != null && !jFieldKey.equals("")) {
                    fieldMap.put(jFieldKey, valueSet);
                }
            }
            return fieldMap;

        } catch (JSONException e) {
            logger.error("Cannot parse Json in generating the search constraints", e);
        }

        return null;
    }

    public static String convertToString(Map<String,List<Object>> constraints) {
        JSONObject jObject = new JSONObject();
        if (constraints != null) {
            for (Entry<String,List<Object>> constaint : constraints.entrySet()) {
                Collection<Object> collection = new ArrayList<Object>();
                for (Object obj : constaint.getValue()) {
                    collection.add(obj);
                }
                if (!constaint.getValue().isEmpty()) {
                    try {
                        jObject.put(constaint.getKey(), collection);
                    } catch (JSONException e) {
                        logger.error("Cannot parse values for key {}", constaint.getKey(), e);
                    }
                }

            }
        }
        return jObject.toString();
    }

    private static JSONObject toJSON(DocumentResult resultantDocument) throws JSONException {
        JSONObject jObj = new JSONObject();
        if (resultantDocument != null) {
            jObj.put("uri", resultantDocument.getDereferencableURI());
            jObj.put("localid", resultantDocument.getLocalId());
            jObj.put("mimetype", resultantDocument.getMimetype());
            jObj.put("title", resultantDocument.getTitle());
            jObj.put("enhancementcount", resultantDocument.getEnhancementCount());
        }
        return jObj;
    }

    private static <T> JSONArray toJSON(List<T> list) throws JSONException {
        JSONArray jArr = new JSONArray();
        if (list != null) {
            for (T element : list) {
                if (DocumentResult.class.isAssignableFrom(element.getClass())) {
                    jArr.put(toJSON((DocumentResult) element));
                } else if (FacetField.class.isAssignableFrom(element.getClass())) {
                    jArr.put(toJSON((FacetField) element));
                } else if (FacetField.Count.class.isAssignableFrom(element.getClass())) {
                    jArr.put(toJSON((FacetField.Count) element));
                } else if (RelatedKeyword.class.isAssignableFrom(element.getClass())) {
                    jArr.put(toJSON((RelatedKeyword) element));
                }
            }
        }
        return jArr;
    }

    private static JSONObject toJSON(RelatedKeyword relatedKeyword) throws JSONException {
        JSONObject jObj = new JSONObject();
        if (relatedKeyword != null) {
            jObj.put("keyword", relatedKeyword.getKeyword());
            jObj.put("score", relatedKeyword.getScore());
            // no need to put the source because it is already indicated at the start of the map
            // jObj.put("source", relatedKeyword.getSource());
        }
        return jObj;
    }

    private static JSONObject toJSON(FacetField.Count count) throws JSONException {
        JSONObject jObj = new JSONObject();
        if (count != null) {
            jObj.put("name", count.getName());
            jObj.put("count", count.getCount());
        }
        return jObj;
    }

    private static JSONObject toJSON(FacetField facet) throws JSONException {
        JSONObject jObj = new JSONObject();
        if (facet != null) {
            jObj.put("name", facet.getName());
            jObj.put("values", toJSON(facet.getValues()));
        }
        return jObj;
    }

    @SuppressWarnings("unchecked")
    private static <V> JSONArray toJSON(Map<String,V> map) throws JSONException {
        JSONArray jArr = new JSONArray();
        if (map != null) {
            for (Entry<String,V> entry : map.entrySet()) {
                JSONObject jObj = new JSONObject();
                if (Map.class.isAssignableFrom(entry.getValue().getClass())) {
                    jObj.put(entry.getKey(), toJSON((Map<String,List<RelatedKeyword>>) entry.getValue()));
                } else if (List.class.isAssignableFrom(entry.getValue().getClass())) {
                    jObj.put(entry.getKey(), toJSON((List<RelatedKeyword>) entry.getValue()));
                }
                jArr.put(jObj);
            }
        }
        return jArr;
    }

    public static String createJSONString(SearchResult searchResult) throws JSONException {
        JSONObject jObj = new JSONObject();
        if (searchResult != null) {
            jObj.put("documents", toJSON(searchResult.getDocuments()));
            jObj.put("facets", toJSON(searchResult.getFacets()));
            jObj.put("relatedkeywords", toJSON(searchResult.getRelatedKeywords()));
        }
        return jObj.toString(4);
    }
    
    public static String createJSONString(SolrContentItem sci) throws JSONException {
        String content = null;
        try {
            content = IOUtils.toString(sci.getStream(), Constants.DEFAULT_ENCODING);
        } catch (IOException ex) {
            logger.error("Cannot read the content.", ex);
        }

        JSONObject jObj = new JSONObject(sci.getConstraints());
        jObj.put("content", content);
        jObj.put("mimeType", sci.getMimeType());
        jObj.put("uri", ContentItemIDOrganizer.detachBaseURI(sci.getUri().getUnicodeString()));
        jObj.put("title", sci.getTitle());
        return jObj.toString(4);
    }

    public static String createJSONString(LDProgramCollection ldpc) throws JSONException {
        JSONObject jObj = new JSONObject();
        for (Map.Entry<String,String> entry : ldpc.asMap().entrySet()) {
            jObj.put(entry.getKey(), entry.getValue());
        }
        return jObj.toString(4);
    }

}
