package org.apache.stanbol.ontologymanager.multiplexer.clerezza;

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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.clerezza.rdf.rdfjson.parser.RdfJsonParsingProvider;
import org.apache.clerezza.rdf.rdfjson.serializer.RdfJsonSerializingProvider;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.ontologymanager.core.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.core.scope.ScopeManagerImpl;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.collector.ClerezzaCollectorFactory;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology.ClerezzaOntologyProvider;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.session.SessionManagerImpl;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;

/**
 * Utility class that provides some objects that would otherwise be provided by SCR reference in an OSGi
 * environment. Can be used to simulate OSGi in unit tests.
 * 
 * @author alexdma
 * 
 */
public class MockOsgiContext {

    private static Dictionary<String,Object> config;

    public static OfflineConfiguration offline;

    public static ScopeManager onManager;

    public static OntologyProvider<TcProvider> ontologyProvider;

    public static ClerezzaCollectorFactory collectorfactory;

    public static Parser parser = Parser.getInstance();

    public static Serializer serializer = Serializer.getInstance();

    public static SessionManager sessionManager;

    public static TcManager tcManager = TcManager.getInstance();

    static {
        config = new Hashtable<String,Object>();
        config.put(OfflineConfiguration.DEFAULT_NS, "http://stanbol.apache.org/test/");
        config.put(SessionManager.MAX_ACTIVE_SESSIONS, "-1");
        offline = new OfflineConfigurationImpl(new Hashtable<String,Object>());
        reset();
    }

    /**
     * Sets up a new mock OSGi context and cleans all resources and components.
     */
    public static void reset() {
        // reset Clerezza objects
        tcManager = new TcManager();
        tcManager.addWeightedTcProvider(new SimpleTcProvider());

        // reset Stanbol objects
        ontologyProvider = new ClerezzaOntologyProvider(tcManager, offline, parser);
        collectorfactory = new ClerezzaCollectorFactory(ontologyProvider, config);
        resetManagers();
    }

    public static void resetManagers() {
        // PersistentCollectorFactory factory = new ClerezzaCollectorFactory(ontologyProvider, config);
        onManager = new ScopeManagerImpl(ontologyProvider, offline, collectorfactory, collectorfactory,
                config);
        sessionManager = new SessionManagerImpl(ontologyProvider, offline, config);
    }

}
