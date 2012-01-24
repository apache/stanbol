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
package org.apache.stanbol.enhancer.servicesapi.helper;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
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
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
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
        return createTextEnhancement(ci.getMetadata(), engine, new UriRef(ci.getUri().getUnicodeString()));
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
        return createEntityEnhancement(ci.getMetadata(), engine, new UriRef(ci.getUri().getUnicodeString()));
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
                Properties.ENHANCER_RELATED_CONTENT_ITEM, new UriRef(ci.getUri().getUnicodeString())));

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
            while(results.hasNext()){
                Triple result = results.next();
                if(result.getObject() instanceof TypedLiteral){
                    return literalFactory.createObject(type, (TypedLiteral)result.getObject());
                } else {
                    log.debug("Triple {} does not have a TypedLiteral as object! -> ignore",result);
                }
            }
            log.info("No value for {} and property {} had the requested Type {} -> return null",
                new Object[]{resource,property,type});
            return null;
        } else {
            log.debug("No Triple found for {} and property {}! -> return null",resource,property);
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
            while (results.hasNext()){
                Triple result = results.next();
                if(result.getObject() instanceof Literal){
                    return ((Literal)result.getObject()).getLexicalForm();
                } else {
                    log.debug("Triple {} does not have a literal as object! -> ignore",result);
                }
            }
            log.info("No Literal value for {} and property {} -> return null",
                resource,property);
            return null;
        } else {
            log.debug("No Triple found for "+resource+" and property "+property+"! -> return null");
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
    public static UriRef getReference(TripleCollection graph, NonLiteral resource, UriRef property){
        Iterator<Triple> results = graph.filter(resource, property, null);
        if(results.hasNext()){
            while(results.hasNext()){
            Triple result = results.next();
                if(result.getObject() instanceof UriRef){
                    return (UriRef)result.getObject();
                } else {
                    log.debug("Triple "+result+" does not have a UriRef as object! -> ignore");
                }
            }
            log.info("No UriRef value for {} and property {} -> return null",resource,property);
            return null;
        } else {
            log.debug("No Triple found for {} and property {}! -> return null",resource,property);
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
    public static Iterator<UriRef> getReferences(TripleCollection graph, NonLiteral resource, UriRef property){
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
    
    /**
     * Comparator that allows to sort a list/array of {@link EnhancementEngine}s
     * based on there {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING}.
     */
    public static final Comparator<EnhancementEngine> EXECUTION_ORDER_COMPARATOR = new Comparator<EnhancementEngine>() {

        @Override
        public int compare(EnhancementEngine engine1, EnhancementEngine engine2) {
            Integer order1 = getEngineOrder(engine1);
            Integer order2 = getEngineOrder(engine2);
            //start with the highest number finish with the lowest ...
            return order1 == order2?0:order1<order2?1:-1;
        }

    };
    /**
     * Gets the {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING} value
     * for the parsed EnhancementEngine. If the Engine does not implement the
     * {@link ServiceProperties} interface or does not provide the
     * {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING} the 
     * {@link ServiceProperties#ORDERING_DEFAULT} is returned <p>
     * This method is guaranteed to NOT return <code>null</code>.
     * @param engine the engine
     * @return the ordering
     */
    public static Integer getEngineOrder(EnhancementEngine engine){
        log.debug("getOrder "+engine);
        if (engine instanceof ServiceProperties){
            log.debug(" ... implements ServiceProperties");
            Object value = ((ServiceProperties)engine).getServiceProperties().get(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING);
            log.debug("   > value = "+value +" "+value.getClass());
            if (value !=null && value instanceof Integer){
                return (Integer)value;
            }
        }
        return ServiceProperties.ORDERING_DEFAULT;
    }
}
