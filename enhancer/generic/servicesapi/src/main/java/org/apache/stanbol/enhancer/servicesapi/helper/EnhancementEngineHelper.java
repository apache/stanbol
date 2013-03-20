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

import static java.util.Collections.singleton;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EnhancementEngineHelper {

    /**
     * The minimum length of the selected text so that fise:selection-head and
     * fise:selection.tail are being used instead of fise:selected-text. The
     * actual size is calculated by using <code>prefixSuffixLength*5</code>.
     * So if a user does not chage the {@link #DEFAULT_PREFIX_SUFFIX_LENGTH} the
     * default value us <code>10 * 5 = 50</code> chars.
     */
    public static final int MIN_SELECTEN_HEAD_TAIL_USAGE_LENGTH = 30;
    /**
     * The default length of fise:selection-prefix and fise:selection-suffix
     * literals (value = 10).
     */
    public static final int DEFAULT_PREFIX_SUFFIX_LENGTH = 10;
    /**
     * The minimum size for fise:selection-prefix and fise:selection-suffix
     * literals (value = 3).
     */
    public static final int MIN_PREFIX_SUFFIX_SIZE = 3;

    protected final static Random rng = new Random();

    private final static Logger log = LoggerFactory.getLogger(EnhancementEngineHelper.class);

    private final static LiteralFactory lf = LiteralFactory.getInstance();

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
        metadata.add(new TripleImpl(enhancement, RDF_TYPE,
                ENHANCER_TEXTANNOTATION));
        return enhancement;
    }
    /**
     * This method sets the fise:start, fise:end, fise:selection-prefix, 
     * fise:selected-text and fise:selection-suffix properties for the 
     * parsed fise:TextAnnotation instance according to the parsed parameters.<p>
     * While it is intended to be used for TextAnnotations this method can also
     * be used to add the mentioned properties to {@link UriRef}s with different
     * type.<p>
     * <b>NOTE</b> the <code>allowSelectionHeadTail</code>: This parameter allows
     * to deactivate the usage of fise:selection-head and fise:selection-tail.
     * Typically users should parse <code>false</code> in case of 'named entities'
     * and <code>true</code> in case sections of the text (e.g. phrases, sentences,
     * chapters ...) are selected.
     * @param metadata The RDF graph to add the information
     * @param textAnnotation the UriRef of the fise:TextAnnotation
     * @param content the plain text content as String
     * @param start the start index of the occurrence 
     * @param end the end index of the occurrence
     * @param lang the lanugage of the content or <code>null</code> if not known
     * @param prefixSuffixSize the size of the prefix, suffix. If the parsed
     * value &lt; 3 than the default 10 is used.
     * @param allowSelectionHeadTail if <code>true</code> the fise:selection-head
     * and fise:selection-tail properties are used instead of fise:selected-text
     * if the selected text is longer as <code>Math.max(30, prefixSuffixSize*5);</code>.
     * If <code>false</code> the fise:selected-text is added regardless of the
     * size of the selected area.
     * @since 0.11.0
     */
    public static void setOccurrence(MGraph metadata, UriRef textAnnotation,
            String content, Integer start, Integer end, Language lang, int prefixSuffixSize, 
            boolean allowSelectionHeadTail){
        //set start, end
        metadata.add(new TripleImpl(textAnnotation, ENHANCER_START, 
            lf.createTypedLiteral(start)));
        metadata.add(new TripleImpl(textAnnotation, ENHANCER_END, 
            lf.createTypedLiteral(end)));
        //set selection prefix and suffix (TextAnnotation new model)
        prefixSuffixSize = prefixSuffixSize < MIN_PREFIX_SUFFIX_SIZE ? 
                DEFAULT_PREFIX_SUFFIX_LENGTH : prefixSuffixSize;
        metadata.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_PREFIX, 
            new PlainLiteralImpl(content.substring(
                Math.max(0,start-prefixSuffixSize), start), lang)));
        metadata.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_SUFFIX, 
            new PlainLiteralImpl(content.substring(
                end,Math.min(content.length(), end+prefixSuffixSize)),lang)));
        //set the selected text (or alternatively head and tail)
        int maxSelectedTextSize = Math.max(MIN_SELECTEN_HEAD_TAIL_USAGE_LENGTH, 
            prefixSuffixSize*5);
        if(!allowSelectionHeadTail || end-start <= maxSelectedTextSize){
            metadata.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, 
                new PlainLiteralImpl(content.substring(start, end),lang)));
        } else { //selected area to long for fise:selected-text
            //use fise:selection-head and fise:selection-tail instead
            metadata.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_HEAD, 
                new PlainLiteralImpl(content.substring(
                    start,start+prefixSuffixSize),lang)));
            metadata.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_TAIL, 
                new PlainLiteralImpl(content.substring(
                    end-prefixSuffixSize,end),lang)));
        }
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
        metadata.add(new TripleImpl(enhancement, RDF_TYPE, ENHANCER_ENTITYANNOTATION));
        return enhancement;
    }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:TopicAnnotation in the parsed graph along with default properties
     * (dc:creator, dc:created and enhancer:extracted-form) and return
     * the UriRef of the extraction so that engines can further add.
     *
     * @param metadata the graph
     * @param engine the engine
     * @param contentItemId the id
     *
     * @return the URI of the new enhancement instance
     */
    public static UriRef createTopicEnhancement(MGraph metadata,
                 EnhancementEngine engine, UriRef contentItemId){
         UriRef enhancement = createEnhancement(metadata, engine, contentItemId);
         metadata.add(new TripleImpl(enhancement, RDF_TYPE, ENHANCER_TOPICANNOTATION));
         return enhancement;
     }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:TopicAnnotation in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the UriRef of the extraction so that engines can further add
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     * @return the URI of the new enhancement instance
     */
    public static UriRef createTopicEnhancement(ContentItem ci,
            EnhancementEngine engine){
        return createTopicEnhancement(ci.getMetadata(), engine, new UriRef(ci.getUri().getUnicodeString()));
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
        metadata.add(new TripleImpl(enhancement, RDF_TYPE,
                ENHANCER_ENHANCEMENT));
        //add the extracted from content item
        metadata.add(new TripleImpl(enhancement,
                ENHANCER_EXTRACTED_FROM, contentItemId));
        // creation date
        metadata.add(new TripleImpl(enhancement, DC_CREATED,
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
        metadata.add(new TripleImpl(enhancement, DC_CREATOR,
                literalFactory.createTypedLiteral(engine.getClass().getName())));
        return enhancement;
    }
    /**
     * Adds the parsed {@link EnhancementEngine} as dc:contributer to the
     * enhancement and also sets the dc:modified property accordingly
     * @param metadata the {@link ContentItem#getMetadata()}
     * @param enhancement the enhancement
     * @param engine the engine
     */
    public static void addContributingEngine(MGraph metadata, UriRef enhancement,
                                             EnhancementEngine engine){
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        // TODO: use a public dereferencing URI instead?
        metadata.add(new TripleImpl(enhancement, DC_CONTRIBUTOR,
            literalFactory.createTypedLiteral(engine.getClass().getName())));
        //set the modification date to the current date.
        set(metadata,enhancement,DC_MODIFIED,new Date(),literalFactory);
    }
    
    /**
     * Create a new extraction instance in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the UriRef of the extraction so that engines can further add
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     * @return the URI of the new extraction instance
     * @deprecated will be remove with 1.0
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

        metadata.add(new TripleImpl(extraction, RDF_TYPE,
                ENHANCER_EXTRACTION));

        // relate the extraction to the content item
        metadata.add(new TripleImpl(extraction,
                ENHANCER_RELATED_CONTENT_ITEM, new UriRef(ci.getUri().getUnicodeString())));

        // creation date
        metadata.add(new TripleImpl(extraction, DC_CREATED,
                literalFactory.createTypedLiteral(new Date())));

        // the engines that extracted the data
        // TODO: add some kind of versioning info for the extractor?
        // TODO: use a public dereferencing URI instead? that would allow for
        // explicit versioning too
        metadata.add(new TripleImpl(extraction, DC_CREATOR,
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
     * Replaces all current values of the property for the resource
     * with the parsed value
     * @param graph the graph
     * @param resource the resource
     * @param property the property
     * @param value the value
     */
    public static void set(MGraph graph, NonLiteral resource, UriRef property, Resource value){
        set(graph,resource,property,value == null ? null : singleton(value),null);
    }
    /**
     * Replaces all current values of the property for the resource
     * with the parsed values
     * @param graph the graph
     * @param resource the resource
     * @param property the property
     * @param value the value
     */
    public static void set(MGraph graph, NonLiteral resource, UriRef property, Collection<Resource> values){
        set(graph,resource,property,values,null);
    }

    /**
     * Replaces all current values of the property for the resource
     * with the parsed value
     * @param graph the graph
     * @param resource the resource
     * @param property the property
     * @param value the value. In case it is an instance of {@link Resource} it
     * is directly added to the graph. Otherwise the parsed {@link LiteralFactory}
     * is used to create a {@link TypedLiteral} for the parsed value.
     * @param literalFactory the {@link LiteralFactory} used in case the parsed
     * value is not an {@link Resource}
     */
    public static void set(MGraph graph, NonLiteral resource, UriRef property,
                           Object value, LiteralFactory literalFactory){
        set(graph,resource,property,value == null ? null : singleton(value),literalFactory);
    }
    /**
     * Replaces all current values of the property for the resource
     * with the parsed values
     * @param graph the graph
     * @param resource the resource
     * @param property the property
     * @param value the value. In case it is an instance of {@link Resource} it
     * is directly added to the graph. Otherwise the parsed {@link LiteralFactory}
     * is used to create a {@link TypedLiteral} for the parsed value.
     * @param literalFactory the {@link LiteralFactory} used in case the parsed
     * value is not an {@link Resource}
     */
    public static void set(MGraph graph, NonLiteral resource, UriRef property,
                               Collection<?> values, LiteralFactory literalFactory){
        Iterator<Triple> currentValues = graph.filter(resource, property, null);
        while(currentValues.hasNext()){
            currentValues.next();
            currentValues.remove();
        }
        if(values != null){
            for(Object value : values){
                if(value instanceof Resource){
                    graph.add(new TripleImpl(resource, property, (Resource) value));
                } else if (value != null){
                    graph.add(new TripleImpl(resource, property, 
                        literalFactory.createTypedLiteral(value)));
                }
            }
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
    
    /**
     * Getter for the Resources of fise:TextAnnotations that do have a value 
     * of the dc:language property. The returned list is sorted by 'fise:confidence'.
     * Annotations with missing confidence are ranked last.<p>
     * NOTE that the returned list will likely contain annotations for the same language
     * if multiple language identification are used in the same {@link Chain}.
     * @param graph the graph with the enhancement. 
     * Typically {@link ContentItem#getMetadata()}
     * @return the sorted list of language annotations or an empty list if none.
     * @throws IllegalArgumentException if <code>null</code> is parsed as graph
     */
    public static List<NonLiteral> getLanguageAnnotations(TripleCollection graph){
        if(graph == null){
            throw new IllegalArgumentException("The parsed graph MUST NOT be NULL!");
        }
        // I do not use SPARQL, because I do not want to instantiate a QueryEngine
        final Map<NonLiteral,Double> confidences = new HashMap<NonLiteral,Double>();
        List<NonLiteral> langAnnotations = new ArrayList<NonLiteral>();
        Iterator<Triple> textAnnoataions = graph.filter(null, RDF_TYPE, ENHANCER_TEXTANNOTATION);
        while(textAnnoataions.hasNext()){
            NonLiteral textAnnotation = textAnnoataions.next().getSubject();
            String language = getString(graph, textAnnotation, DC_LANGUAGE);
            if(language != null){
                Double confidence = get(graph, textAnnotation, ENHANCER_CONFIDENCE, Double.class, lf);
                confidences.put(textAnnotation,confidence);
                langAnnotations.add(textAnnotation);
            }
        }
        if(langAnnotations.size() > 1){
            Collections.sort(langAnnotations,new Comparator<NonLiteral>() {
                @Override
                public int compare(NonLiteral o1, NonLiteral o2) {
                    Double c1 = confidences.get(o1);
                    Double c2 = confidences.get(o2);
                    //decrising order (values without confidence last)
                    if(c1 == null){
                        return c2 == null ? 0 : 1;
                    } else if(c2 == null){
                        return -1;
                    } else {
                        return c2.compareTo(c1);
                    }
                }
            });
        }
        return langAnnotations;
    }
    /**
     * Getter for language identified for (extracted-from) the parsed
     * ContentItem. The returned value is the Annotation with the highest
     * 'fise:confidence' value - or if no annotations are present - the
     * 'dc-terms:language' value of the {@link ContentItem#getUri()}.<p>
     * Users that want to obtain all language annotations should use
     * {@link #getLanguageAnnotations(TripleCollection)} instead.<p>
     * This method ensures a write lock on the {@link ContentItem}.
     * @param ci the contentItem
     * @return the identified language of the parsed {@link ContentItem}.
     * <code>null</code> if not available.
     * @throws IllegalArgumentException if <code>null</code> is parsed as content item
     * @see #getLanguageAnnotations(TripleCollection)
     */
    public static String getLanguage(ContentItem ci){
        if(ci == null){
            throw new IllegalArgumentException("The parsed ContentItem MUST NOT be NULL!");
        }
        ci.getLock().readLock().lock();
        try {
            List<NonLiteral> langAnnotations = getLanguageAnnotations(ci.getMetadata());
            if(langAnnotations.isEmpty()){ //fallback
                return getString(ci.getMetadata(), ci.getUri(), DC_LANGUAGE);
            } else {
                return getString(ci.getMetadata(), langAnnotations.get(0), DC_LANGUAGE);
            }
        } finally {
            ci.getLock().readLock().unlock();
        }
    }
}
