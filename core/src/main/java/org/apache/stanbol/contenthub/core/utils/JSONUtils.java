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

package org.apache.stanbol.contenthub.core.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.contenthub.servicesapi.store.SolrContentItem;
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
        if (jsonFields == null) return null;
        try {
            Map<String,List<Object>> fieldMap = new HashMap<String,List<Object>>();
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

    public static String createJSONString(SolrContentItem sci) {
        String content = null;
        try {
            content = IOUtils.toString(sci.getStream(), "UTF-8");
        } catch (IOException ex) {
            logger.error("Cannot read the content.", ex);
        }

        JSONObject jObj = new JSONObject(sci.getConstraints());
        try {
            jObj.put("content", content);
            jObj.put("mimeType", sci.getMimeType());
            jObj.put("id", ContentItemIDOrganizer.detachBaseURI(sci.getUri().getUnicodeString()));
        } catch (JSONException e) {
            logger.error("Cannot create the JSON Object.", e);
        }

        return jObj.toString();
    }

}
