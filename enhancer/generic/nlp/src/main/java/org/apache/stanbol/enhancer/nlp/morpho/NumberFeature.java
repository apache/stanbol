package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.clerezza.rdf.core.UriRef;

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
    UriRef uri;

    NumberFeature() {
        uri = new UriRef(OLIA_NAMESPACE + name());
    }

    public UriRef getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri.getUnicodeString();
    }
}
