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
package org.apache.stanbol.entityhub.servicesapi.util;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utilities useful for implementations of the Entityhub Model
 * @author Rupert Westenthaler
 *
 */
public final class ModelUtils {

    private static final Logger log = LoggerFactory.getLogger(ModelUtils.class);

    /**
     * Random UUID generator with re-seedable RNG for the tests.
     *
     * @return a new Random UUID
     */
    private static Random rng = new Random();

    /**
     * Do not allow instances of this class
     */
    private ModelUtils(){}

    /**
     * TODO: Maybe we need a better way to generate unique IDs
     * @return
     */
    public static UUID randomUUID() {
        return new UUID(rng.nextLong(), rng.nextLong());
    }

    public static void setSeed(long seed) {
        rng.setSeed(seed);
    }

    /**
     * Comparator based on the {@link RdfResourceEnum#resultScore} property that
     * assumes that values of this property implement {@link Comparable}. The
     * Representation with the highest score will be first
     */
    public static final Comparator<Representation> RESULT_SCORE_COMPARATOR = new Comparator<Representation>() {

        @SuppressWarnings("unchecked")
        @Override
        public int compare(Representation r1, Representation r2) {
            Object score1 = r1.getFirst(RdfResourceEnum.resultScore.getUri());
            Object score2 = r2.getFirst(RdfResourceEnum.resultScore.getUri());
            return score1 == null && score2 == null ? 0 :
                score2 == null ? -1 :
                    score1 == null ? 1 :
                        ((Comparable)score2).compareTo(score1);
        }};
    /**
     * Processes a value parsed as object to the representation.
     * This processing includes:
     * <ul>
     * <li> Removal of <code>null</code> values
     * <li> Converting URIs and URLs to {@link Reference}
     * <li> Converting String[] with at least a single entry where the first
     * entry is not null to {@link Text} (the second entry is used as language.
     * Further entries are ignored.
     * <li> Recursive calling of this Method if a {@link Iterable} (any Array or
     *      {@link Collection}), {@link Iterator} or {@link Enumeration} is parsed.
     * <li> All other Objects are added to the result list
     * </ul>
     * TODO: Maybe we need to enable an option to throw {@link IllegalArgumentException}
     * in case any of the parsed values is invalid. Currently invalid values are
     * just ignored.
     * @param value the value to parse
     * @param results the collections the results of the parsing are added to.
     */
    public static void checkValues(ValueFactory valueFactory, Object value,Collection<Object> results){
        if(value == null){
            return;
        } else if(value instanceof Iterable<?>){
            for(Object current : (Iterable<?>)value){
                checkValues(valueFactory,current,results);
            }
        } else if(value instanceof Iterator<?>){
            while(((Iterator<?>)value).hasNext()){
                checkValues(valueFactory,((Iterator<?>)value).next(),results);
            }
        } else if(value instanceof Enumeration<?>){
            while(((Enumeration<?>)value).hasMoreElements()){
                checkValues(valueFactory,((Enumeration<?>)value).nextElement(),results);
            }
        } else if(value instanceof URI || value instanceof URL){
            results.add(valueFactory.createReference(value.toString()));
        } else if(value instanceof String[]){
            if(((String[])value).length>0 && ((String[])value)[0] != null){
                results.add(valueFactory.createText(((String[])value)[0],
                        ((String[])value).length>1?((String[])value)[1]:null));
            } else {
                log.warn("String[] "+Arrays.toString((String[])value)+" is not a valied natural language array! -> ignore value");
            }
        } else {
            results.add(value);
        }
    }
    /**
     * String representation of the parsed Representation inteded for DEBUG level
     * loggings.
     * @param rep the representation
     * @return the string
     */
    public static String getRepresentationInfo(Representation rep) {
        StringBuilder info = new StringBuilder();
        info.append("Representation id=");
        info.append(rep.getId());
        info.append(" | impl=");
        info.append(rep.getClass());
        info.append('\n');
        for(Iterator<String> fields = rep.getFieldNames();fields.hasNext();){
            String field = fields.next();
            info.append(" o ");
            info.append(field);
            info.append(':');
            Collection<Object> values = new ArrayList<Object>();
            for(Iterator<Object> valueIt = rep.get(field);valueIt.hasNext();){
                values.add(valueIt.next());
            }
            info.append(values);
            info.append('\n');
        }
        return info.toString();
    }
    /**
     * Copies all elements of the parsed Iterator to a {@link ArrayList}.
     * To use other Set implementations that {@link ArrayList} you can use 
     * {@link #addToCollection(Iterator, Collection)
     * @param <T> the generic type of the returned Collection
     * @param it the Iterator with elements compatible to T
     * @return the collection containing all elements of the iterator
     * @throws IllegalArgumentException if the parsed {@link Iterator} is <code>null</code>
     */
    public static <T> Collection<T> asCollection(Iterator<? extends T> it){
        return addToCollection(it, new ArrayList<T>());
    }
    /**
     * Adds the elements of the {@link Iterator} to the parsed Collection
     * @param <T> the type of the collection
     * @param it the iterator over elements that are compatible to T
     * @param c the collection to add the elements
     * @return the parsed Collections with the added Elements
     * @throws IllegalArgumentException if the parsed Collection is <code>null</code>.
     */
    public static <T> Collection<T> addToCollection(Iterator<? extends T> it, Collection<T> c){
        if(it == null){
            return c;
        }
        if(c == null){
            throw new IllegalArgumentException("The parsed Collection MUST NOT be NULL!");
        }
        while(it.hasNext()){
            c.add(it.next());
        }
        return c;
    }
    /**
     * Copies all elements of the parsed Iterator to a {@link HashSet}.
     * To use other Set implementations that {@link HashSet} you can use 
     * {@link #addToSet(Iterator, Set)}
     * @param <T> the generic type of the returned set
     * @param it the Iterator with elements compatible to T
     * @return the set containing all elements of the iterator
     * @throws IllegalArgumentException if the parsed {@link Iterator} is <code>null</code>
     */
    public static <T> Set<T> asSet(Iterator<? extends T> it){
        if(it == null){
            throw new IllegalArgumentException("The parsed Iterator MUST NOT be NULL!");
        }
        return addToSet(it, new HashSet<T>());
    }
    /**
     * Adds the elements of the {@link Iterator} to the parsed {@link Set}
     * @param <T> the type of the set
     * @param it the iterator over elements that are compatible to T
     * @param set the set to add the elements
     * @return the parsed {@link Set} with the added Elements
     * @throws IllegalArgumentException if the parsed Set is <code>null</code>.
     */
    public static <T> Set<T> addToSet(Iterator<? extends T> it,Set<T> set){
        if(it == null){
            return set;
        }
        if(set == null){
            throw new IllegalArgumentException("The parsed Set MUST NOT be NULL!");
        }
        while(it.hasNext()){
            set.add(it.next());
        }
        return set;
    }

