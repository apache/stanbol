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
package org.apache.stanbol.enhancer.engines.tika.metadata;

import static org.apache.tika.metadata.DublinCore.DATE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.rdf.core.InvalidLiteralTypeException;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.NoConvertorException;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TypedLiteralImpl;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used as value for Apache Tika {@link Metadata} mappings. Holds the
 * ontology property as {@link IRI} and optionally a Tika {@link Property}.
 * Later can be used to parse the correct datatype for values contained in the
 * {@link Metadata}
 * 
 * @author westei
 *
 */
public abstract class Mapping {
    
    private final static Logger log = LoggerFactory.getLogger(Mapping.class);
    private static final LiteralFactory lf = LiteralFactory.getInstance();

    /**
     * List with allowed DataTypes.<ul>
     * <li> <code>null</code> is used for {@link PlainLiteral}s
     * <li> {@link XSD} datatyoes are used for {@link TypedLiteral}s
     * <li> {@link RDFS#RDFTerm} is used for {@link BlankNodeOrIRI} values. Note
     * that only {@link IRI} is supported, because for Tika {@link BlankNode}s
     * do not make sense.
     * </ul>
     */
    public static final Set<IRI> ONT_TYPES;
    /**
     * Map with the same keys as contained in {@link #ONT_TYPES}. The values
     * are the java types.
     */
    protected static final Map<IRI,Class<?>> ONT_TYPE_MAP;
    
    static {
        //use a linked HasSetMap to have the nice ordering (mainly for logging)
        Map<IRI,Class<?>> map = new LinkedHashMap<IRI,Class<?>>();
        //Plain Literal values
        map.put(null,null);
        //Typed Literal values
        map.put(XSD.anyURI,URI.class);
        map.put(XSD.base64Binary, byte[].class);
        map.put(XSD.boolean_,Boolean.class);
        map.put(XSD.byte_,Byte.class);
        map.put(XSD.date,Date.class);
        map.put(XSD.dateTime,Date.class);
        map.put(XSD.decimal,BigDecimal.class);
        map.put(XSD.double_,Double.class);
        map.put(XSD.float_,Float.class);
        map.put(XSD.int_,Integer.class);
        map.put(XSD.integer,BigInteger.class);
        map.put(XSD.long_,Long.class);
        map.put(XSD.short_,Short.class);
        map.put(XSD.string,String.class);
        map.put(XSD.time,Date.class);
        //Data Types for BlankNodeOrIRI values
        map.put(RDFS.Resource,URI.class);
        ONT_TYPE_MAP = Collections.unmodifiableMap(map);
        ONT_TYPES = ONT_TYPE_MAP.keySet();

        //NOTE: The following XSD types are not included
        //XSD.gDay,XSD.gMonth,XSD.gMonthDay,XSD.gYearMonth,XSD.hexBinary,XSD.language,
        //XSD.Name,XSD.NCName,XSD.negativeInteger,XSD.NMTOKEN,XSD.nonNegativeInteger,
        //XSD.normalizedString,XSD.positiveInteger,
        //XSD.token,XSD.unsignedByte,XSD.unsignedInt,XSD.unsignedLong,XSD.unsignedShort,
    }
    
    protected final IRI ontProperty;
    
    protected final Converter converter;
    /**
     * Getter for the OntologyProperty for this mapping
     * @return the ontProperty
     */
    public final IRI getOntologyProperty() {
        return ontProperty;
    }
    /**
     * Getter for the set of Tika {@link Metadata} key names that are used
     * by this mapping. This is typically used to determine if based on the 
     * present {@link Metadata#names()} a mapping need to be processed or not.
     * <p>Mappings need to be called if any of the returned keys is present in
     * the {@link Metadata}. Mappings that return an empty list MUST BE
     * called.
     * @return the Tika {@link Metadata} key names that are used by this mapping.
     * If no keys are mapped than it MUST return an empty list.
     */
    public abstract Set<String> getMappedTikaProperties();
    
    protected final IRI ontType;
    
    protected Mapping(IRI ontProperty,IRI ontType){
        this(ontProperty,ontType,null);
    }
    protected Mapping(IRI ontProperty,IRI ontType,Converter converter){
        if(ontProperty == null){
            throw new IllegalArgumentException("The parsed ontology property MUST NOT be NULL!");
        }
        this.ontProperty = ontProperty;
        if(!ONT_TYPES.contains(ontType)){
            throw new IllegalArgumentException("The ontology type '"+ontType
                + "' is not supported. (supported: "+ONT_TYPES+")");
        }
        this.ontType = ontType;
        this.converter = converter;
    }
    
