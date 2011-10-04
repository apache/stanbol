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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.hp.hpl.jena.ontology.OntModel;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * 
 * @author cihan
 * 
 */
public final class EntityHubClient {

    private static final String QUERY_SEPARATOR = "&";
    private static final String EQUALS = "=";
    private static final String QUERY = "?";
    private static final String DELIMITER = "/";
    // private static final String SYMBOL = "symbol";
    private static final String LOOKUP = "lookup";
    private static final String FIND = "find";

    private static final String PATH_SYMBOL_LOOKUP = LOOKUP;
    private static final String PATH_SYMBOL_FIND = FIND;

    private static final String PARAM_ID = "id";
    private static final String PARAM_CREATE = "create";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_FIELD = "field";
    private static final String PARAM_LANGUAGE = "lang";
    private static final String PARAM_SELECT = "select";

    private static final String MEDIA_TYPE_RDF = "application/rdf+xml";

    private String entityhubEndpoint;

    private Client client;

    private RDFUtil rdfUtil = RDFUtil.getInstance();

    public static final EntityHubClient getInstance(String entityhubEndpoint) {
        return new EntityHubClient(entityhubEndpoint);
    }

    private EntityHubClient(String entityhubEndpoint) {
        this.entityhubEndpoint = entityhubEndpoint;
        if (!this.entityhubEndpoint.endsWith(DELIMITER)) {
            this.entityhubEndpoint += DELIMITER;
        }

        this.client = Client.create();
    }

    private void nullCheck(Object obj, String parameterName) {
        if (obj == null) {
            throw new IllegalArgumentException("Parameter " + parameterName + "can not be null");
        }
    }

    public final OntModel symbolLookup(String id, Boolean create) {

        nullCheck(id, PARAM_ID);

        StringBuilder path = new StringBuilder(entityhubEndpoint);
        path.append(PATH_SYMBOL_LOOKUP).append(QUERY);
        path.append(PARAM_ID).append(EQUALS).append(id);
        if (create) {
            path.append(QUERY_SEPARATOR).append(PARAM_CREATE).append(EQUALS).append(Boolean.TRUE.toString());
        }

        WebResource wr = client.resource(path.toString());
        String response = wr.accept(MEDIA_TYPE_RDF).get(String.class);

        return rdfUtil.getOntModel(response);
    }

    public final OntModel symbolFind(String name, String field, String language, String... selects) {
        nullCheck(name, PARAM_NAME);
        StringBuilder path = new StringBuilder(entityhubEndpoint);
        path.append(PATH_SYMBOL_FIND);

        // Form parameters
        MultivaluedMap<String,String> form = new MultivaluedMapImpl();
        if (name != null && !name.isEmpty()) {
            form.add(PARAM_NAME, name);
        }
        if (field != null && !field.isEmpty()) {
            form.add(PARAM_FIELD, field);
        }
        if (language != null && !language.isEmpty()) {
            form.add(PARAM_LANGUAGE, language);
        }

        String selectStr = "";
        for (String select : selects) {
            if (select != null && !select.isEmpty()) {
                selectStr += select;
                selectStr += " ";
            }
        }
        form.add(PARAM_SELECT, selectStr);

        WebResource wr = client.resource(path.toString());
        String response = wr.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MEDIA_TYPE_RDF)
                .post(String.class, form);

        return rdfUtil.getOntModel(response);

    }

    public String getEntityhubEndpoint() {
        return entityhubEndpoint;
    }
}
