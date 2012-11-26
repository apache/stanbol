package org.apache.stanbol.enhancer.nlp.model;

import java.io.IOException;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.nlp.model.impl.AnalysedTextFactoryImpl;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

public abstract class AnalysedTextFactory {

    private static AnalysedTextFactory defaultInstance = new AnalysedTextFactoryImpl();
    
    /**
     * Creates an {@link AnalysedText} instance for the parsed {@link Blob}
     * and registers itself as 
     * {@link ContentItem#addPart(org.apache.clerezza.rdf.core.UriRef, Object) 
     * ContentPart} with the {@link UriRef} {@link AnalysedText#ANALYSED_TEXT_URI}
     * to the parsed {@link ContentItem}.<p>
     * If already a ContentPart with the given UriRef is registered this 
     * Method will throw an {@link IllegalStateException}.
     * @param ci the ContentItem to register the created {@link AnalysedText} instance
     * @param blob the analysed {@link Blob}
     * @return the created {@link AnalysedText}
     * @throws IllegalArgumentException of <code>null</code> is parsed as
     * ContentItem or Blob
     * @throws IllegalStateException if there is already an ContentPart is
     * registered for {@link AnalysedText#ANALYSED_TEXT_URI} with the parsed
     * ContentItem.
     * @throws IOException on any error while reading data from the parsed blob
     */
    public abstract AnalysedText createAnalysedText(ContentItem ci, Blob blob) throws IOException ;
    /**
     * Creates a AnalysedText instance for the parsed blob.<p>
     * NOTE: This implementation does NOT register the {@link AnalysedText}
     * as ContentPart. 
     * @param blob the analysed Blob
     * @return the AnalysedText
     * @throws IllegalArgumentException if <code>null</code> is parsed as 
     * {@link Blob}.
     * @throws IOException on any error while reading data from the parsed blob
     */
    public abstract AnalysedText createAnalysedText(Blob blob) throws IOException ;
    
    /**
     * Intended to be used outside of an OSGI container to obtain an
     * instance of a {@link AnalysedTextFactory}. <p>
     * When using this within an OSGI environment it is preferred to obtain
     * the factory as a service (e.g. via the BundleContext, an ServiceTracker
     * or by injection). As this allows the usage of different implementations.
     * <p>
     * This is hard-wired with the default implementation contained within this
     * module.
     * @return the default {@link AnalysedTextFactory} instance.
     */
    public static final AnalysedTextFactory getDefaultInstance(){
        return defaultInstance;
    }
}
