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
package org.apache.stanbol.commons.ldpath.clerezza;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.model.backend.AbstractBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clerezza based implementation of {@link RDFBackend} interface. This implementation uses the
 * {@link Resource} objects of Clerezza as processing unit RDFBackend.<p>
 * 
 * For type conversions of {@link TypedLiteral}s the {@link LiteralFactory}
 * of Clerezza is used. In case parsed nodes are not {@link TypedLiteral} the
 * super implementations of {@link AbstractBackend} are called as such also
 * support converting values based on the string representation.
 * 
 * @author anil.sinaci
 * @author Rupert Westenthaler
 */
public class ClerezzaBackend extends AbstractBackend<Resource> implements RDFBackend<Resource> {

    private static final Logger logger = LoggerFactory.getLogger(ClerezzaBackend.class);

    /**
     * Enumeration containing supported XSD dataTypes including <ul>
     * <li> local name
     * <li> uri string
     * <li> {@link URI}
     * <li> {@link UriRef}
     * </ul>
     * {@link #toString()} returns the uri.
     */
    public static enum XSD {
        INTEGER,INT,SHORT,BYTE,LONG,DOUBLE,FLOAT,
        ANY_URI("anyURI"),DATE_TIME("dateTime"),BOOLEAN,STRING;
        static final String namespace = "http://www.w3.org/2001/XMLSchema#";
        String localName;
        String uriString;
        URI uri;
        UriRef uriRef;
        /**
         * uses <code>{@link #name()}{@link String#toLowerCase() .toLoverCase()}
         * </code> to generate the {@link #getLocalName()}
         */
        private XSD() {
            this(null);
        }
        /**
         * Constructor that allows to parse the local name. if <code>null</code>
         * it uses <code>{@link #name()}{@link String#toLowerCase() .toLoverCase()}
         * </code> to generate the {@link #getLocalName() localName}
         * @param localName the local name or <code>null</code> to use 
         * <code>{@link #name()}{@link String#toLowerCase() .toLoverCase()}
         * </code>
         */
        private XSD(String localName){
            this.localName = localName != null ? localName : name().toLowerCase();
            this.uriString = namespace+this.localName;
            this.uri = URI.create(uriString);
            this.uriRef = new UriRef(uriString);
        }
        public String getLocalName(){
            return localName;
        }
        public String getUri(){
            return uriString; 
        }
        public URI getURI(){
            return uri;
        }
        public UriRef getUriRef(){
            return uriRef;
        }
        @Override
        public String toString() {
            return uriString;
        }
        private static BidiMap xsdURI2UriRef = new DualHashBidiMap();
        
        static {
            for(XSD type : XSD.values()){
                xsdURI2UriRef.put(type.getURI(), type.getUriRef());
            }
        }
        public static URI getXsdURI(UriRef uri){
            return (URI)xsdURI2UriRef.getKey(uri);
        }
        public static UriRef getXsdUriRef(URI uri){
            return (UriRef)xsdURI2UriRef.get(uri);
        }
    }
    
    private TripleCollection graph;
    
    private static LiteralFactory lf = LiteralFactory.getInstance();

    /**
     * Allows sub-classes to create a instance and setting the {@link #graph}
     * later on by using {@link #setGraph(TripleCollection)}.
     */
    protected ClerezzaBackend() {
    }
    /**
     * Constructs a Clerezza {@link RDFBackend} by using the parsed {@link TripleCollection}
     * @param graph the {@link TripleCollection}
     * @throws IllegalArgumentException if <code>null</code> is parsed as graph.
     */
    public ClerezzaBackend(TripleCollection graph) {
        if(graph == null){
            throw new IllegalArgumentException("The parsed Graph MUST NOT be NULL!");
        }
        this.graph = graph;
    }
    
    protected final TripleCollection getGraph(){
        return this.graph;
    }

    protected final void setGraph(TripleCollection graph){
        this.graph = graph;
    }
    
    @Override
    public Resource createLiteral(String content) {
        return createLiteral(content,null,null);
    }

    @Override
    public Resource createLiteral(String content, Locale language, URI type) {
        logger.debug("creating literal with content \"{}\", language {}, datatype {}",
            new Object[] {content, language, type});
        if (type == null) {
            if(language == null){
                return new PlainLiteralImpl(content);
            } else {
                return new PlainLiteralImpl(content, new Language(language.getLanguage()));
            }
        } else {
            return new TypedLiteralImpl(content, XSD.getXsdUriRef(type));
        }
    }

    @Override
    public Resource createURI(String uriref) {
        return new UriRef(uriref);
    }

