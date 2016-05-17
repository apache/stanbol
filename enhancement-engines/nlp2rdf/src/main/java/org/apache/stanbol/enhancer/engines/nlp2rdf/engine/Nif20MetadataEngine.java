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
package org.apache.stanbol.enhancer.engines.nlp2rdf.engine;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.DEFAULT_PREFIX_SUFFIX_LENGTH;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.nif.Nif20;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true, 
    configurationFactory = true, //allow multiple configuration
    policy = ConfigurationPolicy.OPTIONAL) //create a default instance
@Service
@Properties(value={
        @Property(name=EnhancementEngine.PROPERTY_NAME,value="nif20"),
        @Property(name=Nif20MetadataEngine.PROP_SELECTOR_STATE, 
            boolValue=Nif20MetadataEngine.DEFAULT_SELECTOR_STATE),
        @Property(name=Nif20MetadataEngine.PROP_CONTEXT_ONLY_URI_SCHEME, 
            boolValue=Nif20MetadataEngine.DEFAULT_CONTEXT_ONLY_URI_SCHEME),
        @Property(name=Nif20MetadataEngine.PROP_WRITE_STRING_TYPE, 
            boolValue=Nif20MetadataEngine.DEFAULT_WRITE_STRING_TYPE),
        @Property(name=Nif20MetadataEngine.PROP_HIERARCHY_LINKS_STATE,
            boolValue=Nif20MetadataEngine.DEFAULT_HIERARCHY_LINKS_STATE),
        @Property(name=Nif20MetadataEngine.PROP_PREVIOUSE_NEXT_LINKS_STATE, 
            boolValue=Nif20MetadataEngine.DEFAULT_PREVIOUSE_NEXT_LINKS_STATE)
})
public class Nif20MetadataEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties{

    /**
     * Switch that allows to enable/disable writing of hierarchical links. This
     * includes <code>olia:sentence</code>, <code>olia:superString</code> and
     * <code>olia:subString</code> properties.
     */
    public final static String PROP_HIERARCHY_LINKS_STATE = "enhancer.engines.nlp2rdf.hierarchy";
    /**
     * switch that allows to enable/disable writing of next and previous links 
     * between words, sentences ...
     */
    public final static String PROP_PREVIOUSE_NEXT_LINKS_STATE = "enhancer.engines.nlp2rdf.previousNext";
    /**
     * If enabled Selector related properties such as begin-/end-index, before/after, ...
     * are written. If disabled only the URI is generated (which is sufficient for
     * clients that know about the semantics of how the URI is build). Deactivating
     * this will greatly decrease the triple count.
     */
    public final static String PROP_SELECTOR_STATE = "enhancer.engines.nlp2rdf.selector";
    /**
     * If enabled the {@link Nif20#URIScheme nif:URIScheme} ( typically 
     * {@link Nif20#RFC5147String nif:RFC5147String}) type will only be added 
     * for the {@link Nif20#Context nif:Context} and not
     * all {@link Nif20#String nif:String} instances. If enabled clients need 
     * follow the {@link Nif20#referenceContext nif:referenceContext} to the 
     * {@link Nif20#Context nif:Context} for getting the used 
     * {@link Nif20#URIScheme nif:URIScheme}<p>
     */
    public final static String PROP_CONTEXT_ONLY_URI_SCHEME = "enhancer.engines.nlp2rdf.cotextOnlyUriScheme";
    /**
     * If enabled the {@link Nif20#String nif:String} type is added to all written
     * String. If disabled it is only written if their is no more specific type (
     * such as {@link Nif20#Sentence nif:Sentence} or {@link Nif20#Word nif:Word}.
     */
    public final static String PROP_WRITE_STRING_TYPE = "enhancer.engines.nlp2rdf.writeStringType";
    
    public static final boolean DEFAULT_HIERARCHY_LINKS_STATE = true;
    public static final boolean DEFAULT_PREVIOUSE_NEXT_LINKS_STATE = true;
    public static final boolean DEFAULT_SELECTOR_STATE = true;
    public static final boolean DEFAULT_CONTEXT_ONLY_URI_SCHEME = false;
    public static final boolean DEFAULT_WRITE_STRING_TYPE = false; 

    
    private boolean writeHierary = DEFAULT_HIERARCHY_LINKS_STATE;
    private boolean writePrevNext = DEFAULT_PREVIOUSE_NEXT_LINKS_STATE;
    private boolean writeSelectors = DEFAULT_SELECTOR_STATE;
    private boolean contextOnlyUriScheme = DEFAULT_CONTEXT_ONLY_URI_SCHEME;
    private boolean writeStringType = DEFAULT_WRITE_STRING_TYPE;
    
