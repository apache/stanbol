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
package org.apache.stanbol.enhancer.engines.geonames.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeonamesAPIWrapper {

    static final Logger log = LoggerFactory.getLogger(GeonamesAPIWrapper.class);
    /**
     * URI of the geonames.org web services used as default. Currently this is
     * the server that requires authentication with a free user account. The
     * server that allows anonymous requests is often overloaded and is therefore
     * no longer the default. Note that the anonymous server is still used in 
     * case this class is initialised with the default constructor.
     */
    public static final String DEFAULT_GEONAMES_ORG_WEBSERVICE_URL = "http://api.geonames.org/";
    /**
     * The geonames.org web service URI allowing anonymous connections. Often
     * overloaded. Will be used with the default constructor
     */
    public static final String ANONYMOUS_GEONAMES_ORG_WEBSERVICE_URL = "http://ws.geonames.org/";
    /**
     * Relative path to the search service
     */
    public static final String SEARCH_SERVICE_PATH = "search";
    /**
     * Relative path to the hierarchy service
     */
    public static final String HIERARCHY_SERVICE_PATH = "hierarchy";

    /**
     * The access url for the search service. As default
     * {@link #DEFAULT_GEONAMES_ORG_WEBSERVICE_URL}+{@link #SEARCH_SERVICE_PATH}
     * ({@value #DEFAULT_GEONAMES_ORG_WEBSERVICE_URL}{@value #SEARCH_SERVICE_PATH})
     * is used. Users with a premium account including an own sub domain
     * might need to change this.
     */
    protected String searchServiceUrl;
    /**
     * The access url for the hierarchy service. As default
     * {@link #DEFAULT_GEONAMES_ORG_WEBSERVICE_URL}+{@link #HIERARCHY_SERVICE_PATH}
     * ({@value #DEFAULT_GEONAMES_ORG_WEBSERVICE_URL}{@value #HIERARCHY_SERVICE_PATH})
     * is used. Users with a premium account including an own sub domain
     * might need to change this.
     */
    protected String hierarchyServiceUrl;
    /**
     * The username. Only set if used in combination with a premium account
     */
    private String userName;
    /**
     * The token. Only set if used in combination with a premium account
     */
    private String token;

    /**
     * Enumeration that contains the property definitions used by the LocationEnhancementEngine. This enum
     * does not define all properties defined by geonames.org
     *
     * @author Rupert Westenthaler
     */
    public enum SearchRequestPropertyEnum {
        /**
         * search Toponyms by name
         */
        name(true),
        //this two properties are not used
        //        /**
        //         * search toponyms by full text search over all properties
        //         */
        //        q,
        //        /**
        //         * enable/disable URIs of Linking Open Data entities
        //         */
        //        name_equals,
        /**
         * the maximum number of results (must be < 1000). Default is set to 5
         */
        maxRows("5"),
        /**
         * the index of the first result returned
         */
        startRow,
        /**
         * the ISO 3166 code of countries to restrict the search
         */
        country,
        /**
         * Restrict search to an continent
         */
        continentCode(null, "AF", "AS", "EU", "NA", "OC", "SA", "AN"),
        /**
         * level1 admin code to restrict the search
         */
        adminCode1,
        /**
         * level2 admin code to restrict the search
         */
        adminCode2,
        /**
         * level3 admin code to restrict the search
         */
        adminCode3,
        /**
         * Feature Class of searched entities (multiple possible)
         */
        featureClass(null, "A", "H", "L", "P", "R", "S", "T", "U", "V"),
        /**
         * Restrict search to one or more specific feature codes
         */
        featureCode,
        /**
         * The language of the parsed name. Also will use this language for the
         * returned name of found toponyms
         */
        lang,
        /**
         * The encoding of the results. Default is set to json. Geonames.org
         * would use XML as default if this parameter is not defined
         */
        type("json", "string", "xml", "json", "rdf"),
        /**
         * The detail level of the response. Default is set to FULL, because
         * the score of results is only present with this mode.
         */
        style("FULL", "SHORT", "MEDIUM", "LONG", "FULL"),
        /**
         * Can be used to parse the user name needed for premium accounts
         */
        username,
        /**
         * The token required for requests of premium accounts
         */
        token;
        private RequestProperty property;

        /**
         * An optional property with no default configuration and no value list
         */
        SearchRequestPropertyEnum() {
            this(false);
        }

        /**
         * a required or optional property
         *
         * @param required <code>true</code> if the property is a required one
         * or <code>false</code> for an optional property (in this case one can
         * also use the default constructor)
         */
        SearchRequestPropertyEnum(boolean required) {
            this(required, null);
        }

        /**
         * A optional property with a default configuration and a list of
         * allowed values
         *
         * @param defaultValue the value used if this parameter is not parsed or
         * <code>null</code> if there is no default value.
         * @param valueListthe list of allowed values for this parameter.
         */
        SearchRequestPropertyEnum(String defaultValue, String... valueList) {
            this(false, defaultValue, valueList);
        }

        SearchRequestPropertyEnum(boolean required, String defaultValue, String... valueList) {
            this.property = new RequestProperty(name(), required, defaultValue, valueList);
        }

        public RequestProperty getProperty() {
            return property;
        }

        @Override
        public String toString() {
            return property.toString();
        }
    }

    public enum HierarchyRequestPorpertyEnum {
        /**
         * The ID of the Toponym the hierarchy is requested for
         */
        geonameId(true),
        /**
         * The encoding of the results. Default is set to "json". geonames.org
         * would set the default to xml if this property is not defined
         */
        type("json", "xml", "json"),
        /**
         * Even not documented also the hierarchy service does support the
         * style parameter as documented for the search service.
         * Parse "FULL" to get all the alternate labels
         */
        style("FULL", "SHORT", "MEDIUM", "LONG", "FULL"),
        /**
         * Also not documented but supported is that the hierarchy service
         * supports the language attribute. This is especially interesting
         * because the name property of results will use the label in the parsed
         * language. The toponymName property will provide the preferred name!
         */
        lang,
        /**
         * Can be used to parse the user name needed for premium accounts
         */
        username,
        /**
         * The token required for requests of premium accounts
         */
        token;
        private RequestProperty property;

        /**
         * An optional property with no default configuration and no value list
         */
        HierarchyRequestPorpertyEnum() {
            this(false);
        }

        /**
         * A required or optional property
         *
         * @param required <code>true</code> if the property is a required one
         * or <code>false</code> for an optional property (in this case one can
         * also use the default constructor)
         */
        HierarchyRequestPorpertyEnum(boolean required) {
            this(required, null);
        }

        /**
         * A optional property with a default configuration and a list of
         * allowed values
         *
         * @param defaultValue the value used if this parameter is not parsed or
         * <code>null</code> if there is no default value.
         * @param valueListthe list of allowed values for this parameter.
         */
        HierarchyRequestPorpertyEnum(String defaultValue, String... valueList) {
            this(false, defaultValue, valueList);
        }

        HierarchyRequestPorpertyEnum(boolean required, String defaultValue, String... valueList) {
            this.property = new RequestProperty(name(), required, defaultValue, valueList);
        }

        public RequestProperty getProperty() {
            return property;
        }

        @Override
        public String toString() {
            return property.toString();
        }
    }

    /**
     * Initialises a the geonames API wrapper as used for the free service
     */
    public GeonamesAPIWrapper() {
        this(ANONYMOUS_GEONAMES_ORG_WEBSERVICE_URL + SEARCH_SERVICE_PATH,
                DEFAULT_GEONAMES_ORG_WEBSERVICE_URL + HIERARCHY_SERVICE_PATH,
                null, null);
    }

    /**
     * Initialise the geonames API wrapper for a given server, username and
     * token. Parsing <code>null</code> for any of the parameter will use the
     * default values. If an empty string is parsed as user name it will be
     * ignored. Token is only accepted if a valid user name is prased.
     *
     * @param serverURL The url of the geonames server to use or <code>null</code>
     * to use the default
     * @param userName The user name to use or <code>null</code> to use the
     * default. The user name MUST NOT be empty.
     * @param token The token to use or <code>null</code> to use the default.
     * If no valid user name is parsed the token will be ignored.
     */
    public GeonamesAPIWrapper(String serverURL, String userName, String token) {
        //if serverURL is null parse null to use the default
        //if one is parsed add the tailing "/" if missing
        this(serverURL == null ? null : (serverURL + (serverURL.endsWith("/") ? "" : "/") + SEARCH_SERVICE_PATH),
                serverURL == null ? null : (serverURL + (serverURL.endsWith("/") ? "" : "/") + HIERARCHY_SERVICE_PATH),
                userName, token);
    }

    /**
     * Initialise the geonames API wrapper for a given search and hierarchy
     * service, username and token. Parsing <code>null</code> for any of the
     * parameter will use the default values. If an empty string is parsed as
     * user name it will be ignored. Token is only accepted if a valid user name
     * is prased.
     * <p>
     * The parsed user name and token will be used for both the search and the
     * hierarchy service.
     *
     * @param searchService The url of the search service to use or <code>null</code>
     * to use the default
     * @param hierarchyService The url of the hierarchy service to use or
     * <code>null</code> to use the default
     * @param userName The user name to use or <code>null</code> to use the
     * default. The user name MUST NOT be empty.
     * @param token The token to use or <code>null</code> to use the default.
     * If no valid user name is parsed the token will be ignored.
     */
    public GeonamesAPIWrapper(String searchService, String hierarchyService, String userName, String token) {
        this.userName = userName == null || userName.isEmpty() ? null : userName;
        this.token = this.userName == null ? null : token;
        if(this.userName != null && this.token == null){
            throw new IllegalArgumentException("The Token MUST NOT be NULL nor empty of a User-Name is parsed!");
        }
        String defaultServer; //only used if the parsed service urls are null
        if(this.userName != null){
            defaultServer = DEFAULT_GEONAMES_ORG_WEBSERVICE_URL;
        } else {
            defaultServer = ANONYMOUS_GEONAMES_ORG_WEBSERVICE_URL;
        }
        this.searchServiceUrl = searchService != null ? searchService : (defaultServer + SEARCH_SERVICE_PATH);
        this.hierarchyServiceUrl = hierarchyService != null ? hierarchyService : (defaultServer + HIERARCHY_SERVICE_PATH);
    }

    public List<Toponym> searchToponyms(String name) throws IOException {
        return searchToponyms(Collections.singletonMap(SearchRequestPropertyEnum.name,
                (Collection<String>) Arrays.asList(name)));
    }

    public List<Toponym> searchToponyms(Map<SearchRequestPropertyEnum, Collection<String>> parsedParameter) throws IOException {
        //create a new map because we should not change the parsed map!
        Map<SearchRequestPropertyEnum, Collection<String>> requestProperties =
                new EnumMap<SearchRequestPropertyEnum, Collection<String>>(SearchRequestPropertyEnum.class);
        requestProperties.putAll(parsedParameter);
        if (userName != null && !requestProperties.containsKey(SearchRequestPropertyEnum.username)) {
            requestProperties.put(SearchRequestPropertyEnum.username, Collections.singleton(userName));
            //add the token only if also the user name was added
            // ... we would not like to use the token of an other user name
            if (token != null) {
                requestProperties.put(SearchRequestPropertyEnum.token, Collections.singleton(token));
            }
        }
        StringBuilder requestString = new StringBuilder();
        requestString.append(searchServiceUrl);
        boolean first = true;
        for (SearchRequestPropertyEnum entry : SearchRequestPropertyEnum.values()) {
            if (entry.getProperty().encode(requestString, first, requestProperties.get(entry)) && first) {
                first = false; // if the first parameter is added set first to false
            }
        }
        URL requestUrl;
        try {
            requestUrl = new URL(requestString.toString());
            log.info(" > search request: " + requestUrl);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Unable to build valid request URL for " + requestString);
        }
        long start = System.currentTimeMillis();
        String result = IOUtils.toString(requestUrl.openConnection().getInputStream());
        long responseTime = System.currentTimeMillis() - start;
        if (responseTime > 1000) {
            log.info("    - responseTime: " + responseTime + "ms");
        } else {
            log.debug("    - responseTime: " + responseTime + "ms");
        }
        try {
            JSONObject root = new JSONObject(result);
            if (root.has("totalResultsCount")) {
                long resultCount = root.getLong("totalResultsCount");
                if (resultCount < 1) {
                    return Collections.emptyList();
                } else {
                    if (root.has("geonames")) {
                        List<Toponym> results = new ArrayList<Toponym>();
                        JSONArray resultList = root.getJSONArray("geonames");
                        for (int i = 0; i < resultList.length(); i++) {
                            results.add(new Toponym(resultList.getJSONObject(i)));
                        }
                        return results;
                    } else {
                        throw new IOException(String.format("Result of Query for Toponyms with %s (resultCount=%s) does not contain any Toponym data",
                                requestProperties, resultCount));
                    }
                }
            } else { // illegal Response throw exception
                // test if we can get the error by parsing the status field
                String msg = null;
                if (root.has("status")) {
                    JSONObject status = root.getJSONObject("status");
                    if (status.has("message")) {
                        msg = status.getString("message");
                    }
                }
                if (msg == null) {
                    msg = "Unable to parse results form Response " + root;
                }
                throw new IOException(msg);
            }
        } catch (JSONException e) {
            log.error("Unable to parse Response for Request " + requestUrl);
            log.error("ResponseData: \n" + result);
            throw new IOException("Unable to parse JSON from Results for Request " + requestUrl, e);
        }
    }

    public List<Toponym> getHierarchy(int geonameId) throws IOException {
        StringBuilder requestString = new StringBuilder();
        requestString.append(hierarchyServiceUrl);
        Map<HierarchyRequestPorpertyEnum, Collection<String>> requestProperties =
                new EnumMap<HierarchyRequestPorpertyEnum, Collection<String>>(HierarchyRequestPorpertyEnum.class);
        requestProperties.put(HierarchyRequestPorpertyEnum.geonameId, Collections.singleton(Integer.toString(geonameId)));
        if (userName != null) {
            requestProperties.put(HierarchyRequestPorpertyEnum.username, Collections.singleton(userName));
            //add the token only if also the user name was added
            // ... we would not like to use the token of an other user name
            if (token != null) {
                requestProperties.put(HierarchyRequestPorpertyEnum.token, Collections.singleton(token));
            }
        }
        boolean first = true;
        for (HierarchyRequestPorpertyEnum entry : HierarchyRequestPorpertyEnum.values()) {
            if (entry.getProperty().encode(requestString, first, requestProperties.get(entry)) && first) {
                first = false; // if the first parameter is added set first to false
            }
        }
        URL requestUrl;
        try {
            requestUrl = new URL(requestString.toString());
            log.info(" > hierarchy request: " + requestUrl);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Unable to build valid request URL for " + requestString);
        }
        long start = System.currentTimeMillis();
        String result = IOUtils.toString(requestUrl.openConnection().getInputStream());
        long responseTime = System.currentTimeMillis() - start;
        if (responseTime > 1000) {
            log.info("    - responseTime: " + responseTime + "ms");
        } else {
            log.debug("    - responseTime: " + responseTime + "ms");
        }
        try {
            JSONObject root = new JSONObject(result);
            if (root.has("geonames")) {
                List<Toponym> results = new ArrayList<Toponym>();
                JSONArray resultList = root.getJSONArray("geonames");
                for (int i = 0; i < resultList.length(); i++) {
                    results.add(new Toponym(resultList.getJSONObject(i)));
                }
                return results;
            } else { // illegal Response throw exception
                // test if we can get the error by parsing the status field
                String msg = null;
                if (root.has("status")) {
                    JSONObject status = root.getJSONObject("status");
                    if (status.has("message")) {
                        msg = status.getString("message");
                    }
                }
                if (msg == null) {
                    msg = "Unable to parse results form Response " + root;
                }
                throw new IOException(msg);
            }
        } catch (JSONException e) {
            log.error("Unable to parse Response for Request " + requestUrl);
            log.error("ResponseData: \n" + result);
            throw new IOException("Unable to parse JSON from Results for Request " + requestUrl, e);
        }
    }

}
