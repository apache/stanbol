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
package org.apache.stanbol.commons.owl.util;

/**
 * A collection of OWL 2 vocabulary terms that integrates those provided by Clerezza.
 * 
 * @author alexdma
 * 
 */
public final class OWL2Constants {
   
    /**
     * Restrict instantiation
     */
    private OWL2Constants() {}

    /**
     * The namespace for the OWL language vocabulary.
     */
    public static final String _OWL_NS = "http://www.w3.org/2002/07/owl#";

    /**
     * The owl:versionIRI annotation property that applies to resources of type owl:Ontology in OWL 2.
     */
    public static final String OWL_VERSION_IRI = _OWL_NS + "versionIRI";

}
