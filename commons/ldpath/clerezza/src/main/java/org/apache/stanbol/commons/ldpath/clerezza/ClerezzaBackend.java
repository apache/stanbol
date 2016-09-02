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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TypedLiteralImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.model.backend.AbstractBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clerezza based implementation of {@link RDFBackend} interface. This implementation uses the
 * {@link RDFTerm} objects of Clerezza as processing unit RDFBackend.<p>
 * 
 * For type conversions of {@link Literal}s the {@link LiteralFactory}
 * of Clerezza is used. In case parsed nodes are not {@link Literal} the
 * super implementations of {@link AbstractBackend} are called as such also
 * support converting values based on the string representation.
 * 
 * @author anil.sinaci
 * @author Rupert Westenthaler
 */
public class ClerezzaBackend extends AbstractBackend<RDFTerm> implements RDFBackend<RDFTerm> {

    private static final Logger logger = LoggerFactory.getLogger(ClerezzaBackend.class);

    private static final Collection<IRI> STRING_DATATYPES = Collections.unmodifiableCollection(new HashSet<IRI>(Arrays.asList(
            new IRI("http://www.w3.org/2001/XMLSchema#string"),
            new IRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString"),
            null)));

    /**
     * Enumeration containing supported XSD dataTypes including <ul>
     * <li> local name
     * <li> uri string
     * <li> {@link URI}
     * <li> {@link IRI}
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
        IRI uriRef;
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
            this.uriRef = new IRI(uriString);
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
        public IRI getIRI(){
            return uriRef;
        }
        @Override
        public String toString() {
            return uriString;
        }
        private static BidiMap xsdURI2IRI = new DualHashBidiMap();
        
        static {
            for(XSD type : XSD.values()){
                xsdURI2IRI.put(type.getURI(), type.getIRI());
            }
        }
        public static URI getXsdURI(IRI uri){
            return (URI)xsdURI2IRI.getKey(uri);
        }
        public static IRI getXsdIRI(URI uri){
            return (IRI)xsdURI2IRI.get(uri);
        }
    }
    
    private Graph graph;
    
    private static LiteralFactory lf = LiteralFactory.getInstance();

    /**
     * Allows sub-classes to create a instance and setting the {@link #graph}
     * later on by using {@link #setGraph(Graph)}.
     */
    protected ClerezzaBackend() {
    }
    /**
     * Constructs a Clerezza {@link RDFBackend} by using the parsed {@link Graph}
     * @param graph the {@link Graph}
     * @throws IllegalArgumentException if <code>null</code> is parsed as graph.
     */
    public ClerezzaBackend(Graph graph) {
        if(graph == null){
            throw new IllegalArgumentException("The parsed ImmutableGraph MUST NOT be NULL!");
        }
        this.graph = graph;
    }
    
    protected final Graph getGraph(){
        return this.graph;
    }

    protected final void setGraph(Graph graph){
        this.graph = graph;
    }
    
    @Override
    public RDFTerm createLiteral(String content) {
        return createLiteral(content,null,null);
    }

    @Override
    public RDFTerm createLiteral(String content, Locale language, URI type) {
        logger.debug("creating literal with content \"{}\", language {}, datatype {}",
            new Object[] {content, language, type});
        if (type == null) {
            if(language == null){
                return new PlainLiteralImpl(content);
            } else {
                return new PlainLiteralImpl(content, new Language(language.getLanguage()));
            }
        } else {
            return new TypedLiteralImpl(content, XSD.getXsdIRI(type));
        }
    }

    @Override
    public RDFTerm createURI(String uriref) {
        return new IRI(uriref);
    }

