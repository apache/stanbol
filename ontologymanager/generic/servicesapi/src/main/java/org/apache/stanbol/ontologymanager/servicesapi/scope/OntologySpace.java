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
package org.apache.stanbol.ontologymanager.servicesapi.scope;

import org.apache.stanbol.ontologymanager.servicesapi.collector.Lockable;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OWLExportable;

/**
 * An ontology space identifies the set of OWL ontologies that should be "active" in a given context, e.g. for
 * a certain user session or a specific reasoning service. Each ontology space has an ID and a top ontology
 * that can be used as a shared resource for mutual exclusion and locking strategies.
 * 
 * @author alexdma
 */
public interface OntologySpace extends OntologyCollector, OWLExportable, Lockable {

    static final String shortName = "space";

    /**
     * The possible types of ontology spaces managed by OntoNet.
     */
    public enum SpaceType {

        /**
         * Denotes a core space (1..1). It is instantiated upon creation of the scope.
         */
        CORE("core"),

        /**
         * Denotes a custom space (0..1).
         */
        CUSTOM("custom");

        private String suffix;

        SpaceType(String suffix) {
            this.suffix = suffix;
        }

        /**
         * Returns the preferred string to be attached to an ontology scope IRI (assuming it ends with a hash
         * or slash character) in order to reference the included ontology space.
         * 
         * @return the preferred suffix for this space type.
         */
        public String getIRISuffix() {
            return suffix;
        }

    }

}
