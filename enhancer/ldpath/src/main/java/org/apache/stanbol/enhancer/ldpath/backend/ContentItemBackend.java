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

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.utils.UnionGraph;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.stanbol.commons.ldpath.clerezza.ClerezzaBackend;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basically a {@link ClerezzaBackend} over {@link ContentItem#getMetadata()}
 * that ensures read locks to be used for queries on subjects and objects.
 * @author Rupert Westenthaler
 *
 */
public class ContentItemBackend implements RDFBackend<RDFTerm>{

    private final Logger log = LoggerFactory.getLogger(ContentItemBackend.class);
    
    private static final Map<IRI,Graph> EMPTY_INCLUDED = emptyMap();
    
    private final ContentItem ci;
    private final Lock readLock;
    private final ClerezzaBackend backend;
    private final Map<IRI,Graph> included;
    
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
     * compatible to {@link Graph} 
     * @param ci the content item
     * @param includeAdditionalMetadata if <code>true</code> the {@link RDFBackend}
     * will also include RDF data stored in content parts
     */
    public ContentItemBackend(ContentItem ci, boolean includeAdditionalMetadata){
        included = includeAdditionalMetadata ?
                unmodifiableMap(getContentParts(ci, Graph.class)) :
                    EMPTY_INCLUDED;
        Graph graph;
        if(included.isEmpty()){
            graph = ci.getMetadata();
        } else {
            Graph[] tcs = new Graph[included.size()+1];
            tcs[0] = ci.getMetadata();
            System.arraycopy(included.values().toArray(), 0, tcs, 1, included.size());
            graph = new UnionGraph(tcs);
        }
        backend = new ClerezzaBackend(graph);
        this.ci = ci;
        this.readLock = ci.getLock().readLock();
    }
    /**
     * Creates a {@link RDFBackend} over the {@link ContentItem#getMetadata()
     * metadata} and RDF data stored in content parts with the parsed URIs.
     * If no content part for a parsed URI exists or its type is not compatible
     * to {@link Graph} it will be not included.
     * @param ci the content item
     * @param includedMetadata the URIs for the content parts to include
     */
    public ContentItemBackend(ContentItem ci, Set<IRI> includedMetadata){
        Map<IRI,Graph> included = new LinkedHashMap<IRI,Graph>();
        for(IRI ref : includedMetadata){
            try {
                Graph metadata = ci.getPart(ref, Graph.class);
                included.put(ref, metadata);
            } catch (RuntimeException e) {
                log.warn("Unable to add requested Metadata-ContentPart "+ref+" to" +
                		"ContentItemBackend "+ci.getUri(),e);
            }
        }
        this.included = unmodifiableMap(included);
        Graph graph;
        if(!included.isEmpty()){
            graph = ci.getMetadata();
        } else {
            Graph[] tcs = new Graph[included.size()+1];
            tcs[0] = ci.getMetadata();
            System.arraycopy(tcs, 1, included.values().toArray(), 0, included.size());
            graph = new UnionGraph(tcs);
        }
        backend = new ClerezzaBackend(graph);
        this.ci = ci;
        this.readLock = ci.getLock().readLock();
    }
    

    @Override
    public Collection<RDFTerm> listObjects(RDFTerm subject, RDFTerm property) {
        readLock.lock();
        try {
            return backend.listObjects(subject, property);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<RDFTerm> listSubjects(RDFTerm property, RDFTerm object) {
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
    public Map<IRI,Graph> getIncludedMetadata(){
        return included;
    }
    
    @Override
    public boolean isLiteral(RDFTerm n) {
        return backend.isLiteral(n);
    }
    @Override
    public boolean isURI(RDFTerm n) {
        return backend.isURI(n);
    }
    @Override
    public boolean isBlank(RDFTerm n) {
        return backend.isBlank(n);
    }
    @Override
    public Locale getLiteralLanguage(RDFTerm n) {
        return backend.getLiteralLanguage(n);
    }
    @Override
    public URI getLiteralType(RDFTerm n) {
        return backend.getLiteralType(n);
    }
    @Override
    public RDFTerm createLiteral(String content) {
        return backend.createLiteral(content);
    }
    @Override
    public RDFTerm createLiteral(String content, Locale language, URI type) {
        return backend.createLiteral(content, language, type);
    }
    @Override
    public RDFTerm createURI(String uri) {
        return backend.createURI(uri);
    }
    @Override
    public String stringValue(RDFTerm node) {
        return backend.stringValue(node);
    }
    @Override
    public Double doubleValue(RDFTerm node) {
        return backend.doubleValue(node);
    }
    @Override
    public Long longValue(RDFTerm node) {
        return backend.longValue(node);
    }
    @Override
    public Boolean booleanValue(RDFTerm node) {
        return backend.booleanValue(node);
    }
    @Override
    public Date dateTimeValue(RDFTerm node) {
        return backend.dateTimeValue(node);
    }
    @Override
    public Date dateValue(RDFTerm node) {
        return backend.dateValue(node);
    }
    @Override
    public Date timeValue(RDFTerm node) {
        return backend.timeValue(node);
    }
    @Override
    public Float floatValue(RDFTerm node) {
        return backend.floatValue(node);
    }
    @Override
    public Integer intValue(RDFTerm node) {
        return backend.intValue(node);
    }
    @Override
    public BigInteger integerValue(RDFTerm node) {
        return backend.integerValue(node);
    }
    @Override
    public BigDecimal decimalValue(RDFTerm node) {
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