    /**
     * Applies this mapping based on the parsed {@link Metadata} and stores the 
     * results to {@link Graph}
     * @param graph the ImmutableGraph to store the mapping results
     * @param subject the subject (context) to add the mappings
     * @param metadata the metadata used for applying the mapping
     * @return <code>true</code> if the mapping could be applied based on the
     * parsed data. Otherwise <code>false</code>. This is intended to be used
     * by components that need to check if required mappings could be applied.
     */
    public abstract boolean apply(Graph graph, BlankNodeOrIRI subject, Metadata metadata);
    /**
     * Converts the parsed value based on the mapping information to an RDF
     * {@link RDFTerm}. Optionally supports also validation if the parsed
     * value is valid for the {@link Mapping#ontType ontology type} specified by
     * the parsed mapping.
     * @param value the value
     * @param mapping the mapping
     * @param validate 
     * @return the {@link RDFTerm} or <code>null</code> if the parsed value is
     * <code>null</code> or {@link String#isEmpty() empty}.
     * @throws IllegalArgumentException if the parsed {@link Mapping} is 
     * <code>null</code>
     */
    protected RDFTerm toResource(String value, boolean validate){
        Metadata dummy = null;//used for date validation
        if(value == null || value.isEmpty()){
            return null; //ignore null and empty values
        }
        RDFTerm object;
        if(ontType == null){
            object = new PlainLiteralImpl(value);
        } else if(ontType == RDFS.Resource){
            try {
                if(validate){
                    new URI(value);
                }
                object = new IRI(value);
            } catch (URISyntaxException e) {
                log.warn("Unable to create Reference for value {} (not a valid URI)" +
                        " -> create a literal instead",value);
                object = new PlainLiteralImpl(value);
            }
        } else { //typed literal
            Class<?> clazz = Mapping.ONT_TYPE_MAP.get(ontType);
            if(clazz.equals(Date.class)){ //special handling for dates :(
                //Dates are special, because Clerezza requires W3C date format
                //and Tika uses the iso8601 variants.
                //Because of that here is Tika used to get the Date object for
                //the parsed value and than the LiteralFactory of Clerezza to
                //create the TypedLiteral.
                //Note that because of that no validation is required for
                //Dates.
                
                //Need a dummy metadata object to get access to the private
                //parseDate(..) method
                if(dummy == null) { 
                    dummy = new Metadata();
                }
                //any Property with the Date type could be used here
                dummy.add(DATE.getName(), value);
                Date date = dummy.getDate(DublinCore.DATE); //access parseDate(..)
                if(date != null){ //now use the Clerezza Literal factory
                    object = lf.createTypedLiteral(date);
                } else { //fall back to xsd:string
                    object = new TypedLiteralImpl(value, XSD.string);
                }
            } else {
                object = new TypedLiteralImpl(value, ontType);
            }
            if(validate && clazz != null && 
                    !clazz.equals(Date.class)){ //we need not to validate dates
                try {
                    lf.createObject(clazz,(Literal)object);
                } catch (NoConvertorException e) {
                    log.info("Unable to validate typed literals of type {} because" +
                            "there is no converter for Class {} registered with Clerezza",
                            ontType,clazz);
                } catch (InvalidLiteralTypeException e) {
                    log.info("The value '{}' is not valid for dataType {}!" +
                            "create literal with type 'xsd:string' instead",
                            value,ontType);
                    object = new TypedLiteralImpl(value, XSD.string);
                }
            } //else no validation needed
        }
        if(converter != null){
            object = converter.convert(object);
        }
        return object;
    }
    /**
     * Used by subclasses to log mapped information
     */
    protected final static MappingLogger mappingLogger = new MappingLogger();
    /**
     * Allows nicely formatted logging of mapped properties
     * @author Rupert Westenthaler
     *
     */
    protected static final class MappingLogger{
        
        private List<BlankNodeOrIRI> subjects = new ArrayList<BlankNodeOrIRI>();
        private IRI predicate;
        private final int intendSize = 2;
        private final char[] intnedArray;
        private static final int MAX_INTEND = 5;
        
        private MappingLogger(){
            intnedArray = new char[MAX_INTEND*intendSize];
            Arrays.fill(intnedArray, ' ');
        }
        private String getIntend(int intend){
            return String.copyValueOf(intnedArray, 0, 
                Math.min(MAX_INTEND, intend)*intendSize);
        }
        
        protected void log(BlankNodeOrIRI subject,IRI predicate, String prop, RDFTerm object){
            if(!log.isDebugEnabled()){
                return;
            }
            int intendCount = subjects.indexOf(subject)+1;
            final String intend;
            if(intendCount < 1){
                subjects.add(subject);
                intendCount = subjects.size();
                intend = getIntend(intendCount);
                log.debug("{}context: {}",intend,subject);
            } else if(intendCount < subjects.size()){
                for(int i = intendCount;i<subjects.size();i++){
                    subjects.remove(i);
                }
                intend = getIntend(intendCount);
            } else {
                intend = getIntend(intendCount);
            }
            if(!predicate.equals(this.predicate)){
                log.debug("{}  {}",intend,predicate);
            }
            log.debug("{}    {} {}",new Object[]{
                intend,object,prop != null ? ("(from: '"+prop+')') : ""
            });
        }
    }
    
    public static interface Converter {
        RDFTerm convert(RDFTerm value);
    }
}
