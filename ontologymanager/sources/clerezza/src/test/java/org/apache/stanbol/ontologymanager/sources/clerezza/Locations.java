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
package org.apache.stanbol.ontologymanager.sources.clerezza;

import org.semanticweb.owlapi.model.IRI;

/**
 * Physical and logical IRIs for unit tests.
 */
public class Locations {

    /**
     * Default namespace of Stanbol ontologies.
     */
    public static final IRI __STANBOL_ONT_NAMESPACE = IRI.create("http://stanbol.apache.org/ontologies/");

    /**
     * Default physical location of the ontology registry for testing.
     */
    public static final IRI _REGISTRY_TEST = IRI.create(__STANBOL_ONT_NAMESPACE + "registries/onmtest.owl");

    /**
     * Default physical location of the ontology registry for testing.
     */
    public static final IRI _REGISTRY_TEST_ADDITIONS = IRI.create(__STANBOL_ONT_NAMESPACE
                                                                  + "registries/onmtest_additions.owl");

    /**
     * An ontology in test library 1 but not in test library 2.
     */
    public static final IRI CHAR_ACTIVE = IRI.create(__STANBOL_ONT_NAMESPACE + "pcomics/characters_all.owl");

    /**
     * An ontology in test library 2 but not in test library 1.
     */
    public static final IRI CHAR_DROPPED = IRI.create(__STANBOL_ONT_NAMESPACE
                                                      + "pcomics/droppedcharacters.owl");

    /**
     * An ontology in test libraries 1 and 2.
     */
    public static final IRI CHAR_MAIN = IRI.create(__STANBOL_ONT_NAMESPACE + "pcomics/maincharacters.owl");

    public static final IRI CHAR_MINOR = IRI.create(__STANBOL_ONT_NAMESPACE + "pcomics/minorcharacters.owl");
    
    /**
     * Identifier of test ontology library 1.
     */
    public static final IRI LIBRARY_TEST1 = IRI.create(_REGISTRY_TEST + "#Library1");

    /**
     * Identifier of test ontology library 2.
     */
    public static final IRI LIBRARY_TEST2 = IRI.create(_REGISTRY_TEST + "#Library2");
    
    /**
     * Identifier of test ontology library 1.
     */
    public static final IRI ONT_TEST1 = IRI.create(__STANBOL_ONT_NAMESPACE + "test1.owl");

}