    /**
     * Splits up a URI in local name and namespace based on the following rules
     * <ul>
     * <li> If URI starts with "urn:" and last index of ':' == 3 than the there
     *      is no namespace and the whole URI is a local name
     * <li> if the uri starts with "urn:" and the last index of ':' ia > 3, than
     *      the last index ':' is used.
     * <li> split by the last index of '#' if index >= 0
     * <li> split by the last index of '/' if index >= 0
     * <li> return after the first split
     * <li> return the whole URI as local name if no split was performed.
     * </ul>
     * @param uri The uri
     * @return A array with two fields. In the first the namespace is stored (
     * might be <code>null</code>. In the second the local name is stored.
     */
    public static String[] getNamespaceLocalName(String uri){
        String[] parts = new String[2];
        if(uri.startsWith("urn:")){
            if(uri.lastIndexOf(':')>3){
                parts[1] = uri.substring(uri.lastIndexOf(':')+1);
                parts[0] = uri.substring(0, uri.lastIndexOf(':')+1);
            } else {
                parts[1] = uri;
                parts[0] = null;
            }
        } else if(uri.lastIndexOf('#')>=0){
            parts[1] = uri.substring(uri.lastIndexOf('#')+1);
            parts[0] = uri.substring(0, uri.lastIndexOf('#')+1);
        } else if(uri.lastIndexOf('/')>=0){
            parts[1] = uri.substring(uri.lastIndexOf('/')+1);
            parts[0] = uri.substring(0, uri.lastIndexOf('/')+1);
        } else {
            parts[0] = null;
            parts[1] = uri;
        }
        return parts;
    }
    /**
     * This Method uses {@link #getNamespaceLocalName(String)} to split up
     * namespace and local name. It uses also the Data in the
     * {@link NamespaceEnum} to retrieve prefixes for Namespaces.
     * @param uri the URI
     * @return the QName
     */
    public static QName getQName(String uri){
        String[] nsln = getNamespaceLocalName(uri);
        if(nsln[0] != null){
            NamespaceEnum entry = NamespaceEnum.forNamespace(nsln[0]);
            if(entry != null){
                return new QName(nsln[0], nsln[1],entry.getPrefix());
            } else {
                return new QName(nsln[0], nsln[1]);
            }
        } else {
            return new QName(nsln[1]);
        }
    }
    /**
     * Getter for the id of the Entity the parsed {@link Representation metadata}
     * are {@link RdfResourceEnum#aboutRepresentation about}.
     * @param metadata the metadata
     * @return the id of the entity or <code>null</code> if the parsed {@link Representation}
     * is <code>null</code> or does not define a value for 
     * {@link RdfResourceEnum#aboutRepresentation}
     */
    public static String getAboutRepresentation(Representation metadata) throws IllegalStateException{
        if(metadata == null){
            return null;
        }
        Iterator<Reference> refs = metadata.getReferences(RdfResourceEnum.aboutRepresentation.getUri());
        if(refs.hasNext()){
            Reference about = refs.next();
            if(refs.hasNext()){
                log.warn("The parsed Representation {} claims to be the metadata of" +
                    "multiple Entities (entities: {})", 
                    metadata.getId(),
                    asCollection(metadata.getReferences(RdfResourceEnum.aboutRepresentation.getUri())));
            }
            return about.getReference();
        } else {
            return null;
        }
    }
    
}
