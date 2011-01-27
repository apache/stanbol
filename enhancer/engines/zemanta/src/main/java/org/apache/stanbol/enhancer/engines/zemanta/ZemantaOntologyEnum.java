package org.apache.stanbol.enhancer.engines.zemanta;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Holds concepts, properties and instances found in the Zemanta ontology.
 * See also {@linkplain http://wiki.iks-project.eu/index.php/ZemantaEnhancementEngine}
 * for an overview on Zemanta annotations.
 *
 * @author Rupert Westenthaler
 */
public enum ZemantaOntologyEnum {

    // TODO: use capitals for constant names
    Recognition,
    anchor,
    confidence,
    doc,
    object,
    Object,
    target,
    owlSameAs("http://www.w3.org/2002/07/owl#", "sameAs"),
    Target,
    targetType, //some kind of content type
    targetType_RDF("http://s.zemanta.com/targets#", "rdf"),
    targetType_WIKIPWDIA("http://s.zemanta.com/targets#", "wikipedia"),
    targetType_CATEGORY("http://s.zemanta.com/targets#", "category"),
    targetType_ARTICLE("http://s.zemanta.com/targets#", "article"),
    title,
    Category,
    categorization,
    categorization_DMOZ("http://s.zemanta.com/cat/", "dmoz"),
    Related,
    zemified,
    Keyword,
    name,
    schema,;
    UriRef uri;

    /**
     * Creates n new entity of this Enum by using the parsed namespace and
     * local name.
     *
     * @param ns    The namespace or <code>null</code> to use the default
     * @param local The local name or <code>null</code> to use the default
     */
    private ZemantaOntologyEnum(String ns, String local) {
        uri = new UriRef((ns == null ? "http://s.zemanta.com/ns#" : ns) + (local == null ? name() : local));
    }

    /**
     * Creates a new entry of this Enum by using the parsed local name and the
     * default Zemanta namespace
     *
     * @param local the local name or <code>null</code> to use the name() of the
     *              element
     */
    private ZemantaOntologyEnum(String local) {
        this(null, local);
    }

    /**
     * Creates a new entry of this Enum by using the default Zemanta namespace
     * and the name() of this element as lecal name.
     */
    private ZemantaOntologyEnum() {
        this(null, null);
    }

    /**
     * The unicode name of the URI.
     *
     * @return the unicode name of the URI
     */
    @Override
    public String toString() {
        return uri.getUnicodeString();
    }

    /**
     * The URI of the element of this Enum.
     *
     * @return the URI of the element as Clerezza UriRef
     */
    public UriRef getUri() {
        return uri;
    }
}
