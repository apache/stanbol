package org.apache.stanbol.enhancer.servicesapi.rdf;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Classes to be used as types for resources that are not real life entities but
 * technical data modeling for Stanbol Enhancer components.
 *
 * @author ogrisel
 */
public class TechnicalClasses {

    /**
     * Type used for all enhancement created by Stanbol Enhancer
     */
    public static final UriRef ENHANCER_ENHANCEMENT = new UriRef(
            NamespaceEnum.enhancer+"Enhancement");

    /**
     * Type used for annotations on Text created by Stanbol Enhancer. This type is intended
     * to be used in combination with ENHANCER_ENHANCEMENT
     */
    public static final UriRef ENHANCER_TEXTANNOTATION = new UriRef(
            NamespaceEnum.enhancer+"TextAnnotation");

    /**
     * Type used for annotations of named entities. This type is intended
     * to be used in combination with ENHANCER_ENHANCEMENT
     */
    public static final UriRef ENHANCER_ENTITYANNOTATION = new UriRef(
            NamespaceEnum.enhancer+"EntityAnnotation");

    /**
     * To be used as a type pour any semantic knowledge extraction
     */
    @Deprecated
    public static final UriRef ENHANCER_EXTRACTION = new UriRef(
            "http://iks-project.eu/ns/enhancer/extraction/Extraction");

    /**
     * To be used as a complement type for extraction that are relevant only to
     * the portion of context item (i.e. a sentence, an expression, a word)
     * TODO: rwesten: Check how this standard can be used for Stanbol Enhancer enhancements
     * @deprecated
     */
    @Deprecated
    public static final UriRef ANNOTEA_ANNOTATION = new UriRef(
            "http://www.w3.org/2000/10/annotation-ns#Annotation");

    /**
     * To be used to type the URI of the content item being annotated by Stanbol Enhancer
     */
    public static final UriRef FOAF_DOCUMENT = new UriRef(
            NamespaceEnum.foaf + "Document");

    /**
     * Used to indicate, that an EntityAnnotation describes an Categorisation.
     * see <a href="http://wiki.iks-project.eu/index.php/ZemantaEnhancementEngine#Mapping_of_Categories">
     * Mapping of Categories</a> for more Information)
     */
    public static final UriRef ENHANCER_CATEGORY = new UriRef(
            NamespaceEnum.enhancer + "Category");

    private TechnicalClasses() {
    }

}
