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
package org.apache.stanbol.entityhub.model.sesame;



import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.UnsupportedTypeException;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfValueFactory implements ValueFactory{
    
    protected final Logger log = LoggerFactory.getLogger(RdfValueFactory.class);
    
    static final Map<Object, Set<URI>> JAVA_TO_XML_DATATYPE_MAPPINGS;
    static {
        Map<Object, Set<URI>> mappings = new HashMap<Object,Set<URI>>();
        
        mappings.put(Boolean.class, Collections.singleton(XMLSchema.BOOLEAN));
        
        mappings.put(Byte.class, Collections.singleton(XMLSchema.BYTE));
        mappings.put(Short.class, new HashSet<URI>(Arrays.asList(
            XMLSchema.SHORT, XMLSchema.UNSIGNED_BYTE)));
        mappings.put(Integer.class, new HashSet<URI>(Arrays.asList(
            XMLSchema.INT, XMLSchema.UNSIGNED_SHORT)));
        mappings.put(Long.class, new HashSet<URI>(Arrays.asList(
            XMLSchema.LONG, XMLSchema.UNSIGNED_INT, 
            XMLSchema.NEGATIVE_INTEGER, XMLSchema.POSITIVE_INTEGER,
            XMLSchema.NON_NEGATIVE_INTEGER, XMLSchema.NON_POSITIVE_INTEGER)));
        mappings.put(BigDecimal.class, new HashSet<URI>(Arrays.asList(
            XMLSchema.INTEGER, XMLSchema.UNSIGNED_LONG)));

        mappings.put(Float.class, Collections.singleton(XMLSchema.FLOAT));
        mappings.put(Double.class, Collections.singleton(XMLSchema.DOUBLE));
        mappings.put(BigDecimal.class, Collections.singleton(XMLSchema.DECIMAL));
        
        mappings.put(Date.class, new HashSet<URI>(Arrays.asList(
            XMLSchema.DATE, XMLSchema.DATETIME, XMLSchema.TIME, 
            XMLSchema.GYEARMONTH, XMLSchema.GMONTHDAY, XMLSchema.GYEAR,
            XMLSchema.GMONTH, XMLSchema.GDAY)));
        
        mappings.put(String.class, Collections.singleton(XMLSchema.STRING));
        
        JAVA_TO_XML_DATATYPE_MAPPINGS = Collections.unmodifiableMap(mappings);
    }
    /**
     * Lazy initialised instance returned by {@link #getInstance()}
     */
    private static RdfValueFactory singleton;
    
    protected final org.openrdf.model.ValueFactory sesameFactory;
    /**
     * If not <code>null</code> this is used for all 
     * {@link #createRepresentation(String)} calls to this instance.
     */
    private Model model;
    
    /**
     * Create a RdfValueFactory that does use the same {@link Model} for all
     * created {@link Representation}s.
     * @param model the model to use
     * @param sesameFactory the Sesame ValueFactory or <code>null</code> to use
     * the default
     */
    public RdfValueFactory(Model model, org.openrdf.model.ValueFactory sesameFactory){
        this.model = model;
        this.sesameFactory = sesameFactory == null ? 
                ValueFactoryImpl.getInstance() : sesameFactory;;
    }
    /**
     * Creates a {@link RdfValueFactory} for a given Sesame ValueFactory.<p>
     * Instead of parsing <code>null</code> users should use the
     * {@link #getInstance()} method.
     * @param sesameFactory the Sesame ValueFactory or <code>null</code> to use
     * the default
     */
    private RdfValueFactory(org.openrdf.model.ValueFactory sesameFactory){
        this(null,sesameFactory);
    }
    /**
     * Internally used by the {@link #getInstance()}
     * @param sesameFactory the Sesame ValueFactory or <code>null</code> to use
     * the default
     */
    private RdfValueFactory(){
        this(null);
    }
    /**
     * The default instance of this Factory.
     * @return the instance
     */
    public static RdfValueFactory getInstance(){
        if(singleton == null){
            singleton = new RdfValueFactory();
        }
        return singleton;
    }
    
            
    @Override
    public Reference createReference(Object value) throws UnsupportedTypeException, IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("The parsed value MUST NOT be NULL");
        } else if (value instanceof URI) {
            return new RdfReference((URI)value);
        } else {
            return new RdfReference(sesameFactory.createURI(value.toString()));
        }
    }

    @Override
    public Representation createRepresentation(String id) throws IllegalArgumentException {
        if (id == null){
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL!");
         } else if(id.isEmpty()){
             throw new IllegalArgumentException("The parsed id MUST NOT be empty!");
         } else {
             //use the set model if present
             return createRdfRepresentation(sesameFactory.createURI(id));
        }
    }
    /**
     * Creates a {@link RdfRepresentation} for the parsed {@link URI}
     * @param subject the URI
     * @return the {@link RdfRepresentation}
     */
    public RdfRepresentation createRdfRepresentation(URI subject) {
        Model model = this.model == null ? new TreeModel() : this.model;
        return new RdfRepresentation(subject, model, this);
    }

    @Override
    public Text createText(Object value) throws UnsupportedTypeException, IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("The parsed value MUST NOT be NULL");
        } else if (value instanceof Literal) {
            return new RdfText((Literal) value);
        } else {
            return createText(value.toString(), null);
        }
    }

    @Override
    public Text createText(String text, String language) throws IllegalArgumentException {
        if(text == null){
            throw new IllegalArgumentException("The parsed text MUST NOT be NULL");
        } else if(text.isEmpty()){
            throw new IllegalArgumentException("Tha parsed Text MUST NOT be empty!");
        }
        if(language != null && language.isEmpty()){
            language = null;
        }
        return new RdfText(sesameFactory.createLiteral(text, language));
    }
    
    /**
     * Getter for the Sesame {@link org.openrdf.model.ValueFactory} used by
     * this Entityhub {@link ValueFactory}.
     * @return the Sesame value factory
     */
    public org.openrdf.model.ValueFactory getSesameFactory(){
        return sesameFactory;
    }
    /**
     * Converts any {@link Representation} implementation to a {@link RdfRepresentation}
     * backed by a Sesame {@link Model}.
     * @param representation the representation
     * @return the {@link RdfRepresentation}
     */
    public RdfRepresentation toRdfRepresentation(Representation representation) {
        if(representation instanceof RdfRepresentation){
            return (RdfRepresentation) representation;
        } else if(representation != null){
            //create the Clerezza Represenation
            RdfRepresentation rdfRep = createRdfRepresentation(
                sesameFactory.createURI(representation.getId()));
            //Copy all values field by field
            for (Iterator<String> fields = representation.getFieldNames(); fields.hasNext();) {
                String field = fields.next();
                for (Iterator<Object> fieldValues = representation.get(field); fieldValues.hasNext();) {
                    rdfRep.add(field, fieldValues.next());
                }
            }
            return rdfRep;
        } else {
            return null;
        }
    }
}
