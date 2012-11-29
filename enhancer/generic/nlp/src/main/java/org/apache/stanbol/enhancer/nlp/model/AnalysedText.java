package org.apache.stanbol.enhancer.nlp.model;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

/**
 * Provides access to NLP processing results of the <code>text/plain</code>
 * {@link Blob} of an ContentItem. Intended to be
 * {@link ContentItem#addPart(org.apache.clerezza.rdf.core.UriRef, Object) added
 * as ContentPart} by using {@link #ANALYSED_TEXT_URI}.
 * @see ContentItem#addPart(UriRef, Object)
 */
public interface AnalysedText extends Section{

    
    /**
     * The {@link UriRef} used to register the {@link AnalysedText} instance
     * as {@link ContentItem#addPart(org.apache.clerezza.rdf.core.UriRef, Object) 
     * ContentPart} to the {@link ContentItem}
     */
    public static final UriRef ANALYSED_TEXT_URI = new UriRef("urn:stanbol.enhancer:nlp.analysedText");

    /**
     * Returns {@link SpanTypeEnum#Text}
     * @see Span#getType()
     * @see SpanTypeEnum#Text
     */
    SpanTypeEnum getType();

    /**
     * Adds an Sentence
     * @param start the start index
     * @param end the end index
     * @return the Sentence
     */
    Sentence addSentence(int start, int end);

    /**
     * Adds an Chunk
     * @param start the start of the chunk
     * @param end
     * @return
     */
    Chunk addChunk(int start, int end);

    /**
     * All sentences of the Analysed texts.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return
     */
    Iterator<Sentence> getSentences();

    /**
     * All Chunks of this analysed text.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the chunks
     */
    Iterator<Chunk> getChunks();

    /**
     * Getter for the text.
     * @return 
     */
    CharSequence getText();
    
    /**
     * The analysed {@link Blob}. Typically {@link Blob#getMimeType()} will be
     * <code>text/plain</code>.
     * @return the analysed {@link Blob} instance.
     */
    Blob getBlob();
    
    
}