    private final Logger log = LoggerFactory.getLogger(Nif20MetadataEngine.class);
    //TODO: replace this with a reald ontology
    private final static IRI SENTIMENT_PROPERTY = new IRI(NamespaceEnum.fise+"sentiment-value");
    private final LiteralFactory lf = LiteralFactory.getInstance();
    
    /**
     * Activate and read the properties. Configures and initialises a ChunkerHelper for each language configured in
     * CONFIG_LANGUAGES.
     *
     * @param ce the {@link org.osgi.service.component.ComponentContext}
     */
    @Activate
    protected void activate(ComponentContext ce) throws ConfigurationException {
        log.info("activating POS tagging engine");
        super.activate(ce);
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = ce.getProperties();
        writeHierary = getState(props, PROP_HIERARCHY_LINKS_STATE, DEFAULT_HIERARCHY_LINKS_STATE);
        writePrevNext = getState(props, PROP_PREVIOUSE_NEXT_LINKS_STATE, DEFAULT_PREVIOUSE_NEXT_LINKS_STATE);
        writeSelectors = getState(props, PROP_SELECTOR_STATE, DEFAULT_SELECTOR_STATE);
        contextOnlyUriScheme = getState(props, PROP_CONTEXT_ONLY_URI_SCHEME, DEFAULT_CONTEXT_ONLY_URI_SCHEME);
        writeStringType = getState(props, PROP_WRITE_STRING_TYPE, DEFAULT_WRITE_STRING_TYPE);
    }
    
