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
package org.apache.stanbol.enhancer.engines.celi;

import org.apache.clerezza.commons.rdf.IRI;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public interface CeliConstants {
    
    /**
     * Property used to provide the license key for the CELI service to all the
     * CELI engines. Keys need to be configured in the form '{user-name}:{password}'<p>
     * License keys are read from:<ol>
     * <li> {@link ComponentContext#getProperties()} - engine configuration: 
     * This can be used to configure a specific keys for single Engine
     * <li> {@link BundleContext#getProperty(String)} - system configuration:
     * This can be used to configure the key within the "sling.properties" file
     * or as a system property when starting the Stanbol instance.
     * </ol>
     * <b>Note</b><ul>
     * <li> License keys configures like that will be used by all CELI engines 
     * that do not provide there own key.
     * <li> If the License key is configured via a System property it can be
     * also accessed by other components.
     * </ul>
     */
    String CELI_LICENSE = "celi.license";
    /**
     * If this property is present and set to "true" engines will allow to use
     * the test account. This allows to test the CELI engines without requesting
     * an account on <a href="http://www.linguagrid.org">linguagrid.org</a><p>
     * NOTES: <ul>
     * <li> This can be parsed as configuration for a specific CELI engine, as
     * OSGI framework property or System property. If a {@link #CELI_LICENSE} is
     * present this property will be ignored.
     * <li> The test account does not require to configure a {@link #CELI_LICENSE}
     * <li>Requests are limited to 100 requests per day and IP address.
     * </ul>
     */
    String CELI_TEST_ACCOUNT = "celi.testaccount";
    
    String CELI_CONNECTION_TIMEOUT = "celi.connectionTimeout";
    /**
     * The default connection timeout for HTTP connections (30sec)
     */
    int DEFAULT_CONECTION_TIMEOUT = 30;

    /**
     * Concept used to annotate sentiment expressions within text
     *  TODO: Find standard ontology for reference or check if it is OK to define new properties in the FISE namespace
     */
 	IRI SENTIMENT_EXPRESSION = new IRI("http://fise.iks-project.eu/ontology/Sentiment Expression");
 	/**
     * Datatype property (targets double literals) used to represent the polarity of a sentiment expression
     *  TODO: Find standard ontology for reference or check if it is OK to define new properties in the FISE namespace
     */
 	IRI HAS_SENTIMENT_EXPRESSION_POLARITY=new IRI("http://fise.iks-project.eu/ontology/hasSentimentPolarityValue");
}
