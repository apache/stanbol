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
/**
 *
 */
package org.apache.stanbol.enhancer.rdfentities.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.NoConvertorException;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.enhancer.rdfentities.Rdf;
import org.apache.stanbol.enhancer.rdfentities.RdfEntity;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;


public class RdfProxyInvocationHandler implements InvocationHandler {

    /**
     * The getID method of the RdfEntity Interface
     */
    protected static final Method getIDMethod;

    /**
     * The toString Method of {@link Object}
     */
    protected static final Method toString;

    /**
     * The equals Method of {@link Object}
     */
    protected static final Method equals;

    /**
     * The hashCode Method of {@link Object}
     */
    protected static final Method hashCode;

    static {
        try {
            getIDMethod = RdfEntity.class.getMethod("getId");
            toString = Object.class.getMethod("toString");
            equals = Object.class.getMethod("equals", Object.class);
            hashCode = Object.class.getMethod("hashCode");
        } catch (SecurityException e) {
            throw new IllegalStateException("Unable to access getId Method in the "+RdfEntity.class+" Interface",e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find getId Method in the "+RdfEntity.class+" Interface",e);
        }
    }

    /**
     * The logger TODO: Question: How to get the dependencies for logging working with maven :(
     */
//    private static final Logger log = LoggerFactory.getLogger(RdfProxyInvocationHandler.class);

    protected SimpleRdfEntityFactory factory;
    protected LiteralFactory literalFactory;
    protected BlankNodeOrIRI rdfNode;
    private final Set<Class<?>> interfaces;
    public RdfProxyInvocationHandler(SimpleRdfEntityFactory factory, BlankNodeOrIRI rdfNode, Class<?>[] parsedInterfaces, LiteralFactory literalFactory){
        this.rdfNode = rdfNode;
        this.factory = factory;
        this.literalFactory = literalFactory;
        //TODO If slow implement this by directly using the Graph Interface!
        Collection<IRI> nodeTypes = getValues(Properties.RDF_TYPE, IRI.class);
        Set<Class<?>> interfaceSet = new HashSet<Class<?>>();
        for (Class<?> clazz : parsedInterfaces){
            if(!clazz.isInterface()){
                throw new IllegalStateException("Parsed Class "+clazz+" is not an interface!");
            }
            interfaceSet.add(clazz);
            getSuperInterfaces(clazz, interfaceSet);
        }
        this.interfaces = Collections.unmodifiableSet(interfaceSet); //nobody should be able to change this!
        for (Class<?> clazz : this.interfaces){
            Rdf classAnnotation = clazz.getAnnotation(Rdf.class);
            if(classAnnotation == null){
            } else { //check of the type statement is present
                IRI typeRef = new IRI(classAnnotation.id());
                if(!nodeTypes.contains(typeRef)){
                    //TODO: Question: How to get the dependencies for logging working with maven :(
                    //log.debug("add type "+typeRef+" for interface "+clazz+" to node "+rdfNode);
                    addValue(Properties.RDF_TYPE, typeRef); //add the missing type!
                } else {
                    // else the type is already present ... nothing to do
                    //TODO: Question: How to get the dependencies for logging working with maven :(
                    //log.debug("type "+typeRef+" for interface "+clazz+" is already present for node "+rdfNode);
                }
            }
        }
    }

    private static void getSuperInterfaces(Class<?> interfaze, Collection<Class<?>> interfaces){
        for (Class<?> superInterface : interfaze.getInterfaces()){
            if(superInterface != null){
                interfaces.add(superInterface);
                getSuperInterfaces(superInterface, interfaces); //recursive
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //RdfEntity rdfEntity;
        if(!(proxy instanceof RdfEntity)){
            throw new IllegalArgumentException("Parsed proxy instance is not of type "+RdfEntity.class
                    +". This RdfWrapperInvocationHandler implementations only work for proxies implementing this interface!");
        }
        //First check for Methods defined in RDFEntity and java.lang.Object
        //implementation of the RffEntity Interface method!
        if(method.equals(getIDMethod)){
            return rdfNode;
        }
        //implement toString
        if(method.equals(equals)){
            return args[0] != null && args[0] instanceof RdfEntity && ((RdfEntity)args[0]).getId().equals(rdfNode);
        }
        //implement hashCode
        if(method.equals(hashCode)){
            return rdfNode.toString().hashCode();
        }
        //implement toString
        if(method.equals(toString)){
            return "Proxy for Node "+rdfNode+" and interfaces "+interfaces;
        }
        Rdf rdf = method.getAnnotation(Rdf.class);
        if(rdf == null){
            throw new IllegalStateException("Invoked Method does not have an Rdf annotation!");
        }
        IRI property;
        if(rdf.id().startsWith("http://") || rdf.id().startsWith("urn:")){
            property = new IRI(rdf.id());
        } else {
            throw new IllegalStateException("The id=\""+rdf.id()+"\"provided by the rdf annotation is not an valid URI");
        }
        //check for Write (Setter) Method
        if(method.getReturnType().equals(void.class)){
            Type[] parameterTypes = method.getGenericParameterTypes();
            //check the parameter types to improve error messages!
            //Only methods with a single parameter are supported
            if(parameterTypes.length!=1){
                throw new IllegalStateException("Unsupported parameters for Method "+method.toString()
                        +"! Only setter methodes with a singe parameter are supported.");
            }
            final Type parameterType = parameterTypes[0];
            //now check if args != null and has an element
            if(args == null){
                throw new IllegalArgumentException(
                        "NULL parsed as \"Object[] args\". An array with a single value is expected when calling "+method.toString()+"!");
            }
            if(args.length<1){
                throw new IllegalArgumentException(
                        "An empty array was parsed as \"Object[] args\". An array with a single value is expected when calling method "+method.toString()+"!");
            }
            final Object value = args[0];
            //Handle Arrays
            if(parameterType instanceof Class<?> && ((Class<?>)parameterType).isArray()){
                throw new IllegalStateException("No support for Arrays right now. Use "+Collection.class+" instead");
            }
            //if null is parsed as value we need to delete all values
            if (value == null){
                removeValues(property);
                return null; //setter methods are void -> return null
            }
            //if a collection is parsed we need to check the generic type
            if (Collection.class.isAssignableFrom(value.getClass())){
                Type genericType = null;
                if (parameterTypes[0] instanceof ParameterizedType){
                    for(Type typeArgument : ((ParameterizedType)parameterTypes[0]).getActualTypeArguments()){
                        if (genericType == null){
                            genericType = typeArgument;
                        } else {
                            //TODO: replace with a warning but for testing start with an exception
                            throw new IllegalStateException(
                                    "Multiple generic type definition for method "+method.toString()
                                    +" (generic types: "+((ParameterizedType) parameterTypes[0]).getActualTypeArguments()+")");
                        }
                    }
                }
                setValues(property, (Collection<?>) value);
                return null;
            } else {
                setValue(property, value);
                return null;
            }
        } else { //assume an read (getter) method
            Class<?> returnType = method.getReturnType();
            if (Collection.class.isAssignableFrom(returnType)){
                Type genericType = null;
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType){
                    ParameterizedType type = (ParameterizedType) genericReturnType;
                    for (Type typeArgument : type.getActualTypeArguments()){
                        if (genericType == null){
                            genericType = typeArgument;
                        } else {
                            //TODO: replace with a warning but for testing start with an exception
                            throw new IllegalStateException(
                                    "Multiple generic type definition for method "+method.toString()
                                    +" (generic types: "+type.getActualTypeArguments()+")");
                        }
                    }
                }
                if (genericType == null){
                    throw new IllegalStateException(
                            "Generic Type not defined for Collection in Method "+method.toString()
                            +" (generic type is needed to correctly map rdf values for property "+property);
                }
                return getValues(property, (Class<?>)genericType);
            } else {
                return getValue(property, returnType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(IRI property, Class<T> type){
        Iterator<Triple> results = factory.getGraph().filter(rdfNode, property, null);
        if (results.hasNext()){
            RDFTerm result = results.next().getObject();
            if (result instanceof BlankNodeOrIRI){
                if (RdfEntity.class.isAssignableFrom(type)){
                    return (T)factory.getProxy((BlankNodeOrIRI)result, (Class<? extends RdfEntity>)type);
                } else { //check result for IRI and types IRI, URI or URL
                    if(result instanceof IRI){
                        if (IRI.class.isAssignableFrom(type)){
                            return (T)result;
                        } else if (URI.class.isAssignableFrom(type)){
                            try {
                                return (T)new URI(((IRI)result).getUnicodeString());
                            } catch (URISyntaxException e) {
                                throw new IllegalStateException("Unable to parse "+URI.class
                                        +" for "+IRI.class+" value="+((IRI)result).getUnicodeString());
                            }
                        } else if (URL.class.isAssignableFrom(type)){
                            try {
                                return (T)new URL(((IRI)result).getUnicodeString());
                            } catch (MalformedURLException e) {
                                throw new IllegalStateException("Unable to parse "+URL.class
                                        +" for "+IRI.class+" value="+((IRI)result).getUnicodeString());
                            }
                        } else {
                            throw new IllegalArgumentException("Parsed Type "+type
                                    +" is not compatible for result type "+result.getClass()
                                    +" (value "+result+") of node "+rdfNode+" and property "+property
                                    +"! (Subclass of RdfEntity, IRI, URI or URL is expected for BlankNodeOrIRI Values)");
                        }
                    } else {
                        throw new IllegalArgumentException("Parsed Type "+type
                                +" is not compatible for result type "+result.getClass()
                                +" (value "+result+") of node "+rdfNode+" and property "+property
                                +"! (Subclass of RdfEntity expected as type for BlankNodeOrIRI values that are no instanceof IRI)");
                    }
                }
            } else {
                return literalFactory.createObject(type,(Literal) result);
            }
        } else {
            return null;
        }
    }
    private <T> Collection<T> getValues(IRI property, Class<T> type){
        return new RdfProxyPropertyCollection<T>(property, type);
    }
    private void setValue(IRI property, Object value){
        removeValues(property);
        addValue(property, value);
    }
    private void setValues(IRI property, Collection<?> values){
        removeValues(property);
        for(Object value : values){
            addValue(property, value);
        }
    }
    protected RDFTerm getRdfResource(Object value) throws NoConvertorException{
        if(value instanceof RDFTerm){
            //if the parsed object is already a RDFTerm
            return (RDFTerm) value; //return it
        } else if(value instanceof RdfEntity){ //check for other proxies
            return ((RdfEntity)value).getId();
        } else if(value instanceof URI){ //or URI links
            return new IRI(value.toString());
        } else if(value instanceof URL){ //or URL links
            return new IRI(value.toString());
        } else { //nothing of that
            //try to make an Literal (Clarezza internal Adapters)
            return literalFactory.createTypedLiteral(value);
        }
    }
    private boolean addValue(IRI property, Object value){
        RDFTerm rdfValue;
        try {
            rdfValue = getRdfResource(value);
            return factory.getGraph().add(new TripleImpl(rdfNode, property, rdfValue));
        } catch (NoConvertorException e){
            throw new IllegalArgumentException("Unable to transform "+value.getClass()
                    +" to an RDF Node. Only "+RdfEntity.class+" and RDF Literal Types are supported");
        }
    }
    private boolean removeValue(IRI property, Object value){
        RDFTerm rdfValue;
        try {
            rdfValue = getRdfResource(value);
            return factory.getGraph().remove(new TripleImpl(rdfNode, property, rdfValue));
        } catch (NoConvertorException e){
            throw new IllegalArgumentException("Unable to transform "+value.getClass()
                    +" to an RDF Node. Only "+RdfEntity.class+" and RDF Literal Types are supported");
        }
    }
    private void removeValues(IRI proptery){
        Iterator<Triple> toRemove = factory.getGraph().filter(rdfNode, proptery, null);
        while(toRemove.hasNext()){
            factory.getGraph().remove(toRemove.next());
        }
    }

    /**
     * We need this class to apply changes in the collection to the Graph.
     * This collection implementation is a stateless wrapper over the
     * triples selected by the subject,property pair over the Graph!<br>
     * Default implementation of {@link AbstractCollection} are very poor
     * performance. Because of that this class overrides some methods
     * already implemented by its abstract super class.
     * @author westei
     *
     * @param <T>
     */
    private final class RdfProxyPropertyCollection<T> extends AbstractCollection<T> {

        //private final BlankNodeOrIRI resource;
        private final IRI property;
        private final Class<T> genericType;
        private final boolean entity;
        private final boolean uri;
        private final boolean url;
        private final boolean uriRef;

        private RdfProxyPropertyCollection(IRI property,Class<T> genericType) {
            this.property = property;
            this.genericType = genericType;
            entity = RdfEntity.class.isAssignableFrom(genericType);
            uri = URI.class.isAssignableFrom(genericType);
            url = URL.class.isAssignableFrom(genericType);
            uriRef = IRI.class.isAssignableFrom(genericType);
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                Iterator<Triple> results = factory.getGraph().filter(rdfNode, property, null);
                @Override
                public boolean hasNext() {
                    return results.hasNext();
                }

                @SuppressWarnings("unchecked")
                @Override
                public T next() {
                    RDFTerm value = results.next().getObject();
                    if (entity){
                        //type checks are done within the constructor
                        return (T) factory.getProxy((BlankNodeOrIRI)value, (Class<? extends RdfEntity>)genericType);
                    } else if(uri){
                        try {
                            return (T)new URI(((IRI)value).getUnicodeString());
                        } catch (URISyntaxException e) {
                            throw new IllegalStateException("Unable to parse "+URI.class+" for "+IRI.class+" value="+((IRI)value).getUnicodeString());
                        }
                    } else if(url){
                        try {
                            return (T)new URL(((IRI)value).getUnicodeString());
                        } catch (MalformedURLException e) {
                            throw new IllegalStateException("Unable to parse "+URL.class+" for "+IRI.class+" value="+((IRI)value).getUnicodeString());
                        }
                    } else if(uriRef){
                        return (T)value;
                    } else {
                        return literalFactory.createObject(genericType, (Literal)value);
                    }
                }

                @Override
                public void remove() {
                    results.remove(); //no Idea if Clerezza implements that ^
                }
            };
        }

        @Override
        public int size() {
            Iterator<Triple> results = factory.getGraph().filter(rdfNode, property, null);
            int size = 0;
            for (;results.hasNext();size++){
                results.next();
            }
            return size;
        }
        public boolean add(T value) {
            return addValue(property, value);
        }
        @Override
        public boolean remove(Object value) {
            return removeValue(property,value);
        }
        @Override
        public boolean isEmpty() {
            return !factory.getGraph().filter(rdfNode, property, null).hasNext();
        }
    }
}
