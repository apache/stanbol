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
package org.apache.stanbol.ontologymanager.ontonet;

import org.semanticweb.owlapi.model.IRI;

/**
 * Physical and logical IRIs for unit tests.
 */
public class Locations {

    /**
     * Default physical location of the ontology registry for testing.
     */
    public static final IRI _CP_NAMESPACE = IRI.create("http://www.ontologydesignpatterns.org/cp/owl/");

    /**
     * Default physical location of the ontology registry for testing.
     */
    public static final IRI _REGISTRY_TEST = IRI
            .create("http://www.ontologydesignpatterns.org/registry/krestest.owl");

    /**
     * Identifier of test ontology library 1.
     */
    public static final IRI LIBRARY_TEST1 = IRI.create(_REGISTRY_TEST + "#TestRegistry");

    /**
     * Identifier of test ontology library 2.
     */
    public static final IRI LIBRARY_TEST2 = IRI.create(_REGISTRY_TEST + "#TestLibrary2");

    /**
     * An ontology in test libraries 1 and 2.
     */
    public static final IRI ODP_OBJECTROLE = IRI.create(_CP_NAMESPACE + "objectrole.owl");

    /**
     * An ontology in test library 2 but not in test library 1.
     */
    public static final IRI ODP_SITUATION = IRI.create(_CP_NAMESPACE + "situation.owl");

    /**
     * An ontology in test library 1 but not in test library 2.
     */
    public static final IRI ODP_TYPESOFENTITIES = IRI.create(_CP_NAMESPACE + "typesofentities.owl");

}
