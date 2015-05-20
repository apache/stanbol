package org.apache.stanbol.ontologymanager.sources.clerezza;

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
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;

/**
 * Utility class that provides some objects that would otherwise be provided by SCR reference in an OSGi
 * environment. Can be used to simulate OSGi in unit tests.
 * 
 * @author alexdma
 * 
 */
public class MockOsgiContext {

    private static Dictionary<String,Object> config;

    public static Parser parser;

    public static Serializer serializer;

    public static TcManager tcManager;

    static {
        config = new Hashtable<String,Object>();
        reset();
    }

    /**
     * Sets up a new mock OSGi context and cleans all resources and components.
     */
    public static void reset() {
        // reset Clerezza objects
        tcManager = new TcManager();
        tcManager.addWeightedTcProvider(new SimpleTcProvider());
        parser = Parser.getInstance();
        serializer = Serializer.getInstance();
    }

}