    private boolean getState(Dictionary<String, Object> props,
            String prop, boolean def) {
        Object val = props.get(prop);
        boolean state = val == null ? def : val instanceof Boolean ? ((Boolean)val).booleanValue() :
            Boolean.parseBoolean(val.toString());
        log.debug(" - {}: {}",prop,state);
        return state;
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        return getAnalysedText(this, ci, false) != null ? 
                ENHANCE_ASYNC : CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at = getAnalysedText(this, ci, true);
        String lang = EnhancementEngineHelper.getLanguage(ci);
        Language language = lang == null ? null : new Language(lang);
        //now iterate over the AnalysedText data and create the RDF representation
        //TODO: make configureable
        boolean sentences = true;
        boolean phrases = true;
        boolean words = true;
        
        EnumSet<SpanTypeEnum> activeTypes = EnumSet.noneOf(SpanTypeEnum.class);
        if(sentences){
            activeTypes.add(SpanTypeEnum.Sentence);
        }
        if(phrases){
            activeTypes.add(SpanTypeEnum.Chunk);
        }
        if(words){
            activeTypes.add(SpanTypeEnum.Token);
        }
        Graph metadata = ci.getMetadata();
        IRI base = ci.getUri();
        ci.getLock().writeLock().lock();
        try {
            //write the context
            IRI text = writeSpan(metadata, base, at, language, at);
            metadata.add(new TripleImpl(text, Nif20.sourceUrl.getUri(), ci.getUri()));
            
            Iterator<Span> spans = at.getEnclosed(activeTypes);
            IRI sentence = null;
            IRI phrase = null;
            IRI word = null;
            boolean firstWordInSentence = true;
            while(spans.hasNext()){
                Span span = spans.next();
                //TODO: filter Spans based on additional requirements
                //(1) write generic information about the span
                IRI current = writeSpan(metadata, base, at, language, span);
                //write the context
                metadata.add(new TripleImpl(current, Nif20.referenceContext.getUri(), text));
                //(2) add the relations between the different spans
                switch (span.getType()) {
                    case Sentence:
                        if(sentence != null && writePrevNext){
                            metadata.add(new TripleImpl(sentence, Nif20.nextSentence.getUri(), current));
                            metadata.add(new TripleImpl(current, Nif20.previousSentence.getUri(), sentence));
                        }
                        if(word != null){
                            metadata.add(new TripleImpl(sentence, Nif20.lastWord.getUri(), word));
                        }
                        sentence = current;
                        firstWordInSentence = true;
                        break;
                    case Chunk:
                        if(sentence != null && writeHierary){
                            metadata.add(new TripleImpl(current, Nif20.superString.getUri(), sentence));
                        }
                        phrase = current;
                        break;
                    case Token:
                        if(sentence != null){
                            if(writeHierary){
                                metadata.add(new TripleImpl(current, Nif20.sentence.getUri(), sentence));
                            }
                            //metadata.add(new TripleImpl(sentence, Nif20.word.getUri(), current));
                            if(firstWordInSentence){
                                metadata.add(new TripleImpl(sentence, Nif20.firstWord.getUri(), current));
                                firstWordInSentence = false;
                            }
                        }
                        if(writeHierary && phrase != null && !phrase.equals(current)){
                            metadata.add(new TripleImpl(current, Nif20.subString.getUri(), phrase));
                        }
                        if(word != null && writePrevNext){
                            metadata.add(new TripleImpl(word, Nif20.nextWord.getUri(), current));
                            metadata.add(new TripleImpl(current, Nif20.previousWord.getUri(), word));
                        }
                        word = current;
                        break;
                    default:
                        break;
                }
                //(3) add specific information such as POS, chunk type ...
                Nif20Helper.writePhrase(metadata, span, current);
                Nif20Helper.writePos(metadata, span, current);

                //TODO: sentiment support
                
                Value<Double> sentiment = span.getAnnotation(NlpAnnotations.SENTIMENT_ANNOTATION);
                if(sentiment != null && sentiment.value() != null){
                    metadata.add(new TripleImpl(current, SENTIMENT_PROPERTY, 
                        lf.createTypedLiteral(sentiment.value())));
                }
            }
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.singletonMap(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            (Object)ServiceProperties.ORDERING_POST_PROCESSING);
    }

    /**
     * Writes basic information of the parsed span by using NIF 1.0 including the
     * {@link SsoOntology} Sentence/Phrase/Word type based on 
     * the {@link Span#getType()}<p>
     * As {@link AnalysedText} is based on the plain text version of the ContentItem
     * this uses the {@link StringOntology#OffsetBasedString} notation.<p>
     * <i>NOTE:</i> This DOES NOT write string relations, lemma, pos ... information
     * that might be stored as {@link Annotation} with the parsed {@link Span}.
     * @param graph the graph to add the triples
     * @param base the base URI
     * @param text the {@link AnalysedText}
     * @param language the {@link Language} or <code>null</code> if not known
     * @param span the {@link Span} to write.
     * @return the {@link IRI} representing the parsed {@link Span} in the
     * graph
     */
    public IRI writeSpan(Graph graph, IRI base, AnalysedText text, Language language, Span span){
        IRI segment = Nif20Helper.getNifRFC5147URI(base, span.getStart(), 
                span.getType() == SpanTypeEnum.Text ? -1 : span.getEnd());
        if(!contextOnlyUriScheme || span.getType() == SpanTypeEnum.Text){
            graph.add(new TripleImpl(segment, RDF_TYPE, Nif20.RFC5147String.getUri()));
        }
        if(writeSelectors){
            if(span.getEnd() - span.getStart() < 100){
                graph.add(new TripleImpl(segment, Nif20.anchorOf.getUri(), 
                    new PlainLiteralImpl(span.getSpan(),language)));
            } else {
                graph.add(new TripleImpl(segment, Nif20.head.getUri(), 
                    new PlainLiteralImpl(span.getSpan().substring(0,10),language)));
            }
            graph.add(new TripleImpl(segment, Nif20.beginIndex.getUri(), 
                lf.createTypedLiteral(span.getStart())));
            graph.add(new TripleImpl(segment, Nif20.endIndex.getUri(), 
                lf.createTypedLiteral(span.getEnd())));
            String content = text.getSpan();
            if(span.getType() != SpanTypeEnum.Text){
                //prefix and suffix
                int prefixStart = Math.max(0, span.getStart() - DEFAULT_PREFIX_SUFFIX_LENGTH);
                graph.add(new TripleImpl(segment, Nif20.before.getUri(), new PlainLiteralImpl(
                        content.substring(prefixStart, span.getStart()), language)));
                int suffixEnd = Math.min(span.getEnd() + DEFAULT_PREFIX_SUFFIX_LENGTH, text.getEnd());
                graph.add(new TripleImpl(segment, Nif20.after.getUri(), new PlainLiteralImpl(
                        content.substring(span.getEnd(), suffixEnd), language)));
            }
        }
        if(writeStringType){
            graph.add(new TripleImpl(segment, RDF_TYPE, Nif20.String.getUri()));
        }
        switch (span.getType()) {
            case Token:
                graph.add(new TripleImpl(segment, RDF_TYPE, Nif20.Word.getUri()));
                break;
            case Chunk:
                graph.add(new TripleImpl(segment, RDF_TYPE, Nif20.Phrase.getUri()));
                break;
            case Sentence:
                graph.add(new TripleImpl(segment, RDF_TYPE, Nif20.Sentence.getUri()));
                break;
            case Text:
                graph.add(new TripleImpl(segment, RDF_TYPE, Nif20.Context.getUri()));
                break;
            default:
                if(!writeStringType){
                    graph.add(new TripleImpl(segment, RDF_TYPE, Nif20.String.getUri()));
                } //string type was already added
        }
        return segment;
    }



}
