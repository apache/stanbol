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

public enum NumberFeature {
    /**
     * MULTEXT-East feature Number="count" (Nouns in Serbian, Macedonian, Bulgarian), 
     * e.g., Bulgarian яка/як, язовира/язовир, яда/яд, юргана/юрган, юбилея/юбилей, 
     * ъгъла/ъгъл (http://purl.org/olia/mte/multext-east.owl#CountNumber)
     */
    CountNumber,
    /**
     * Plural is a grammatical number, typically referring to more than one of the referent in the real world.
     * In English, nouns, pronouns, and demonstratives inflect for plurality. In many other languages, for
     * example German and the various Romance languages, articles and adjectives also inflect for plurality.
     */
    Plural,
    /**
     * Singular is a grammatical number denoting a unit quantity (as opposed to the plural and other forms).
     */
    Singular,
    /**
     * A collective number is a number referring to 'a set of things'. Languages that have this feature can
     * use it to get a phrase like 'flock of sheeps' by using 'sheep' in collective number.
     */
    Collective,
    /**
     * Form used in some languages to designate two persons or things.
     */
    Dual,
    /**
     * Number that specifies 'a few' things.
     */
    Paucal,
    /**
     * Property related to four elements.
     */
    Quadrial,
    /**
     * Grammatical number referring to 'three things', as opposed to 'singular' and 'plural'.
     */
    Trial;
    static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";
    IRI uri;

    NumberFeature() {
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
