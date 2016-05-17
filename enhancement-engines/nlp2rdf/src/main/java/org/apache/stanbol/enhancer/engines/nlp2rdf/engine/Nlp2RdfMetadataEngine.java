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

import static org.apache.stanbol.enhancer.nlp.utils.NIFHelper.writePhrase;
import static org.apache.stanbol.enhancer.nlp.utils.NIFHelper.writePos;
import static org.apache.stanbol.enhancer.nlp.utils.NIFHelper.writeSpan;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotated;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.nif.SsoOntology;
import org.apache.stanbol.enhancer.nlp.nif.StringOntology;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.utils.NIFHelper;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
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
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="nlp2rdf")
})
public class Nlp2RdfMetadataEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties{

    private final Logger log = LoggerFactory.getLogger(Nlp2RdfMetadataEngine.class);
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
        Dictionary<String, Object> properties = ce.getProperties();
        //TODO: read configuration
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
                //(2) add the relations between the different spans
                switch (span.getType()) {
                    case Sentence:
                        if(sentence != null){
                            metadata.add(new TripleImpl(sentence, SsoOntology.nextSentence.getUri(), current));
                        }
                        sentence = current;
                        firstWordInSentence = true;
                        break;
                    case Chunk:
                        if(sentence != null){
                            metadata.add(new TripleImpl(current, StringOntology.superString.getUri(), sentence));
                            if(word != null){
                                metadata.add(new TripleImpl(word, SsoOntology.lastWord.getUri(), sentence));
                            }
                        }
                        phrase = current;
                        break;
                    case Token:
                        if(sentence != null){
                            metadata.add(new TripleImpl(current, SsoOntology.sentence.getUri(), sentence));
                            if(firstWordInSentence){
                                metadata.add(new TripleImpl(current, SsoOntology.firstWord.getUri(), sentence));
                                firstWordInSentence = false;
                            }
                        }
                        if(phrase != null){
                            metadata.add(new TripleImpl(current, SsoOntology.parent.getUri(), phrase));
                        }
                        if(word != null){
                            metadata.add(new TripleImpl(word, SsoOntology.nextWord.getUri(), current));
                            metadata.add(new TripleImpl(current, SsoOntology.previousWord.getUri(), word));
                        }
                        word = current;
                        break;
                    default:
                        break;
                }
                //(3) add specific information such as POS, chunk type ...
                writePos(metadata, span, current);
                writePhrase(metadata, span, current);
                //OlIA does not include Sentiments
                
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





}