    @Override
    public Double doubleValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Double.class, (TypedLiteral) resource);
        } else {
            return super.doubleValue(resource);
        }
    }

    @Override
    public Locale getLiteralLanguage(Resource resource) {
        if (resource instanceof PlainLiteral) {
            Language lang = ((PlainLiteral) resource).getLanguage();
            return lang != null ? new Locale(lang.toString()) : null;
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a PlainLiteral");
        }
    }

    @Override
    public URI getLiteralType(Resource resource) {
        if (resource instanceof TypedLiteral) {
            UriRef type = ((TypedLiteral) resource).getDataType();
            return type != null ? XSD.getXsdURI(type) : null;
        } else {
            throw new IllegalArgumentException("Value " + resource.toString() + " is not a literal");
        }
    }

    @Override
    public boolean isBlank(Resource resource) {
        return resource instanceof BNode;
    }

    @Override
    public boolean isLiteral(Resource resource) {
        return resource instanceof Literal;
    }

    @Override
    public boolean isURI(Resource resource) {
        return resource instanceof UriRef;
    }

    @Override
    public Collection<Resource> listObjects(Resource subject, Resource property) {
        if (!(property instanceof UriRef) || 
                !(subject instanceof NonLiteral)) {
            throw new IllegalArgumentException("Subject needs to be a URI or blank node, property a URI node");
        }

        Collection<Resource> result = new ArrayList<Resource>();
        Lock readLock = readLockGraph();
        try {
            Iterator<Triple> triples = graph.filter((NonLiteral) subject, (UriRef) property, null);
            while (triples.hasNext()) {
                result.add(triples.next().getObject());
            }
        } finally {
            if(readLock != null){ //will be null if #graph is a read-only graph instance
                readLock.unlock();
            }
        }

        return result;
    }

    @Override
    public Collection<Resource> listSubjects(Resource property, Resource object) {
        if (!(property instanceof UriRef)) {
            throw new IllegalArgumentException("Property needs to be a URI node");
        }

        Collection<Resource> result = new ArrayList<Resource>();
        Lock readLock = readLockGraph();
        try {
            Iterator<Triple> triples = graph.filter(null, (UriRef) property, object);
            while (triples.hasNext()) {
                result.add(triples.next().getSubject());
            }
        } finally {
            if(readLock != null){ //will be null if #graph is a read-only graph instance
                readLock.unlock();
            }
        }
        return result;
    }

    @Override
    public Long longValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return lf.createObject(Long.class, (TypedLiteral) resource);
        } else {
            return super.longValue(resource);
        }
    }

    @Override
    public String stringValue(Resource resource) {
        if (resource instanceof UriRef) {
            return ((UriRef) resource).getUnicodeString();
        } else if (resource instanceof Literal) {
            return ((Literal) resource).getLexicalForm();
        } else { //BNode
            return resource.toString();
        }
    }

    @Override
    public Boolean booleanValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return lf.createObject(Boolean.class, (TypedLiteral) resource);
        } else {
            return super.booleanValue(resource);
        }
    }

    @Override
    public Date dateTimeValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return lf.createObject(Date.class, (TypedLiteral) resource);
        } else {
            return super.dateTimeValue(resource);
        }
    }

    @Override
    public Date dateValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return lf.createObject(Date.class, (TypedLiteral) resource);
        } else {
            return super.dateValue(resource);
        }
    }

    @Override
    public Date timeValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return lf.createObject(Date.class, (TypedLiteral) resource);
        } else {
            return super.timeValue(resource);
        }
    }

    @Override
    public Float floatValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return lf.createObject(Float.class, (TypedLiteral) resource);
        } else {
            return super.floatValue(resource);
        }
    }

    @Override
    public Integer intValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return lf.createObject(Integer.class, (TypedLiteral) resource);
        } else {
            return super.intValue(resource);
        }
    }

    @Override
    public BigInteger integerValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return lf.createObject(BigInteger.class, (TypedLiteral) resource);
        } else {
            return super.integerValue(resource);
        }
    }

    @Override
    public BigDecimal decimalValue(Resource resource) {
        //currently there is no converter for BigDecimal in clerezza
        //so as a workaround use the lexical form (as provided by the super
        //implementation
        return super.decimalValue(resource);
    }
    
    @Override
    public boolean supportsThreading() {
        return false;
    }
    @Override
    public ThreadPoolExecutor getThreadPool() {
        return null;
    }
    
    /**
     * @return the readLock or <code>null</code>if no read lock is needed
     */
    private Lock readLockGraph() {
        final Lock readLock;
        if(graph instanceof LockableMGraph){
            readLock = ((LockableMGraph)graph).getLock().readLock();
            readLock.lock();
        } else {
            readLock = null;
        }
        return readLock;
    }

    
}