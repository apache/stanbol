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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonLdProfileParser extends JsonLdParserCommon {

    private static Logger logger = LoggerFactory.getLogger(JsonLdProfileParser.class);

    public static JsonLdProfile parseProfile(String jsonLdProfileString) throws Exception {
        logger.debug("Parsing {} " + jsonLdProfileString);
        
        JsonLdProfile profile = null;

        JSONObject jo = parseJson(jsonLdProfileString);
        if (jo != null) {
            if (jo.has(JsonLdCommon.CONTEXT)) {
                JSONObject ctx = jo.getJSONObject(JsonLdCommon.CONTEXT);
                if (ctx.has(JsonLdCommon.TYPES)) {
                    profile = new JsonLdProfile();
                    parseSubject(jo, profile, 1);
                }
            } else {
                logger.error("A JSON-LD Profile must have a context element " + JsonLdCommon.CONTEXT);
                throw new IllegalArgumentException("A JSON-LD Profile must have a context element "
                                                   + JsonLdCommon.CONTEXT);
            }
        } else {
            logger.error("Could not parse JSON-LD Profile '{}'" + jsonLdProfileString);
            throw new IllegalArgumentException("Could not parse JSON-LD Profile String");
        }

        return profile;
    }

    /**
     * Parses a single subject.
     * 
     * @param jo
     *            JSON object that holds the subject's data.
     * @param profile
     *            JsonLd object to add the created subject resource.
     */
    private static void parseSubject(JSONObject jo, JsonLdProfile profile, int bnodeCount) throws Exception {
        if (jo.has(JsonLdCommon.CONTEXT)) {
            JSONObject context = jo.getJSONObject(JsonLdCommon.CONTEXT);
            for (int i = 0; i < context.names().length(); i++) {
                String name = context.names().getString(i).toLowerCase();
                if (name.equals(JsonLdCommon.TYPES)) {
                    JSONObject typesObject = context.getJSONObject(name);
                    for (int j = 0; j < typesObject.names().length(); j++) {
                        String property = typesObject.names().getString(j);

                        Object typeObject = typesObject.get(property);
                        if (typeObject instanceof String) {
                            String typeStr = (String) typeObject;
                            profile.addType(property, typeStr);
                        } else if (typeObject instanceof JSONArray) {
                            JSONArray typesArray = (JSONArray) typeObject;
                            for (int t = 0; t < typesArray.length(); t++) {
                                profile.addType(property, typesArray.getString(t));
                            }
                        }
                    }
                } else {
                    profile.addNamespacePrefix(context.getString(name), name);
                }
            }

            jo.remove(JsonLdCommon.CONTEXT);
        }
    }

}
