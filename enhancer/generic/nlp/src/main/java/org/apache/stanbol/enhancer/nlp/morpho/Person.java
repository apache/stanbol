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

/**
 * Enumeration representing the different persons of words based on the <a*
 * href="http://purl.org/olia/olia.owl">OLIA</a> Ontology
 * 
 */
public enum Person {

    /**
     * Refers to the speaker and one or more nonparticipants, but not hearer(s). Contrasts with
     * FirstPersonInclusive (Crystal 1997: 285). (http://purl.oclc.org/linguistics/gold/First)
     */
    First("FirstPerson"),
    /**
     * Refers to the person(s) the speaker is addressing (Crystal 1997: 285).
     * (http://purl.oclc.org/linguistics/gold/Second)
     */
    Second("SecondPerson"),
    /**
     * Third person is deictic reference to a referent(s) not identified as the speaker or addressee. For
     * example in English "he", "she", "they" or the third person singular verb suffix -s, e.g. in
     * "Hesometimes flies."
     * (http://www.sil.org/linguistics/GlossaryOfLinguisticTerms/WhatIsThirdPersonDeixis.htm 20.11.06)
     */
    Third("ThirdPerson");

    static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";
    IRI uri;

    Person() {
        this(null);
    }

    Person(String name) {
        uri = new IRI(OLIA_NAMESPACE + (name == null ? name() : name));
    }

    public IRI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri.getUnicodeString();
    }

}
