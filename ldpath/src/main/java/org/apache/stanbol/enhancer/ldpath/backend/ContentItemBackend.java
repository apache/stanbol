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
package org.apache.stanbol.enhancer.ldpath.backend;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getContentParts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.utils.UnionMGraph;
import org.apache.stanbol.commons.ldpath.clerezza.ClerezzaBackend;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.api.backend.RDFBackend;

/**
 * Basically a {@link ClerezzaBackend} over {@link ContentItem#getMetadata()}
 * that ensures read locks to be used for queries on subjects and objects.
 * @author Rupert Westenthaler
 *
 */
public class ContentItemBackend implements RDFBackend<Resource>{

    private final Logger log = LoggerFactory.getLogger(ContentItemBackend.class);
    
    private static final Map<UriRef,TripleCollection> EMPTY_INCLUDED = emptyMap();
    
    private final ContentItem ci;
    private final Lock readLock;
    private final ClerezzaBackend backend;
    private final Map<UriRef,TripleCollection> included;
    
    /**
     * Creates a {@link RDFBackend} over the {@link ContentItem#getMetadata()
     * metadata} of the parsed content item.
     * @param ci the content item
     */
    public ContentItemBackend(ContentItem ci) {
        this(ci,false);
    }
    /**
     * Creates a {@link RDFBackend} over the {@link ContentItem#getMetadata()
     * metadata} and all {@link ContentItem#getPart(int, Class) content parts}
     * compatible to {@link TripleCollection} 
     * @param ci the content item
     * @param includeAdditionalMetadata if <code>true</code> the {@link RDFBackend}
     * will also include RDF data stored in content parts
     */
    public ContentItemBackend(ContentItem ci, boolean includeAdditionalMetadata){
        included = includeAdditionalMetadata ?
                unmodifiableMap(getContentParts(ci, TripleCollection.class)) :
                    EMPTY_INCLUDED;
        MGraph graph;
        if(!included.isEmpty()){
            graph = ci.getMetadata();
        } else {
            TripleCollection[] tcs = new TripleCollection[included.size()+1];
            tcs[0] = ci.getMetadata();
            System.arraycopy(tcs, 1, included.values().toArray(), 0, included.size());
            graph = new UnionMGraph(tcs);
        }
        backend = new ClerezzaBackend(graph);
        this.ci = ci;
        this.readLock = ci.getLock().readLock();
    }
    /**
     * Creates a {@link RDFBackend} over the {@link ContentItem#getMetadata()
     * metadata} and RDF data stored in content parts with the parsed URIs.
     * If no content part for a parsed URI exists or its type is not compatible
     * to {@link TripleCollection} it will be not included.
     * @param ci the content item
     * @param includedMetadata the URIs for the content parts to include
     */
    public ContentItemBackend(ContentItem ci, Set<UriRef> includedMetadata){
        Map<UriRef,TripleCollection> included = new LinkedHashMap<UriRef,TripleCollection>();
        for(UriRef ref : includedMetadata){
            try {
                TripleCollection metadata = ci.getPart(ref, TripleCollection.class);
                included.put(ref, metadata);
            } catch (RuntimeException e) {
                log.warn("Unable to add requested Metadata-ContentPart "+ref+" to" +
                		"ContentItemBackend "+ci.getUri(),e);
            }
        }
        this.included = unmodifiableMap(included);
        MGraph graph;
        if(!included.isEmpty()){
            graph = ci.getMetadata();
        } else {
            TripleCollection[] tcs = new TripleCollection[included.size()+1];
            tcs[0] = ci.getMetadata();
            System.arraycopy(tcs, 1, included.values().toArray(), 0, included.size());
            graph = new UnionMGraph(tcs);
        }
        backend = new ClerezzaBackend(graph);
        this.ci = ci;
        this.readLock = ci.getLock().readLock();
    }
    

    @Override
    public Collection<Resource> listObjects(Resource subject, Resource property) {
        readLock.lock();
        try {
            return backend.listObjects(subject, property);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<Resource> listSubjects(Resource property, Resource object) {
        readLock.lock();
        try {
            return backend.listSubjects(property, object);
        } finally {
            readLock.unlock();
        }
    }
    /**
     * Getter for the content item
     * @return the content item
     */
    public ContentItem getContentItem(){
        return ci;
    }
    /**
     * Getter for the read-only map of the content parts included in this
     * RDF backend
     * @return the content parts included in this {@link RDFBackend}
     */
    public Map<UriRef,TripleCollection> getIncludedMetadata(){
        return included;
    }
    
    @Override
    public boolean isLiteral(Resource n) {
        return backend.isLiteral(n);
    }
    @Override
    public boolean isURI(Resource n) {
        return backend.isURI(n);
    }
    @Override
    public boolean isBlank(Resource n) {
        return backend.isBlank(n);
    }
    @Override
    public Locale getLiteralLanguage(Resource n) {
        return backend.getLiteralLanguage(n);
    }
    @Override
    public URI getLiteralType(Resource n) {
        return backend.getLiteralType(n);
    }
    @Override
    public Resource createLiteral(String content) {
        return backend.createLiteral(content);
    }
    @Override
    public Resource createLiteral(String content, Locale language, URI type) {
        return backend.createLiteral(content, language, type);
    }
    @Override
    public Resource createURI(String uri) {
        return backend.createURI(uri);
    }
    @Override
    public String stringValue(Resource node) {
        return backend.stringValue(node);
    }
    @Override
    public Double doubleValue(Resource node) {
        return backend.doubleValue(node);
    }
    @Override
    public Long longValue(Resource node) {
        return backend.longValue(node);
    }
    @Override
    public Boolean booleanValue(Resource node) {
        return backend.booleanValue(node);
    }
    @Override
    public Date dateTimeValue(Resource node) {
        return backend.dateTimeValue(node);
    }
    @Override
    public Date dateValue(Resource node) {
        return backend.dateValue(node);
    }
    @Override
    public Date timeValue(Resource node) {
        return backend.timeValue(node);
    }
    @Override
    public Float floatValue(Resource node) {
        return backend.floatValue(node);
    }
    @Override
    public Integer intValue(Resource node) {
        return backend.intValue(node);
    }
    @Override
    public BigInteger integerValue(Resource node) {
        return backend.integerValue(node);
    }
    @Override
    public BigDecimal decimalValue(Resource node) {
        return backend.decimalValue(node);
    }
    
    /* NO SUPPORT FOR THREADING REQUIRED */
    @Override
    public boolean supportsThreading() {
        return false;
    }
    @Override
    public ThreadPoolExecutor getThreadPool() {
        return null;
    }

}