    @Override
    public Double doubleValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return LiteralFactory.getInstance().createObject(Double.class, (Literal) resource);
        } else {
            return super.doubleValue(resource);
        }
    }

    @Override
    public Locale getLiteralLanguage(RDFTerm resource) {
        if (resource instanceof Literal) {
            Language lang = ((Literal) resource).getLanguage();
            return lang != null ? new Locale(lang.toString()) : null;
        } else {
            throw new IllegalArgumentException("RDFTerm " + resource.toString() + " is not a PlainLiteral");
        }
    }

    @Override
    public URI getLiteralType(RDFTerm resource) {
        if (resource instanceof Literal) {
            IRI type = ((Literal) resource).getDataType();
            return type != null ? XSD.getXsdURI(type) : null;
        } else {
            throw new IllegalArgumentException("Value " + resource.toString() + " is not a literal");
        }
    }

    @Override
    public boolean isBlank(RDFTerm resource) {
        return resource instanceof BlankNode;
    }

    @Override
    public boolean isLiteral(RDFTerm resource) {
        return resource instanceof Literal;
    }

    @Override
    public boolean isURI(RDFTerm resource) {
        return resource instanceof IRI;
    }

    @Override
    public Collection<RDFTerm> listObjects(RDFTerm subject, RDFTerm property) {
        if (!(property instanceof IRI) || 
                !(subject instanceof BlankNodeOrIRI)) {
            throw new IllegalArgumentException("Subject needs to be a URI or blank node, property a URI node");
        }

        Collection<RDFTerm> result = new ArrayList<RDFTerm>();
        Lock readLock = readLockGraph();
        try {
            Iterator<Triple> triples = graph.filter((BlankNodeOrIRI) subject, (IRI) property, null);
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
    public Collection<RDFTerm> listSubjects(RDFTerm property, RDFTerm object) {
        if (!(property instanceof IRI)) {
            throw new IllegalArgumentException("Property needs to be a URI node");
        }

        Collection<RDFTerm> result = new ArrayList<RDFTerm>();
        Lock readLock = readLockGraph();
        try {
            Iterator<Triple> triples = graph.filter(null, (IRI) property, object);
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
    public Long longValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return lf.createObject(Long.class, (Literal) resource);
        } else {
            return super.longValue(resource);
        }
    }

    @Override
    public String stringValue(RDFTerm resource) {
        if (resource instanceof IRI) {
            return ((IRI) resource).getUnicodeString();
        } else if (resource instanceof Literal) {
            return ((Literal) resource).getLexicalForm();
        } else { //BlankNode
            return resource.toString();
        }
    }

    @Override
    public Boolean booleanValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return lf.createObject(Boolean.class, (Literal) resource);
        } else {
            return super.booleanValue(resource);
        }
    }

    @Override
    public Date dateTimeValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return lf.createObject(Date.class, (Literal) resource);
        } else {
            return super.dateTimeValue(resource);
        }
    }

    @Override
    public Date dateValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return lf.createObject(Date.class, (Literal) resource);
        } else {
            return super.dateValue(resource);
        }
    }

    @Override
    public Date timeValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return lf.createObject(Date.class, (Literal) resource);
        } else {
            return super.timeValue(resource);
        }
    }

    @Override
    public Float floatValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return lf.createObject(Float.class, (Literal) resource);
        } else {
            return super.floatValue(resource);
        }
    }

    @Override
    public Integer intValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return lf.createObject(Integer.class, (Literal) resource);
        } else {
            return super.intValue(resource);
        }
    }

    @Override
    public BigInteger integerValue(RDFTerm resource) {
        if (isDataTypeLiteral(resource)) {
            return lf.createObject(BigInteger.class, (Literal) resource);
        } else {
            return super.integerValue(resource);
        }
    }

    private boolean isDataTypeLiteral(RDFTerm resource){
        return resource instanceof Literal &&
                !STRING_DATATYPES.contains(((Literal)resource).getDataType());
    }
    
    @Override
    public BigDecimal decimalValue(RDFTerm resource) {
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
        readLock = graph.getLock().readLock();
        readLock.lock();
        return readLock;
    }

    
}