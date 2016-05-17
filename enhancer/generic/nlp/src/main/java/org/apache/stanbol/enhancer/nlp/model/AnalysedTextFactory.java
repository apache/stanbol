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
package org.apache.stanbol.enhancer.nlp.model;

import java.io.IOException;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.nlp.model.impl.AnalysedTextFactoryImpl;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;

/**
 * Abstract implementation of the {@link AnalysedTextFactory} that
 * provides the implementation of the {@link #createAnalysedText(ContentItem, Blob)}
 * based on the {@link #createAnalysedText(Blob)} method.
 * <p>
 * The {@link #getDefaultInstance()} methods returns the in-memory implementation
 * of the AnalyzedText domain model and should only be used outside of an
 * OSGI Service as implementation are also registered as OSGI services.
 */
public abstract class AnalysedTextFactory {

    private static AnalysedTextFactory defaultInstance = new AnalysedTextFactoryImpl();
    
    /**
     * Creates an {@link AnalysedText} instance for the parsed {@link Blob}
     * and registers itself as 
     * {@link ContentItem#addPart(org.apache.clerezza.commons.rdf.IRI, Object) 
     * ContentPart} with the {@link IRI} {@link AnalysedText#ANALYSED_TEXT_URI}
     * to the parsed {@link ContentItem}.<p>
     * If already a ContentPart with the given IRI is registered this 
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
    public final AnalysedText createAnalysedText(ContentItem ci, Blob blob) throws IOException {
        ci.getLock().readLock().lock();
        try {
            AnalysedText existing = ci.getPart(AnalysedText.ANALYSED_TEXT_URI, AnalysedText.class);
            throw new IllegalStateException("The AnalysedText ContentPart already exists (impl: "
                +existing.getClass().getSimpleName()+"| blob: "+existing.getBlob().getMimeType()+")");
        }catch (NoSuchPartException e) {
            //this is the expected case
        }catch (ClassCastException e) {
            throw new IllegalStateException("A ContentPart with the URI '"
                + AnalysedText.ANALYSED_TEXT_URI+"' already exists but the parts "
                + "type is not compatible with "+AnalysedText.class.getSimpleName()+"!",
                e);
        } finally {
            ci.getLock().readLock().unlock();
        }
        //create the Analysed text
        AnalysedText at = createAnalysedText(blob);
        ci.getLock().writeLock().lock();
        try {
            //NOTE: there is a possibility that an other thread has added
            // the contentpart
            ci.addPart(AnalysedText.ANALYSED_TEXT_URI, at);
        } finally {
            ci.getLock().writeLock().unlock();
        }
        return at;
    }
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
