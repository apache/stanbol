package org.apache.stanbol.enhancer.nlp.utils;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Span.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotated;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.nif.SsoOntology;
import org.apache.stanbol.enhancer.nlp.nif.StringOntology;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;

public final class NIFHelper {
    
    private static final LiteralFactory lf = LiteralFactory.getInstance();

    private NIFHelper(){}
    
    public static final Map<SpanTypeEnum,UriRef> SPAN_TYPE_TO_SSO_TYPE;
    static {
        Map<SpanTypeEnum,UriRef> mapping = new EnumMap<SpanTypeEnum,UriRef>(SpanTypeEnum.class);
        //mapping.put(SpanTypeEnum.Text, null);
        //mapping.put(SpanTypeEnum.TextSection, null);
        mapping.put(SpanTypeEnum.Sentence, SsoOntology.Sentence.getUri());
        mapping.put(SpanTypeEnum.Chunk, SsoOntology.Phrase.getUri());
        mapping.put(SpanTypeEnum.Token, SsoOntology.Word.getUri());
        SPAN_TYPE_TO_SSO_TYPE = Collections.unmodifiableMap(mapping);
    }
    
    /**
     * Read-only map that maps from the {@link LexicalCategory} to the OLIA
     * Concept representing the Phrase (e.g. {@link LexicalCategory#Noun} maps
     * to "<code>http://purl.org/olia/olia.owl#NounPhrase</code>").
     */
    public static final Map<LexicalCategory,UriRef> LEXICAL_TYPE_TO_PHRASE_TYPE;
    static {
        String olia = "http://purl.org/olia/olia.owl#";
        Map<LexicalCategory,UriRef> mapping = new EnumMap<LexicalCategory,UriRef>(LexicalCategory.class);
        mapping.put(LexicalCategory.Noun, new UriRef(olia+"NounPhrase"));
        mapping.put(LexicalCategory.Verb, new UriRef(olia+"VerbPhrase"));
        mapping.put(LexicalCategory.Adjective, new UriRef(olia+"AdjectivePhrase"));
        mapping.put(LexicalCategory.Adverb, new UriRef(olia+"AdverbPhrase"));
        mapping.put(LexicalCategory.Conjuction, new UriRef(olia+"ConjuctionPhrase"));
        LEXICAL_TYPE_TO_PHRASE_TYPE = Collections.unmodifiableMap(mapping);
    }    
    /**
     * Creates a NIF2.0 Fragment URI using the parsed base URI and the start/end
     * indexes.
     * @param base the base URI
     * @param start the start position. If <code>&lt; 0</code> than zero is added.
     * @param end the end position or values &lt; 1 when open ended.
     * @return the NIF 2.0 Fragment URI
     * @throws IllegalArgumentException if <code>null</code> is parsed as base
     * {@link UriRef} or the end position is &gt;=0 but &lt= the parsed start
     * position.
     */
    public static final UriRef getNifFragmentURI(UriRef base, int start,int end){
        if(base == null){
            throw new IllegalArgumentException("Base URI MUST NOT be NULL!");
        }
        StringBuilder sb = new StringBuilder(base.getUnicodeString());
        sb.append("#char=");
        sb.append(start >= 0 ? start : 0).append(',');
        if(end >= 0){
            if(end < start){
                throw new IllegalArgumentException("End index '"+end+"' < start '"+start+"'!");
            }
            sb.append(end);
        } //else open ended ...
        return new UriRef(sb.toString());
    }
 
    public static final UriRef getNifOffsetURI(UriRef base, int start, int end){
        if(base == null){
            throw new IllegalArgumentException("Base URI MUST NOT be NULL!");
        }
        StringBuilder sb = new StringBuilder(base.getUnicodeString());
        sb.append("#offset_");
        sb.append(start >= 0 ? start : 0).append('_');
        if(end >= 0){
            if(end < start){
                throw new IllegalArgumentException("End index '"+end+"' < start '"+start+"'!");
            }
            sb.append(end);
        } //else open ended ...
        return new UriRef(sb.toString());
    }
    
    public static final int NIF_HASH_CONTEXT_LENGTH = 10;
    public static final int NIF_HASH_MAX_STRING_LENGTH = 20;
    
    public static final Charset UTF8 = Charset.forName("UTF8");
    
    public static final UriRef getNifHashURI(UriRef base, int start, int end, String text){
        if(base == null){
            throw new IllegalArgumentException("Base URI MUST NOT be NULL!");
        }
        start = start < 0 ? 0 : start;
        end = end < 0 ? start : end;
        if(end < start){
            throw new IllegalArgumentException("End index '"+end+"' < start '"+start+"'!");
        }
        if(end >= text.length()){
            throw new IllegalArgumentException("The End index '"+end+"' exeeds the "
                + "length of the text '"+text.length()+"'!");
        }
        int contextStart = Math.max(0, start-NIF_HASH_CONTEXT_LENGTH);
        int contextEnd = Math.min(text.length(), end+NIF_HASH_CONTEXT_LENGTH);
        StringBuilder sb = new StringBuilder(base.getUnicodeString());
        sb.append("#hash_");
        sb.append(NIF_HASH_CONTEXT_LENGTH);
        sb.append('_');
        sb.append(end-start);
        sb.append('_');
        sb.append(getContextDigest(text, contextStart, start, end, contextEnd));
        sb.append('_');
        sb.append(text.substring(start, 
            Math.min(end,start+NIF_HASH_MAX_STRING_LENGTH)));
        return new UriRef(sb.toString());
    }

