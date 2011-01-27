package org.apache.stanbol.enhancer.servicesapi.helper;

import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EnhancementEngineHelper {

    protected static Random rng = new Random();

    private static final Logger log = LoggerFactory.getLogger(EnhancementEngineHelper.class);

    public static void setSeed(long seed) {
        rng.setSeed(seed);
    }

    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:TextAnnotation in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the UriRef of the extraction so that engines can further add.
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     *
     * @return the URI of the new enhancement instance
     */
    public static UriRef createTextEnhancement(ContentItem ci,
            EnhancementEngine engine){
        return createTextEnhancement(ci.getMetadata(), engine, new UriRef(ci.getId()));
    }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:TextAnnotation in the parsed graph along with default properties
     * (dc:creator, dc:created and enhancer:extracted-form) and return
     * the UriRef of the extraction so that engines can further add.
     *
     * @param metadata the graph
     * @param engine the engine
     * @param contentItemId the id
     *
     * @return the URI of the new enhancement instance
     */
    public static UriRef createTextEnhancement(MGraph metadata,
                EnhancementEngine engine, UriRef contentItemId){
        UriRef enhancement = createEnhancement(metadata, engine,contentItemId);
        //add the Text Annotation Type
        metadata.add(new TripleImpl(enhancement, Properties.RDF_TYPE,
                TechnicalClasses.ENHANCER_TEXTANNOTATION));
        return enhancement;
    }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:EntityAnnotation in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the UriRef of the extraction so that engines can further add
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     * @return the URI of the new enhancement instance
     */
    public static UriRef createEntityEnhancement(ContentItem ci,
            EnhancementEngine engine){
        return createEntityEnhancement(ci.getMetadata(), engine, new UriRef(ci.getId()));
    }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:EntityAnnotation in the parsed graph along with default properties
     * (dc:creator, dc:created and enhancer:extracted-form) and return
     * the UriRef of the extraction so that engines can further add.
     *
     * @param metadata the graph
     * @param engine the engine
     * @param contentItemId the id
     *
     * @return the URI of the new enhancement instance
     */
    public static UriRef createEntityEnhancement(MGraph metadata,
                EnhancementEngine engine, UriRef contentItemId){
        UriRef enhancement = createEnhancement(metadata, engine, contentItemId);
        metadata.add(new TripleImpl(enhancement, Properties.RDF_TYPE,
                TechnicalClasses.ENHANCER_ENTITYANNOTATION));
        return enhancement;
    }
    /**
     * Create a new enhancement instance in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the UriRef of the extraction so that engines can further add.
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     *
     * @return the URI of the new enhancement instance
     */
    protected static UriRef createEnhancement(MGraph metadata,
            EnhancementEngine engine, UriRef contentItemId){
        LiteralFactory literalFactory = LiteralFactory.getInstance();

        UriRef enhancement = new UriRef("urn:enhancement-"
                + EnhancementEngineHelper.randomUUID());
        //add the Enhancement Type
        metadata.add(new TripleImpl(enhancement, Properties.RDF_TYPE,
                TechnicalClasses.ENHANCER_ENHANCEMENT));
        //add the extracted from content item
        metadata.add(new TripleImpl(enhancement,
                Properties.ENHANCER_EXTRACTED_FROM, contentItemId));
        // creation date
        metadata.add(new TripleImpl(enhancement, Properties.DC_CREATED,
                literalFactory.createTypedLiteral(new Date())));

        // the engines that extracted the data
        // TODO: add some kind of versioning info for the extractor?
        // TODO: use a public dereferencing URI instead? that would allow for
        // explicit versioning too
        /* NOTE (Rupert Westenthaler 2010-05-26):
         * The Idea is to use the  ComponentContext in the activate() method of
         * an Enhancer to get the bundle name/version and use that as an
         * URI for the creator.
         * We would need to add getEnhancerID() method to the enhancer interface
         * to access this information
          */
        metadata.add(new TripleImpl(enhancement, Properties.DC_CREATOR,
                literalFactory.createTypedLiteral(engine.getClass().getName())));
        return enhancement;
    }
    /**
     * Create a new extraction instance in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the UriRef of the extraction so that engines can further add
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     * @return the URI of the new extraction instance
     * @deprecated
     * @see EnhancementEngineHelper#createEntityEnhancement(ContentItem, EnhancementEngine)
     * @see EnhancementEngineHelper#createTextEnhancement(ContentItem, EnhancementEngine)
     */
    @Deprecated
    public static UriRef createNewExtraction(ContentItem ci,
            EnhancementEngine engine) {
        LiteralFactory literalFactory = LiteralFactory.getInstance();

        MGraph metadata = ci.getMetadata();
        UriRef extraction = new UriRef("urn:extraction-"
                + EnhancementEngineHelper.randomUUID());

        metadata.add(new TripleImpl(extraction, Properties.RDF_TYPE,
                TechnicalClasses.ENHANCER_EXTRACTION));

        // relate the extraction to the content item
        metadata.add(new TripleImpl(extraction,
                Properties.ENHANCER_RELATED_CONTENT_ITEM, new UriRef(ci.getId())));

        // creation date
        metadata.add(new TripleImpl(extraction, Properties.DC_CREATED,
                literalFactory.createTypedLiteral(new Date())));

        // the engines that extracted the data
        // TODO: add some kind of versioning info for the extractor?
        // TODO: use a public dereferencing URI instead? that would allow for
        // explicit versioning too
        metadata.add(new TripleImpl(extraction, Properties.DC_CREATOR,
                literalFactory.createTypedLiteral(engine.getClass().getName())));

        return extraction;
    }

    /**
     * Random UUID generator with re-seedable RNG for the tests.
     *
     * @return a new Random UUID
     */
    public static UUID randomUUID() {
        return new UUID(rng.nextLong(), rng.nextLong());
    }

    /**
     * Getter for the first typed literal value of the property for a resource.
     *
     * @param <T> the java class the literal value needs to be converted to.
     * Note that the parsed LiteralFactory needs to support this conversion
     * @param graph the graph used to query for the property value
     * @param resource the resource
     * @param property the property
     * @param type the type the literal needs to be converted to
     * @param literalFactory the literalFactory
     * @return the value
     */
    public static <T> T get(TripleCollection graph, NonLiteral resource, UriRef property, Class<T> type,
            LiteralFactory literalFactory){
        Iterator<Triple> results = graph.filter(resource, property, null);
        if(results.hasNext()){
            Triple result = results.next();
            if(result.getObject() instanceof TypedLiteral){
                return literalFactory.createObject(type, (TypedLiteral)result.getObject());
            } else {
                log.warn("Triple "+result+" does not have a TypedLiteral as object! -> return null");
                return null;
            }
        } else {
            log.info("No Triple found for "+resource+" and property "+property+"! -> return null");
            return null;
        }
    }
    /**
     * Getter for the typed literal values of the property for a resource
     * @param <T> the java class the literal value needs to be converted to.
     * Note that the parsed LiteralFactory needs to support this conversion
     * @param graph the graph used to query for the property value
     * @param resource the resource
     * @param property the property
     * @param type the type the literal needs to be converted to
     * @param literalFactory the literalFactory
     * @return the value
     */
    public static <T> Iterator<T> getValues(TripleCollection graph, NonLiteral resource,
            UriRef property, final Class<T> type, final  LiteralFactory literalFactory){
        final Iterator<Triple> results = graph.filter(resource, property, null);
        return new Iterator<T>() {
            //TODO: dose not check if the object of the triple is of type UriRef
            @Override
            public boolean hasNext() {    return results.hasNext(); }
            @Override
            public T next() {
                return literalFactory.createObject(type, (TypedLiteral)results.next().getObject());
            }
            @Override
            public void remove() { results.remove(); }
        };
    }
    /**
     * Getter for the first String literal value the property for a resource
     * @param graph the graph used to query for the property value
     * @param resource the resource
     * @param property the property
     * @return the value
     */
    public static String getString(TripleCollection graph, NonLiteral resource, UriRef property){
        Iterator<Triple> results = graph.filter(resource, property, null);
        if(results.hasNext()){
            Triple result = results.next();
            if(result.getObject() instanceof Literal){
                return ((Literal)result.getObject()).getLexicalForm();
            } else {
                log.warn("Triple "+result+" does not have a literal as object! -> return null");
                return null;
            }
        } else {
            log.info("No Triple found for "+resource+" and property "+property+"! -> return null");
            return null;
        }
    }
    /**
     * Getter for the string literal values the property for a resource
     * @param graph the graph used to query for the property value
     * @param resource the resource
     * @param property the property
     * @return the value
     */
    public static Iterator<String> getStrings(TripleCollection graph, NonLiteral resource, UriRef property){
        final Iterator<Triple> results = graph.filter(resource, property, null);
        return new Iterator<String>() {
            //TODO: dose not check if the object of the triple is of type UriRef
            @Override
            public boolean hasNext() { return results.hasNext(); }
            @Override
            public String next() {
                return ((Literal)results.next().getObject()).getLexicalForm();
            }
            @Override
            public void remove() { results.remove(); }
        };
    }
    /**
     * Getter for the first value of the data type property for a resource
     * @param graph the graph used to query for the property value
     * @param resource the resource
     * @param property the property
     * @return the value
     */
    public static UriRef getReference(MGraph graph, NonLiteral resource, UriRef property){
        Iterator<Triple> results = graph.filter(resource, property, null);
        if(results.hasNext()){
            Triple result = results.next();
            if(result.getObject() instanceof UriRef){
                return (UriRef)result.getObject();
            } else {
                log.warn("Triple "+result+" does not have a UriRef as object! -> return null");
                return null;
            }
        } else {
            log.info("No Triple found for "+resource+" and property "+property+"! -> return null");
            return null;
        }
    }
    /**
     * Getter for the values of the data type property for a resource.
     *
     * @param graph the graph used to query for the property value
     * @param resource the resource
     * @param property the property
     * @return The iterator over all the values (
     */
    public static Iterator<UriRef> getReferences(MGraph graph, NonLiteral resource, UriRef property){
        final Iterator<Triple> results = graph.filter(resource, property, null);
        return new Iterator<UriRef>() {
            //TODO: dose not check if the object of the triple is of type UriRef
            @Override
            public boolean hasNext() { return results.hasNext(); }
            @Override
            public UriRef next() { return (UriRef)results.next().getObject(); }
            @Override
            public void remove() { results.remove(); }
        };
    }

}
