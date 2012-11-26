package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.clerezza.rdf.core.UriRef;

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
    UriRef uri;

    Person() {
        this(null);
    }

    Person(String name) {
        uri = new UriRef(OLIA_NAMESPACE + (name == null ? name() : name));
    }

    public UriRef getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri.getUnicodeString();
    }

}