    /**
     * Creates the UTF8 byte representation for the '{prefix}({selected}){suffix}'
     * calculated based on the parsed parameters
     * @param text the text
     * @param contextStart the start index of the prefix
     * @param start the start index of the selected text part
     * @param end the end index of the selecte text part
     * @param contextEnd the end index of the suffix
     * @return the HASH string representation of the MD5 over 
     *  <code>'{prefix}({selected}){suffix}'</code> (NOTE the brackets that are
     *  added at the start/end of the selected text)
     */
    private static String getContextDigest(String text, int contextStart, int start, int end, int contextEnd) {
        ByteArrayOutputStream contextOs = new ByteArrayOutputStream();
        Writer contextWriter = new OutputStreamWriter(contextOs, UTF8);
        try {
            if(contextStart<start){
                contextWriter.append(text, contextStart, start);
            }
            contextWriter.append('(');
            if(start < end){
                contextWriter.append(text, start, end);
            }
            contextWriter.append(')');
            if(end < contextEnd){
                contextWriter.append(text,end,contextEnd);
            }
            contextWriter.flush();
            return ContentItemHelper.streamDigest(
                new ByteArrayInputStream(contextOs.toByteArray()),
                null, "MD5");
        } catch (IOException e) {
            //NO IOExceptions in in-memory stream implementations
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(contextOs);
        }
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
     * @return the {@link UriRef} representing the parsed {@link Span} in the
     * graph
     */
    public static UriRef writeSpan(MGraph graph, UriRef base, AnalysedText text, Language language, Span span){
        UriRef segment = getNifOffsetURI(base, span.getStart(), span.getEnd());
        graph.add(new TripleImpl(segment, RDF_TYPE, StringOntology.OffsetBasedString.getUri()));
        graph.add(new TripleImpl(segment, StringOntology.anchorOf.getUri(), 
            new PlainLiteralImpl(span.getSpan(),language)));
        graph.add(new TripleImpl(segment, StringOntology.beginIndex.getUri(), 
            lf.createTypedLiteral(span.getStart())));
        graph.add(new TripleImpl(segment, StringOntology.endIndex.getUri(), 
            lf.createTypedLiteral(span.getEnd())));
        switch (span.getType()) {
            case Token:
                graph.add(new TripleImpl(segment, RDF_TYPE, SsoOntology.Word.getUri()));
                break;
            case Chunk:
                graph.add(new TripleImpl(segment, RDF_TYPE, SsoOntology.Phrase.getUri()));
                break;
            case Sentence:
                graph.add(new TripleImpl(segment, RDF_TYPE, SsoOntology.Sentence.getUri()));
                break;
//            case Text:
//                graph.add(new TripleImpl(segment, RDF_TYPE, StringOntology.Document.getUri()));
            //no default:
        }
        return segment;
    }
    
    /**
     * Writes the {@link NlpAnnotations#POS_ANNOTATION} as NIF 1.0 to the parsed
     * RDF graph by using the parsed segmentUri as subject
     * @param graph the graph
     * @param annotated the annotated element (e.g. a {@link Token})
     * @param segmentUri the URI of the resource representing the parsed 
     * annotated element in the graph
     */
    public static void writePos(MGraph graph, Annotated annotated, UriRef segmentUri) {
        Value<PosTag> posTag = annotated.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        if(posTag != null){
            if(posTag.value().isMapped()){
                for(Pos pos : posTag.value().getPos()){
                    graph.add(new TripleImpl(segmentUri, SsoOntology.oliaLink.getUri(), 
                        pos.getUri()));
                }
                for(LexicalCategory cat : posTag.value().getCategories()){
                    graph.add(new TripleImpl(segmentUri, SsoOntology.oliaLink.getUri(), 
                        cat.getUri()));
                }
            }
            graph.add(new TripleImpl(segmentUri, SsoOntology.posTag.getUri(), 
                lf.createTypedLiteral(posTag.value().getTag())));
            graph.add(new TripleImpl(segmentUri, ENHANCER_CONFIDENCE, 
                lf.createTypedLiteral(posTag.probability())));
        }
    }    
    
    /**
     * Writes a {@link NlpAnnotations#PHRASE_ANNOTATION} as NIF 1.0 to the
     * parsed RDF graph by using the segmentUri as subject
     * @param graph the graph
     * @param annotated the annotated element (e.g. a {@link Chunk})
     * @param segmentUri the URI of the resource representing the parsed 
     * annotated element in the graph
     */
    public static void writePhrase(MGraph graph, Annotated annotated, UriRef segmentUri) {
        Value<PhraseTag> phraseTag = annotated.getAnnotation(NlpAnnotations.PHRASE_ANNOTATION);
        if(phraseTag != null){
            UriRef phraseTypeUri = LEXICAL_TYPE_TO_PHRASE_TYPE.get(phraseTag.value().getCategory());
            if(phraseTypeUri != null){ //add the oliaLink for the Phrase
                graph.add(new TripleImpl(segmentUri, SsoOntology.oliaLink.getUri(), phraseTypeUri));
                graph.add(new TripleImpl(segmentUri, ENHANCER_CONFIDENCE, 
                    lf.createTypedLiteral(phraseTag.probability())));
            }
        }
    }

}
