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
package org.apache.stanbol.entityhub.yard.solr.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.apache.solr.client.solrj.impl.StreamingUpdateSolrServer;
import org.apache.stanbol.entityhub.yard.solr.SolrServerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link SolrServerProvider} interface supporting all the types directly supported by
 * the SolrJ library. This includes all Clients using an remote SolrServer.
 * <p>
 * This does not support an embedded SolrServer
 * 
 * @author Rupert Westenthaler
 * 
 */
@Component(immediate = true)
@Service
public class DefaultSolrServerProvider implements SolrServerProvider {

    private final Logger log = LoggerFactory.getLogger(DefaultSolrServerProvider.class);

    @Override
    public SolrServer getSolrServer(Type type, String uriOrPath, String... additional) throws NullPointerException,
                                                                                      IllegalArgumentException {
        if (uriOrPath == null) {
            throw new IllegalArgumentException("The parsed SolrServer URI MUST NOT be NULL!");
        }
        if (type == null) {
            type = Type.HTTP;
        } else if (type == Type.EMBEDDED) {
            throw new IllegalArgumentException(
                    String.format(
                        "The EmbeddedSolrServer (type=%s) is not supported by this SolrServerProvider implementation",
                        type));
        }
        final URL solrServerURL;
        try {
            solrServerURL = new URL(uriOrPath);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The parsed SolrServer location is not a valid URL", e);
        }
        if (type != Type.LOAD_BALANCE && additional != null && additional.length > 0) {
            log.warn(String.format(
                "The parsed SolrServer type \"%s\" does not support multiple SolrServer instaces."
                        + "The %s additional SolrServer locations parsed are ignored! (ignored Servers: %s)",
                type, additional.length, Arrays.toString(additional)));
        }
        switch (type) {
            case HTTP:
                return new CommonsHttpSolrServer(solrServerURL);
            case STREAMING:
                try {
                    return new StreamingUpdateSolrServer(uriOrPath, 10, 3);
                } catch (MalformedURLException e) {
                    // URL is already validated before!
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            case LOAD_BALANCE:
                Collection<String> solrServers = new ArrayList<String>();
                solrServers.add(uriOrPath); // the main server
                // check the additional server locations
                if (additional != null) {
                    for (String serverlocation : additional) {
                        if (serverlocation != null) {
                            try {
                                new URL(serverlocation);
                                solrServers.add(serverlocation);
                            } catch (MalformedURLException e) {
                                log.warn(
                                    String.format(
                                        "The parsed additional SolrServer %s is no valid URL. -> This location is ignored",
                                        serverlocation), e);
                            }
                        } // else ignore
                    }
                }
                try {
                    return new LBHttpSolrServer(solrServers.toArray(new String[solrServers.size()]));
                } catch (MalformedURLException e) {
                    // URLs are already validated before!
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            default:
                // write an error to clearly show that there is something wrong with this implementation
                log.error("This should never happen, because this class should eighter throw an IllegalArgumentException or support the type "
                          + type);
                throw new IllegalArgumentException("The parsed Type " + type + " is not supported");

        }
    }

    @Override
    public Set<Type> supportedTypes() {
        return EnumSet.of(Type.HTTP, Type.LOAD_BALANCE, Type.STREAMING);
    }

}
