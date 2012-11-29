package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.clerezza.rdf.core.UriRef;

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
    UriRef uri;

    Definitness() {
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
