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
package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.clerezza.commons.rdf.IRI;

public enum Definitness {
    /**
     * Value referring to the capacity of identification of an entity. (http://www.isocat.org/datcat/DC-2004)
     * <p>
     * An entity is specified as definite when it refers to a particularized individual of the species denoted
     * by the noun. (http://languagelink.let.uu.nl/tds/onto/LinguisticOntology.owl#definite)
     * <p>
     * Definite noun phrases are used to refer to entities which are specific and identifiable in a given
     * context. (http://en.wikipedia.org/wiki/Definiteness 20.11.06)
     */
    Definite,
    /**
     * An entity is specified as indefinite when it refers to a non-particularized individual of the species
     * denoted by the noun. (http://languagelink.let.uu.nl/tds/onto/LinguisticOntology.owl#indefinite)
     * <p>
     * Indefinite noun phrases are used to refer to entities which are not specific and identifiable in a
     * given context. (http://en.wikipedia.org/wiki/Definiteness 20.11.06)
     */
    Indefinite;
    static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";
    IRI uri;

    Definitness() {
        uri = new IRI(OLIA_NAMESPACE + name());
    }

    public IRI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri.getUnicodeString();
    }
}
