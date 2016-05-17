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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.rdf.core.InvalidLiteralTypeException;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementPropertyException;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class EnhancementEngineHelper {

    /**
     * Restrict instantiation
     */
    private EnhancementEngineHelper() {}

    /**
     * The maximum size of the prefix/suffix for the selection context
     * @since 0.11.0
     */
    public static final int DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE = 50;
    /**
     * The minimum size of the prefix/suffix for the selection context
     * @since 0.11.0
     */
    public static final int MIN_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE = 15;
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
     * the IRI of the extraction so that engines can further add.
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     *
     * @return the URI of the new enhancement instance
     */
    public static IRI createTextEnhancement(ContentItem ci,
            EnhancementEngine engine){
        return createTextEnhancement(ci.getMetadata(), engine, new IRI(ci.getUri().getUnicodeString()));
    }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:TextAnnotation in the parsed graph along with default properties
     * (dc:creator, dc:created and enhancer:extracted-form) and return
     * the IRI of the extraction so that engines can further add.
     *
     * @param metadata the graph
     * @param engine the engine
     * @param contentItemId the id
     *
     * @return the URI of the new enhancement instance
     */
    public static IRI createTextEnhancement(Graph metadata,
                EnhancementEngine engine, IRI contentItemId){
        IRI enhancement = createEnhancement(metadata, engine,contentItemId);
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
     * be used to add the mentioned properties to {@link IRI}s with different
     * type.<p>
     * <b>NOTE</b> the <code>allowSelectionHeadTail</code>: This parameter allows
     * to deactivate the usage of fise:selection-head and fise:selection-tail.
     * Typically users should parse <code>false</code> in case of 'named entities'
     * and <code>true</code> in case sections of the text (e.g. phrases, sentences,
     * chapters ...) are selected.
     * @param metadata The RDF graph to add the information
     * @param textAnnotation the IRI of the fise:TextAnnotation
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
    public static void setOccurrence(Graph metadata, IRI textAnnotation,
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
     * Extracts the selection context based on the content, selection and
     * the start char offset of the selection. Tries to cut of the context 
     * on whole words. The size of the prefix/suffix is set to
     * {@link #DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE}.
     * @param content the content
     * @param selection the selected text
     * @param selectionStartPos the start char position of the selection
     * @return the context
     * @since 0.11.0
     */
    public static String getSelectionContext(String content, String selection, int selectionStartPos){
        return getSelectionContext(content, selection, selectionStartPos, 
            DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE);
    }
    /**
     * Extracts the selection context based on the content, selection and
     * the start char offset of the selection. Tries to cut of the context 
     * on whole words.
     * @param content the content
     * @param selection the selected text
     * @param selectionStartPos the start char position of the selection
     * @param contextSize the size of the prefix/suffix. If less than zero the
     * {@link #DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE} is used. If in the
     * range [0..{@link #MIN_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE}] than the
     * size is set to {@link #MIN_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE}
     * @return the context
     * @since 0.11.0
     */
    public static String getSelectionContext(String content, String selection, int selectionStartPos, int contextSize){
        if(contextSize < 0){
            contextSize = DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
        }
        if(contextSize < MIN_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE){
            contextSize = MIN_PREFIX_SUFFIX_SIZE;
        }
        //extract the selection context
        int beginPos;
        if(selectionStartPos <= DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE){
            beginPos = 0;
        } else {
            int start = selectionStartPos-DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            beginPos = content.indexOf(' ',start);
            if(beginPos < 0 || beginPos >= selectionStartPos){ //no words
                beginPos = start; //begin within a word
            }
        }
        int endPos;
        if(selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE >= content.length()){
            endPos = content.length();
        } else {
            int start = selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            endPos = content.lastIndexOf(' ', start);
            if(endPos <= selectionStartPos+selection.length()){
                endPos = start; //end within a word;
            }
        }
        return content.substring(beginPos, endPos);
    }

    
    
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:EntityAnnotation in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the IRI of the extraction so that engines can further add
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     * @return the URI of the new enhancement instance
     */
    public static IRI createEntityEnhancement(ContentItem ci,
            EnhancementEngine engine){
        return createEntityEnhancement(ci.getMetadata(), engine, new IRI(ci.getUri().getUnicodeString()));
    }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:EntityAnnotation in the parsed graph along with default properties
     * (dc:creator, dc:created and enhancer:extracted-form) and return
     * the IRI of the extraction so that engines can further add.
     *
     * @param metadata the graph
     * @param engine the engine
     * @param contentItemId the id
     *
     * @return the URI of the new enhancement instance
     */
    public static IRI createEntityEnhancement(Graph metadata,
                EnhancementEngine engine, IRI contentItemId){
        IRI enhancement = createEnhancement(metadata, engine, contentItemId);
        metadata.add(new TripleImpl(enhancement, RDF_TYPE, ENHANCER_ENTITYANNOTATION));
        return enhancement;
    }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:TopicAnnotation in the parsed graph along with default properties
     * (dc:creator, dc:created and enhancer:extracted-form) and return
     * the IRI of the extraction so that engines can further add.
     *
     * @param metadata the graph
     * @param engine the engine
     * @param contentItemId the id
     *
     * @return the URI of the new enhancement instance
     */
    public static IRI createTopicEnhancement(Graph metadata,
                 EnhancementEngine engine, IRI contentItemId){
         IRI enhancement = createEnhancement(metadata, engine, contentItemId);
         metadata.add(new TripleImpl(enhancement, RDF_TYPE, ENHANCER_TOPICANNOTATION));
         return enhancement;
     }
    /**
     * Create a new instance with the types enhancer:Enhancement and
     * enhancer:TopicAnnotation in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the IRI of the extraction so that engines can further add
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     * @return the URI of the new enhancement instance
     */
    public static IRI createTopicEnhancement(ContentItem ci,
            EnhancementEngine engine){
        return createTopicEnhancement(ci.getMetadata(), engine, new IRI(ci.getUri().getUnicodeString()));
    }
    /**
     * Create a new enhancement instance in the metadata-graph of the content
     * item along with default properties (dc:creator and dc:created) and return
     * the IRI of the extraction so that engines can further add. <p>
     * <i>NOTE:</i> This method was protected prior to <code>0.12.1</code> (see
     * <a href="https://issues.apache.org/jira/browse/STANBOL-1321">STANBOL-1321</a>)
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     *
     * @return the URI of the new enhancement instance
     * @since 0.12.1
     */
    public static IRI createEnhancement(Graph metadata,
            EnhancementEngine engine, IRI contentItemId){
        LiteralFactory literalFactory = LiteralFactory.getInstance();

        IRI enhancement = new IRI("urn:enhancement-"
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
    public static void addContributingEngine(Graph metadata, IRI enhancement,
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
     * the IRI of the extraction so that engines can further add
     *
     * @param ci the ContentItem being under analysis
     * @param engine the Engine performing the analysis
     * @return the URI of the new extraction instance
     * @deprecated will be remove with 1.0
     * @see EnhancementEngineHelper#createEntityEnhancement(ContentItem, EnhancementEngine)
     * @see EnhancementEngineHelper#createTextEnhancement(ContentItem, EnhancementEngine)
     */
    @Deprecated
    public static IRI createNewExtraction(ContentItem ci,
            EnhancementEngine engine) {
        LiteralFactory literalFactory = LiteralFactory.getInstance();

        Graph metadata = ci.getMetadata();
        IRI extraction = new IRI("urn:extraction-"
                + EnhancementEngineHelper.randomUUID());

        metadata.add(new TripleImpl(extraction, RDF_TYPE,
                ENHANCER_EXTRACTION));

        // relate the extraction to the content item
        metadata.add(new TripleImpl(extraction,
                ENHANCER_RELATED_CONTENT_ITEM, new IRI(ci.getUri().getUnicodeString())));

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
    public static <T> T get(Graph graph, BlankNodeOrIRI resource, IRI property, Class<T> type,
            LiteralFactory literalFactory){
        Iterator<Triple> results = graph.filter(resource, property, null);
        if(results.hasNext()){
            while(results.hasNext()){
                Triple result = results.next();
                if(result.getObject() instanceof Literal){
                    return literalFactory.createObject(type, (Literal)result.getObject());
                } else {
                    log.debug("Triple {} does not have a Literal as object! -> ignore",result);
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
    public static void set(Graph graph, BlankNodeOrIRI resource, IRI property, RDFTerm value){
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
    public static void set(Graph graph, BlankNodeOrIRI resource, IRI property, Collection<RDFTerm> values){
        set(graph,resource,property,values,null);
    }

    /**
     * Replaces all current values of the property for the resource
     * with the parsed value
     * @param graph the graph
     * @param resource the resource
     * @param property the property
     * @param value the value. In case it is an instance of {@link RDFTerm} it
     * is directly added to the graph. Otherwise the parsed {@link LiteralFactory}
     * is used to create a {@link TypedLiteral} for the parsed value.
     * @param literalFactory the {@link LiteralFactory} used in case the parsed
     * value is not an {@link RDFTerm}
     */
    public static void set(Graph graph, BlankNodeOrIRI resource, IRI property,
                           Object value, LiteralFactory literalFactory){
        set(graph,resource,property,value == null ? null : singleton(value),literalFactory);
    }
    /**
     * Replaces all current values of the property for the resource
     * with the parsed values
     * @param graph the graph
     * @param resource the resource
     * @param property the property
     * @param value the value. In case it is an instance of {@link RDFTerm} it
     * is directly added to the graph. Otherwise the parsed {@link LiteralFactory}
     * is used to create a {@link TypedLiteral} for the parsed value.
     * @param literalFactory the {@link LiteralFactory} used in case the parsed
     * value is not an {@link RDFTerm}
     */
    public static void set(Graph graph, BlankNodeOrIRI resource, IRI property,
                               Collection<?> values, LiteralFactory literalFactory){
        Iterator<Triple> currentValues = graph.filter(resource, property, null);
        while(currentValues.hasNext()){
            currentValues.next();
            currentValues.remove();
        }
        if(values != null){
            for(Object value : values){
                if(value instanceof RDFTerm){
                    graph.add(new TripleImpl(resource, property, (RDFTerm) value));
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
    public static <T> Iterator<T> getValues(Graph graph, BlankNodeOrIRI resource,
            IRI property, final Class<T> type, final  LiteralFactory literalFactory){
        final Iterator<Triple> results = graph.filter(resource, property, null);
        return new Iterator<T>() {
            //TODO: dose not check if the object of the triple is of type IRI
            @Override
            public boolean hasNext() {    return results.hasNext(); }
            @Override
            public T next() {
                return literalFactory.createObject(type, (Literal)results.next().getObject());
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
    public static String getString(Graph graph, BlankNodeOrIRI resource, IRI property){
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
    public static Iterator<String> getStrings(Graph graph, BlankNodeOrIRI resource, IRI property){
        final Iterator<Triple> results = graph.filter(resource, property, null);
        return new Iterator<String>() {
            //TODO: dose not check if the object of the triple is of type IRI
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
    public static IRI getReference(Graph graph, BlankNodeOrIRI resource, IRI property){
        Iterator<Triple> results = graph.filter(resource, property, null);
        if(results.hasNext()){
            while(results.hasNext()){
            Triple result = results.next();
                if(result.getObject() instanceof IRI){
                    return (IRI)result.getObject();
                } else {
                    log.debug("Triple "+result+" does not have a IRI as object! -> ignore");
                }
            }
            log.info("No IRI value for {} and property {} -> return null",resource,property);
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
    public static Iterator<IRI> getReferences(Graph graph, BlankNodeOrIRI resource, IRI property){
        final Iterator<Triple> results = graph.filter(resource, property, null);
        return new Iterator<IRI>() {
            //TODO: dose not check if the object of the triple is of type IRI
            @Override
            public boolean hasNext() { return results.hasNext(); }
            @Override
            public IRI next() { return (IRI)results.next().getObject(); }
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
    public static List<BlankNodeOrIRI> getLanguageAnnotations(Graph graph){
        if(graph == null){
            throw new IllegalArgumentException("The parsed graph MUST NOT be NULL!");
        }
        // I do not use SPARQL, because I do not want to instantiate a QueryEngine
        final Map<BlankNodeOrIRI,Double> confidences = new HashMap<BlankNodeOrIRI,Double>();
        List<BlankNodeOrIRI> langAnnotations = new ArrayList<BlankNodeOrIRI>();
        Iterator<Triple> textAnnoataions = graph.filter(null, RDF_TYPE, ENHANCER_TEXTANNOTATION);
        while(textAnnoataions.hasNext()){
            BlankNodeOrIRI textAnnotation = textAnnoataions.next().getSubject();
            String language = getString(graph, textAnnotation, DC_LANGUAGE);
            if(language != null){
                Double confidence = null;
                try {
                    confidence = get(graph, textAnnotation, ENHANCER_CONFIDENCE, Double.class, lf);
                } catch (InvalidLiteralTypeException e){ // STANBOL-1417: not a double value
                    try { //try with float
                        Float fconf = get(graph,textAnnotation,ENHANCER_CONFIDENCE,Float.class,lf);
                        if(fconf != null){
                            confidence = Double.valueOf(fconf.doubleValue());
                        }
                    } catch (InvalidLiteralTypeException e1){
                        log.warn("Unable to parse confidence for language annotation "
                            + textAnnotation, e);
                    }
                }
                confidences.put(textAnnotation,confidence);
                langAnnotations.add(textAnnotation);
            }
        }
        if(langAnnotations.size() > 1){
            Collections.sort(langAnnotations,new Comparator<BlankNodeOrIRI>() {
                @Override
                public int compare(BlankNodeOrIRI o1, BlankNodeOrIRI o2) {
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
     * {@link #getLanguageAnnotations(Graph)} instead.<p>
     * This method ensures a write lock on the {@link ContentItem}.
     * @param ci the contentItem
     * @return the identified language of the parsed {@link ContentItem}.
     * <code>null</code> if not available.
     * @throws IllegalArgumentException if <code>null</code> is parsed as content item
     * @see #getLanguageAnnotations(Graph)
     */
    public static String getLanguage(ContentItem ci){
        if(ci == null){
            throw new IllegalArgumentException("The parsed ContentItem MUST NOT be NULL!");
        }
        ci.getLock().readLock().lock();
        try {
            List<BlankNodeOrIRI> langAnnotations = getLanguageAnnotations(ci.getMetadata());
            if(langAnnotations.isEmpty()){ //fallback
                return getString(ci.getMetadata(), ci.getUri(), DC_LANGUAGE);
            } else {
                return getString(ci.getMetadata(), langAnnotations.get(0), DC_LANGUAGE);
            }
        } finally {
            ci.getLock().readLock().unlock();
        }
    }
    
    /* 
     * Helper Methods for retrieving EnhancementProperties in 0.12 from the
     * parsed ContentItem (see STANBOL-1280).
     * NOTE: in 1.0.0 those are obsolete as EnhancementProperties will be parsed
     * as additional parameter to the computeEnhancement method.
     */
    private static final String EHPROP_NS = NamespaceEnum.ehp.getNamespace();
    private static final int EHPROP_NS_LENGTH = EHPROP_NS.length();

    /**
     * Retrieves the Enhancement Properties for the parsed Engine from the ContentItem.
     * <p>
     * The returned map will contain: <ol>
     * <li> Request scoped properties defined for the parsed enhancement engines
     * <li> Request scoped properties defined for chain
     * <li> Chain scoped properties defined for the parsed enhancement engine
     * <li> Chain scoped properties defined for the chain.
     * </ol>
     * NOTES: <ul>
     * <li> The specification (see <a href="https://issues.apache.org/jira/browse/STANBOL-488">STANBOL-488</a>)
     * required properties to start with '<code>enhancer.</code>'. While this
     * implementation does not enforce this requirement non compliant properties
     * will most likely get filtered earlier and not be part of the returned map.
     * <li> Properties of an higher priority do override those with an lower one.
     * </ul>
     * @param engine the enhancement engine requesting the properties
     * @param ci the content item (representing the enhancement request).
     * @return The enhancement properties. This is a read/write copy of the
     * read-only configuration.
     * @see #getEnhancementPropertyDict(EnhancementEngine, ContentItem)
     */
    public static Dictionary<String,Object> getEnhancementPropertyDict(EnhancementEngine engine, ContentItem ci){
        return new DictionaryAdapter<String,Object>(getEnhancementProperties(engine, ci));
    }
    /**
     * Retrieves the Enhancement Properties for the parsed Engine from the ContentItem.
     * <p>
     * The returned map will contain: <ol>
     * <li> Request scoped properties defined for the parsed enhancement engines
     * <li> Request scoped properties defined for chain
     * <li> Chain scoped properties defined for the parsed enhancement engine
     * <li> Chain scoped properties defined for the chain.
     * </ol>
     * NOTES: <ul>
     * <li> The specification (see <a href="https://issues.apache.org/jira/browse/STANBOL-488">STANBOL-488</a>)
     * required properties to start with '<code>enhancer.</code>'. While this
     * implementation does not enforce this requirement non compliant properties
     * will most likely get filtered earlier and not be part of the returned map.
     * <li> Properties of an higher priority do override those with an lower one.
     * </ul>
     * @param engine the enhancement engine requesting the properties
     * @param ci the content item (representing the enhancement request).
     * @return The enhancement properties. This is a read/write copy of the
     * read-only configuration.
     * @see #getEnhancementPropertyDict(EnhancementEngine, ContentItem)
     */
    public static Map<String,Object> getEnhancementProperties(EnhancementEngine engine, ContentItem ci){
        if(engine == null){
            throw new IllegalArgumentException("The parsed EnhancementEngine MUST NOT be NULL");
        }
        if(ci == null){
            throw new IllegalArgumentException("The parsed ContentItem MUST NOT be NULL");
        }
        //(1) retrieve Chain scope Enhancement Properties
        Map<String,Object> chainExProps = getChainExecutionProperties(engine, ci);
        
        //(2) retrieve Request specific EnhancementProperties
        //TODO: in future Stanbol version request specific EnhancementProperties
        //      will get stored in the ExecutionMetadata. Chain level properties
        //      with the `em:ChainExecution` node and engine specific properties
        //      with the `em:EngineExecution` node.
        //      So this code will need to be refactored similar to the above one
        Map<String,Object> epContentPart = ContentItemHelper.getRequestPropertiesContentPart(ci);
        Map<String,Object> chainProperties = new HashMap<String,Object>();
        Map<String,Object> engineProperties = new HashMap<String,Object>();
        if(epContentPart != null){
            String enginePrefix = new StringBuilder(engine.getName()).append(':').toString();
            log.debug("Retrieve EnhancementProperties for Engine {} and ContentItem {}", 
                engine.getName(), ci.getUri());
            //Set<String> engineKeys = new HashSet<String>();
            for(Entry<String,Object> entry : epContentPart.entrySet()){
                String key = entry.getKey();
                int sepIndex = key.indexOf(':');
                if(sepIndex < 0){
                    log.debug(" ... add chain request level property {}='{}'", key,entry.getValue());
                    chainProperties.put(key, entry.getValue());
                } else if(key.startsWith(enginePrefix) && key.length() > enginePrefix.length()){
                    key = key.substring(enginePrefix.length(),key.length());
                    log.debug(" ... add engine request level property {}='{}'", key,entry.getValue());
                    engineProperties.put(key, entry.getValue());
                } // else not a enhancement property for the current engine.
            }
        } else {
            log.debug(" - no Request scope EnhancementProperties for ContentItem",ci.getUri());
        }
        //Now we need to merge the properties based on the Enhancement Properties Precedence
        //defined by STANBOL-488
        // engineProp > engineEx > chainProp > chainExProp
        Map<String,Object> properties = new HashMap<String,Object>(chainExProps);
        properties.putAll(chainProperties);
        properties.putAll(engineProperties);
        return properties;
    }

    /**
     * Getter for the {@link Chain} scoped (chain and chain-engine scoped) properties 
     * for the parsed enhancement engine and content item.
     * @param engine the enhancement engine
     * @param ci the content item
     * @return the chain scoped enhancement properties. This will not include any
     * request scoped properties.
     * @since 0.12.1 (<a href="https://issues.apache.org/jira/browse/STANBOL-1361">STANBOL-1361</a>)
     */
    public static Map<String,Object> getChainExecutionProperties(EnhancementEngine engine, ContentItem ci) {
        if(engine == null){
            throw new IllegalArgumentException("The parsed EnhancementEngine MUST NOT be NULL");
        }
        if(ci == null){
            throw new IllegalArgumentException("The parsed ContentItem MUST NOT be NULL");
        }
        Map<String,Object> chainExProps = new HashMap<String,Object>();
        Map<String,Object> engineExProps = new HashMap<String,Object>();
        ci.getLock().readLock().lock();
        try{
            Graph em = ExecutionMetadataHelper.getExecutionMetadata(ci);
            //(1.a) retrieve EnhancementProperties from the ep:ExecutionPlan
            log.debug("> extract EnhancementProperties form the ExecutionPlan");
            BlankNodeOrIRI executionPlanNode = ExecutionMetadataHelper.getExecutionPlanNode(em, 
                ExecutionMetadataHelper.getChainExecution(em, ci.getUri()));
            extractEnhancementProperties(chainExProps, em, executionPlanNode, "Chain Execution");
            //(1.b) retrieve Enhancement Properties from the ep:ExectutionNode
            //      for the parsed EnhancementEngine
            log.debug("> extract EnhancementProperties form the ExecutionNode of Engine {}",
                engine.getName());
            Iterator<Triple> engineExecutions = em.filter(null, ExecutionPlan.ENGINE, new PlainLiteralImpl(engine.getName()));
            //NOTE: we expect only a single execution node for an engine, but if
            //      there are multiple we will merge the properties of those
            while(engineExecutions.hasNext()){
                BlankNodeOrIRI engineExecution = engineExecutions.next().getSubject();
                if(em.contains(new TripleImpl(executionPlanNode, ExecutionPlan.HAS_EXECUTION_NODE, engineExecution))){
                    extractEnhancementProperties(engineExProps,em, engineExecution, "Engine Execution");
                } //else engine execution of a different execution plan
            }
        } catch(NoSuchPartException e){ //no execution metadata are present
            log.debug("  - no ExecutionMetadata are present ...");
        } finally {
            ci.getLock().readLock().unlock();
        }
        //finally merge the chain-engine scoped properties into the chain scoped properties
        chainExProps.putAll(engineExProps);
        return chainExProps;
    }

    /**
     * Extracts all EnhancementProperties from the parsed Node and adds them to
     * the parsed map
     * @param properties The Map to add the extracted properties. extracted values
     * are appended to existing values.
     * @param graph the RDF graph containing the data
     * @param node the node to extract the properties from
     * @param level the name of the level (only used for logging)
     */
    private static void extractEnhancementProperties(Map<String,Object> properties, Graph graph,
            BlankNodeOrIRI node, String level) {
        log.debug(" - extract {} properties from {}", level, node);
        Iterator<Triple> props = graph.filter(node, null, null);
        while(props.hasNext()){
            Triple t = props.next();
            String propUri =  t.getPredicate().getUnicodeString();
            if(propUri.startsWith(EHPROP_NS)){
                String prop = propUri.substring(EHPROP_NS_LENGTH);
                RDFTerm resource = t.getObject();
                Object value = extractEnhancementPropertyValue(resource);
                if(value != null && !prop.isEmpty()){
                    Object current = properties.get(prop);
                    if(log.isDebugEnabled()){
                        if(current != null){
                            log.debug(" ... append {} property '{}' to {}='{}'", 
                                new Object[]{level, value, prop,current});
                        } else {
                            log.debug(" ... add {} property {}='{}'", 
                                new Object[]{level, prop, value});
                        }
                    }
                    if(current instanceof Collection<?>){
                        ((Collection) current).add(value);
                    } else if(current != null){
                        Collection<Object> col = new ArrayList<Object>(4);
                        col.add(current);
                        col.add(value);
                        properties.put(prop, col);
                    } else {
                        properties.put(prop, value);
                    }
                }
            }
        }
    }

    /**
     * Extracts the EnhancementProperty value from the parsed RDFTerm.<p>
     * Currently this will return {@link IRI#getUnicodeString()} or
     * {@link Literal#getLexicalForm()}. For {@link BlankNode}s <code>null</code> 
     * is returned.
     * @param r the resource to parse the value form
     * @return the parsed value
     */
    private static Object extractEnhancementPropertyValue(RDFTerm r) {
        Object value;
        if(r instanceof IRI){
            value = ((IRI)r).getUnicodeString();
        } else if(r instanceof Literal){
            value = ((Literal) r).getLexicalForm();
        } else {
            value = null;
        }
        return value;
    }
    
    /**
     * Getter for all configuration values for the parsed property.<p>
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned. <code>NULL</code>
     * value contained in the parsed value will be silently removed.
     * @param config the OSGI component configuration.
     * @param property the configuration property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @return the configuration values for the parsed property or <code>null</code>
     * if the property was not contained in the parsed configuration
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws ConfigurationException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float).
     * @since 0.12.1
     */
    public static <T> Collection<T> getConfigValues(Dictionary<String,Object> config,
        String property, Class<T> type) throws ConfigurationException {
        if(config == null){
            throw new NullPointerException("The parsed Dictionary with the configuration MUST NOT be NULL!");
        }
        if(property == null){
            throw new NullPointerException("The parsed configuration property MUST NOT be NULL!");
        }
        try {
            return parseConfigValues(config.get(property),type);
        } catch (IllegalStateException e){
            throw new ConfigurationException(property, e.getMessage(),e);
        }
    }
    /**
     * Getter for all configuration values for the parsed enhancement property.<p>
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned. <code>NULL</code>
     * value contained in the parsed value will be silently removed.
     * @param ee the enhancement engine (only used to report errors
     * @param ci the content item (only used to report errors)
     * @param enhProps the enhancement properties as parsed the the engine with
     * the parsed content item
     * @param enhProp the enhancement property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @return the configuration values for the parsed property or <code>null</code>
     * if the property was not contained in the parsed enhancement properties
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws EnhancementPropertyException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float).
     * @since 0.12.1
     */
    public static <T> Collection<T> getConfigValues(EnhancementEngine ee,
        ContentItem ci, Map<String,Object> enhProps, String enhProp, Class<T> type)
        throws EnhancementPropertyException {
        if(enhProp == null){
            throw new NullPointerException("The parsed EnhancementProperty MUST NOT be NULL");
        }
        if(enhProps == null){
            throw new NullPointerException("The parsed Map with the EnhancementProperties MUST NOT be NULL");
        }
        try {
            return parseConfigValues(enhProps.get(enhProp), type);
        } catch(IllegalStateException e){
            throw new EnhancementPropertyException(ee, ci, enhProp, e.getMessage(),e);
        }
    }
    
    /**
     * Extracts multiple Configuration values from the parsed Object value.
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned. <code>NULL</code>
     * value contained in the parsed value will be silently removed.
     * @param value the value. {@link Collection}s and Arrays are supported for
     * multiple values. If the parsed value is of an other type a single value
     * is assumed.
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @return the configuration values as parsed from the parsed value
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws IllegalStateException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float.
     * @since 0.12.1
     */
    public static <T> Collection<T> parseConfigValues(Object value, Class<T> type){
        return parseConfigValues(value, type, false);
    }
    
    /**
     * Getter for all configuration values for the parsed property.<p>
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned.
     * @param config the OSGI component configuration.
     * @param property the configuration property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration values for the parsed property or <code>null</code>
     * if the property was not contained in the parsed configuration
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws ConfigurationException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float).
     * @since 0.12.1
     */
    public static <T> Collection<T> getConfigValues(Dictionary<String,Object> config,
        String property, Class<T> type, boolean preserveNullValues) throws ConfigurationException {
        if(config == null){
            throw new NullPointerException("The parsed Dictionary with the configuration MUST NOT be NULL!");
        }
        if(property == null){
            throw new NullPointerException("The parsed configuration property MUST NOT be NULL!");
        }
        try {
            return parseConfigValues(config.get(property),type, preserveNullValues);
        } catch (IllegalStateException e){
            throw new ConfigurationException(property, e.getMessage(),e);
        }
    }
    /**
     * Getter for all configuration values for the parsed enhancement property.<p>
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned.
     * @param ee the enhancement engine (only used to report errors
     * @param ci the content item (only used to report errors)
     * @param enhProps the enhancement properties as parsed the the engine with
     * the parsed content item
     * @param enhProp the enhancement property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration values for the parsed property or <code>null</code>
     * if the property was not contained in the parsed enhancement properties.
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws EnhancementPropertyException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float).
     * @since 0.12.1
     */
    public static <T> Collection<T> getConfigValues(EnhancementEngine ee,
        ContentItem ci, Map<String,Object> enhProps, String enhProp, Class<T> type,
        boolean preserveNullValues) throws EnhancementPropertyException {
        if(enhProp == null){
            throw new NullPointerException("The parsed EnhancementProperty MUST NOT be NULL");
        }
        if(enhProps == null){
            throw new NullPointerException("The parsed Map with the EnhancementProperties MUST NOT be NULL");
        }
        try {
            return parseConfigValues(enhProps.get(enhProp), type, preserveNullValues);
        } catch(IllegalStateException e){
            throw new EnhancementPropertyException(ee, ci, enhProp, e.getMessage(),e);
        }
    }
    
    /**
     * Extracts multiple Configuration values from the parsed Object value.
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned.
     * @param value the value. {@link Collection}s and Arrays are supported for
     * multiple values. If the parsed value is of an other type a single value
     * is assumed.
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration values as parsed from the parsed value
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws IllegalStateException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float.
     * @since 0.12.1
     */
    public static <T> Collection<T> parseConfigValues(Object value, Class<T> type,
        boolean preseveNullValues){
        return parseConfigValues(value,type,  null, preseveNullValues);
    }
    /**
     * Getter for all configuration values for the parsed property.<p>
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned. <code>NULL</code>
     * value contained in the parsed value will be silently removed.
     * @param config the OSGI component configuration.
     * @param property the configuration property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param configValues The collection to add the parsed configuration values
     * to. If <code>null</code> an {@link ArrayList} will be used.
     * @return the configuration values for the parsed property or <code>null</code>
     * if the property was not contained in the parsed configuration
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws ConfigurationException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float).
     * @since 0.12.1
     */
    public static <T> Collection<T> getConfigValues(Dictionary<String,Object> config,
        String property, Class<T> type, Collection<T> configValues) throws ConfigurationException {
        if(config == null){
            throw new NullPointerException("The parsed Dictionary with the configuration MUST NOT be NULL!");
        }
        if(property == null){
            throw new NullPointerException("The parsed configuration property MUST NOT be NULL!");
        }
        try {
            return parseConfigValues(config.get(property),type, configValues);
        } catch (IllegalStateException e){
            throw new ConfigurationException(property, e.getMessage(),e);
        }
    }
    /**
     * Getter for all configuration values for the parsed enhancement property.<p>
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned. <code>NULL</code>
     * value contained in the parsed value will be silently removed.
     * @param ee the enhancement engine (only used to report errors
     * @param ci the content item (only used to report errors)
     * @param enhProps the enhancement properties as parsed the the engine with
     * the parsed content item
     * @param enhProp the enhancement property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param configValues The collection to add the parsed configuration values
     * to. If <code>null</code> an {@link ArrayList} will be used.
     * @return the configuration values for the parsed property or <code>null</code>
     * if the property was not contained in the parsed enhancement properties
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws EnhancementPropertyException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float).
     * @since 0.12.1
     */
    public static <T> Collection<T> getConfigValues(EnhancementEngine ee,
        ContentItem ci, Map<String,Object> enhProps, String enhProp, Class<T> type
        ,Collection<T> configValues) throws EnhancementPropertyException {
        if(enhProp == null){
            throw new NullPointerException("The parsed EnhancementProperty MUST NOT be NULL");
        }
        if(enhProps == null){
            throw new NullPointerException("The parsed Map with the EnhancementProperties MUST NOT be NULL");
        }
        try {
            return parseConfigValues(enhProps.get(enhProp), type, configValues);
        } catch(IllegalStateException e){
            throw new EnhancementPropertyException(ee, ci, enhProp, e.getMessage(),e);
        }
    }

    /**
     * Extracts multiple Configuration values from the parsed Object value.
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned.
     * @param value the value. {@link Collection}s and Arrays are supported for
     * multiple values. If the parsed value is of an other type a single value
     * is assumed.
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param configValues The collection to add the parsed configuration values
     * to. If <code>null</code> an {@link ArrayList} will be used.
     * @return the configuration values as parsed from the parsed value. 
     * <code>null</code> if the parsed value was <code>null</code>.
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws IllegalStateException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float.
     * @since 0.12.1
     */
    public static <T> Collection<T> parseConfigValues(Object value, 
        Class<T> type, Collection<T> configValues){
        return parseConfigValues(value, type, configValues,false);
    }
    
    /**
     * Getter for all configuration values for the parsed property.<p>
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned.
     * @param config the OSGI component configuration.
     * @param property the configuration property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param configValues The collection to add the parsed configuration values
     * to. If <code>null</code> an {@link ArrayList} will be used.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration values for the parsed property or <code>null</code>
     * if the property was not contained in the parsed configuration
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws ConfigurationException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float).
     * @since 0.12.1
     */
    public static <T> Collection<T> getConfigValues(Dictionary<String,Object> config,
        String property, Class<T> type, Collection<T> configValues, boolean preserveNullValues) 
                throws ConfigurationException {
        if(config == null){
            throw new NullPointerException("The parsed Dictionary with the configuration MUST NOT be NULL!");
        }
        if(property == null){
            throw new NullPointerException("The parsed configuration property MUST NOT be NULL!");
        }
        try {
            return parseConfigValues(config.get(property),type, configValues, preserveNullValues);
        } catch (IllegalStateException e){
            throw new ConfigurationException(property, e.getMessage(),e);
        }
    }
    /**
     * Getter for all configuration values for the parsed enhancement property.<p>
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned.
     * @param ee the enhancement engine (only used to report errors
     * @param ci the content item (only used to report errors)
     * @param enhProps the enhancement properties as parsed the the engine with
     * the parsed content item
     * @param enhProp the enhancement property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param configValues The collection to add the parsed configuration values
     * to. If <code>null</code> an {@link ArrayList} will be used.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration values for the parsed property or <code>null</code>
     * if the property was not contained in the parsed enhancement properties
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws EnhancementPropertyException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float).
     * @since 0.12.1
     */
    public static <T> Collection<T> getConfigValues(EnhancementEngine ee,
        ContentItem ci, Map<String,Object> enhProps, String enhProp, Class<T> type
        ,Collection<T> configValues, boolean preserveNullValues) throws EnhancementPropertyException {
        if(enhProp == null){
            throw new NullPointerException("The parsed EnhancementProperty MUST NOT be NULL");
        }
        if(enhProps == null){
            throw new NullPointerException("The parsed Map with the EnhancementProperties MUST NOT be NULL");
        }
        try {
            return parseConfigValues(enhProps.get(enhProp), type, configValues, preserveNullValues);
        } catch(IllegalStateException e){
            throw new EnhancementPropertyException(ee, ci, enhProp, e.getMessage(),e);
        }
    }
    /**
     * Extracts multiple Configuration values from the parsed Object value.
     * This does support arrays and {@link Collection}s for multiple values.
     * In any other case a single value collection will be returned.
     * @param value the value. {@link Collection}s and Arrays are supported for
     * multiple values. If the parsed value is of an other type a single value
     * is assumed.
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param configValues The collection to add the parsed configuration values
     * to. If <code>null</code> an {@link ArrayList} will be used.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration values as parsed from the parsed value. 
     * <code>null</code> if the parsed value was <code>null</code>.
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws IllegalStateException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float.
     * @since 0.12.1
     */
    public static <T> Collection<T>  parseConfigValues(Object value, 
        Class<T> type, Collection<T> configValues, boolean preseveNullValues){
        if(value == null){
            return null;
        }
        final Collection<?> values;
        if(value instanceof Collection<?>){
            values = (Collection<?>)value;
        } else if(value.getClass().isArray()){
            Class<?> componentType = value.getClass().getComponentType();
            if(componentType.isPrimitive()){
               int len = Array.getLength(value);
               List<Object> av = new ArrayList<Object>(len);
               for(int i = 0; i < len;i++){
                   av.add(Array.get(value, i));
               }
               values = av;
            } else {
                values = Arrays.asList((Object[])value);
            }
        } else {
            values = Collections.singleton(value);
        }
        final Constructor<T> constructor = getConfigTypeConstructor(type);
        if(configValues == null){
            //no idea why I have to cast to C ...
            configValues = new ArrayList<T>(values.size());
        }
        for(Object o : values){
            if(o == null){
                if(preseveNullValues){
                    configValues.add(null);
                } //else skip 
            } else {
                configValues.add(parseConfigValue(o, type, constructor));
            }
        }
        return configValues;
    }
    /**
     * Getter for the first configuration value
     * @param config the OSGI component configuration
     * @param property the configuration property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration value as parsed from the parsed value
     * @throws ConfigurationException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float.
     * @throws NullPointerException if the parsed {@link Dictionary} with the component
     * configuration, the configuration property or the type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @since 0.12.1
     */
    public static final <T> T getFirstConfigValue(Dictionary<String,Object> config,
            String property,Class<T> type) throws ConfigurationException {
        if(config == null){
            throw new NullPointerException("The parsed configuration MUST NOT be NULL!");
        }
        if(property == null){
            throw new NullPointerException("The pased configuration property MUST NOT be NULL!");
        }
        try {
            return parseFirstConfigValue(config.get(property), type);
        } catch(IllegalStateException e){
            throw new ConfigurationException(property, e.getMessage(),e);
        }
    }
    /**
     * Getter for the first value of an EnhancementProperty
     * @param ee the enhancement engine (only used for reporting errors)
     * @param ci the content item (only used for reporting errors)
     * @param enhProps the map with the enhancement properties
     * @param enhProp the enhancement property
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration value as parsed from the parsed value
     * @throws EnhancementPropertyException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float.
     * @throws NullPointerException if the parsed {@link Map} with the enhancement
     * properties, the enhancement property or the type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @since 0.12.1
     */
    public static final <T> T getFirstConfigValue(EnhancementEngine ee, 
            ContentItem ci, Map<String,Object> enhProps,
            String enhProp, Class<T> type) throws EnhancementPropertyException {
        if(enhProp == null){
            throw new NullPointerException("The parsed EnhancementProperty MUST NOT be NULL");
        }
        if(enhProps == null){
            throw new NullPointerException("The parsed Map with the EnhancementProperties MUST NOT be NULL");
        }
        try {
            return parseFirstConfigValue(enhProps.get(enhProp), type);
        } catch(IllegalStateException e){
            throw new EnhancementPropertyException(ee, ci, enhProp, e.getMessage(),e);
        }
    }
    
    /**
     * Extracts a single Configuration values from the parsed Object value.
     * In case the parsed value is an Array or a Collection it will take the
     * first non <code>null</code> value.
     * @param value the value. In case of an Array or a Collection it will take
     * the first non <code>null</code> value
     * @param type the desired type of the configuration values. The parsed type
     * MUST define a {@link Constructor} taking a {@link String} as only parameter.
     * @param preseveNullValues if <code>null</code> values in the parsed
     * value should be preserved or removed.
     * @return the configuration value as parsed from the parsed value
     * @throws NullPointerException if the parsed type is <code>null</code>
     * @throws IllegalArgumentException if the parsed type does not have a
     * {@link Constructor} that takes a {@link String} as only parameter; if the
     * {@link Constructor} is not visible or can not be instantiated (e.g.
     * because the parsed type is an Interface or an abstract class).
     * @throws IllegalStateException if the parsed type can not be instantiated
     * if one of the parsed values (e.g. if {@link Float} is used as type and
     * one of the parsed values is not a valid float.
     * @since 0.12.1
     */
    public static final <T> T parseFirstConfigValue(Object value, Class<T> type){
        if(value == null){
            return null;
        }
        Object first = null;
        if(value instanceof Collection<?>){
            Collection<?> c = (Collection<?>)value;
            if(c.isEmpty()){
                return null;
            } else {
                Iterator<?> it = c.iterator();
                while(first == null && it.hasNext()){
                    first = it.next();
                }
            }
        } else if(value.getClass().isArray()){
            Class<?> componentType = value.getClass().getComponentType();
            int len = Array.getLength(value);
            if(len < 1){
                return null;
            } else {
                if(componentType.isPrimitive()){
                   first = Array.get(value, 0);
                } else {
                   for(int i=0; first == null && i < len; i++){
                       first = Array.get(value, i);
                   }
                }
            }
        } else {
            first = value;
        }
        return parseConfigValue(first, type, getConfigTypeConstructor(type));
    }

    /**
     * Internally used to get the config value for the parsed value and type.
     * @param value
     * @param type
     * @param constructor the constructor typically retrieved by calling
     * {@link #getConfigTypeConstructor(Class)} for the type
     * @return the value
     */
    private static <T> T parseConfigValue(Object value, Class<T> type, final Constructor<T> constructor) {
        if(value == null){
            return null;
        }
        T configValue;
        if(constructor == null){
            configValue = type.cast(value.toString());
        } else {
            try {
                configValue = constructor.newInstance(value.toString());
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Unable to instantiate the "
                    + "parsed value type '" + type.getClass().getName() +"'!", e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Unable to access the "
                        + "constructor of the parsed value type '" 
                        + type.getClass().getName() + "'!", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Unable to instantiate the "
                        + "parsed value type '" + type.getClass().getName() 
                        + "' with the String value '"+value+ "'!", e);
            }
        }
        return configValue;
    }

    /**
     * Internally used to get the String parameter constructor for the parsed
     * config value type
     * @param type
     * @return
     */
    private static <T> Constructor<T> getConfigTypeConstructor(Class<T> type) {
        final Constructor<T> constructor;
        if(String.class.equals(type)){
            constructor = null;
        } else {
            try {
                constructor = type.getConstructor(String.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Parsed config value type '"
                    + type.getClass().getName()+ "' does not define a Constructor "
                    + "that takes a String as only parameter!", e);
            }
        }
        return constructor;
    }
}
