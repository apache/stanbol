package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Enumeration representing the different genders of words based on the <a
 * href="http://purl.org/olia/olia.owl">OLIA</a> Ontology
 * 
 */
public enum Gender {
    /**
     * One of the two grammatical genders, or classes of nouns, the other being inanimate. Membership in the
     * animate grammatical class is largely based on meanings, in that living things, including humans,
     * animals, spirits, trees, and most plants are included in the animate class of nouns
     */
    Animate("AnimateGender"),
    /**
     * Common is an optional attribute for nouns in EAGLES. The Common gender contrasts with Neuter in a
     * two-gender system e.g. Danish, Dutch. This value is also used for articles, pronouns and determiners
     * especially for Danish.
     */
    Common("CommonGender"),
    /**
     * Feminine gender is a grammatical gender that marks nouns, articles, pronouns, etc. that have human or
     * animal female referents, and often marks nouns that have referents that do not carry distinctions of
     * sex.
     */
    Feminine,
    /**
     * One of the two grammatical genders, or noun classes, of Nishnaabemwin, the other being animate.
     * Membership in the inanimate grammatical class is largely based on meaning, in that non-living things,
     * such as objects of manufacture and natural 'non-living' things are included in it
     */
    Inanimate("InanimateGender"),
    /**
     * Masculine gender is a grammatical gender that marks nouns, articles, pronouns, etc. having human or
     * animal male referents, and often marks nouns having referents that do not have distinctions of sex.
     */
    Masculine,
    /**
     * Neuter gender is a grammatical gender that includes those nouns, articles, pronouns, etc. having
     * referents which do not have distinctions of sex, and often includes some which do have a natural sex
     * distinction.
     */
    Neuter;
    static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";
    UriRef uri;

    Gender() {
        this(null);
    }

    Gender(String name) {
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
