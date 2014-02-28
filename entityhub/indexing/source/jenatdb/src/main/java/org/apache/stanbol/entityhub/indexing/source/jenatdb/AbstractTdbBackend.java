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
package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import java.net.URI;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.model.backend.AbstractBackend;

import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;

/**
 * Implements all the value converter methods of {@link RDFBackend}.
 * @author Rupert Westenthaler
 *
 */
public abstract class AbstractTdbBackend extends AbstractBackend<Node> implements RDFBackend<Node> {

    /**
     * Avoids massive instance creation for language literal
     */
    private Map<String,Locale> localeCache = new TreeMap<String,Locale>();
    /**
     * Avoids massive instance creation for typed literal URIs
     */
    private Map<String,URI> xsdTypeCache = new TreeMap<String,URI>();
    /**
     * Provides the jena type for a type URI
     */
    private TypeMapper typeMapper = TypeMapper.getInstance();
    
    private Locale toLocale(String lang){
      //Jena TDB uses '' for representing Literals without language
        if(lang == null || lang.isEmpty()){ 
            return null;
        }
        Locale locale = localeCache.get(lang);
        if(locale == null){
            locale = new Locale(lang);
            localeCache.put(lang, locale);
        }
        return locale;
    }
    private URI toLiteralTypeURI(String type){
        if(type == null){ 
            return null;
        }
        URI uri = xsdTypeCache.get(type);
        if(uri == null){
            uri = URI.create(type);
            xsdTypeCache.put(type, uri);
        }
        return uri;
    }    
    
    @Override
    public boolean supportsThreading() {
        //threading is supposed to be used in cases where LDpath needs to
        //use remote services. No advantage of having multiple threads on a
        //triple store limited by local File access.
        return false;
    }

    @Override
    public ThreadPoolExecutor getThreadPool() {
        return null;
    }

    @Override
    public boolean isLiteral(Node n) {
        return n.isLiteral();
    }

    @Override
    public boolean isURI(Node n) {
        return n.isURI();
    }

    @Override
    public boolean isBlank(Node n) {
        return n.isBlank();
    }


    @Override
    public Locale getLiteralLanguage(Node n) {
        if(n.isLiteral()){
            return toLocale(n.getLiteralLanguage());
        } else {
            throw new IllegalArgumentException("The parsed Node is not a Literal");
        }
    }


    @Override
    public URI getLiteralType(Node n) {
        if(n.isLiteral()){
            return toLiteralTypeURI(n.getLiteralDatatypeURI());
        } else {
            throw new IllegalArgumentException("The parsed Node is not a Literal");
        }
    }

    @Override
    public Node createLiteral(String content) {
        return Node.createLiteral(content);
    }
    @Override
    public Node createLiteral(String content, Locale language, URI type) {
        return Node.createLiteral(content, 
            language == null ? null : language.getLanguage(), 
            typeMapper.getSafeTypeByName(
                type == null ? null : type.toString()));
    }

    @Override
    public Node createURI(String uri) {
        return Node.createURI(uri);
    }

    @Override
    public String stringValue(Node node) {
        if(node.isLiteral()){
            //we do not want '"example"@en' but 'example'
            return node.getLiteralLexicalForm();
        } else {
            return node.toString();
        }
    }

    @Override
    public Double doubleValue(Node node) {
        if(node.isLiteral()){
            Object value = node.getLiteral().getValue();
            if(value instanceof Double){
                return (Double)value;
            } else {
                return super.doubleValue(node);
            }
        } else {
            throw new IllegalArgumentException("parsed node is not an Literal");
        }
    }

    @Override
    public Long longValue(Node node) {
        if(node.isLiteral()){
            Object value = node.getLiteral().getValue();
            if(value instanceof Long){
                return (Long)value;
            } else {
                return super.longValue(node);
            }
        } else {
            throw new IllegalArgumentException("parsed node is not an Literal");
        }
    }

    @Override
    public Boolean booleanValue(Node node) {
        if(node.isLiteral()){
            Object value = node.getLiteral().getValue();
            if(value instanceof Boolean){
                return (Boolean)value;
            } else {
                return super.booleanValue(node);
            }
        } else {
            throw new IllegalArgumentException("parsed node is not an Literal");
        }
    }

    @Override
    public Date dateTimeValue(Node node) {
        if(node.isLiteral()){
            Object value = node.getLiteral().getValue();
            try {
                if(value instanceof XSDDateTime){
                    return ((XSDDateTime)value).asCalendar().getTime();
                }
            } catch (RuntimeException e) { /*ignore*/}
            return super.dateTimeValue(node);
        } else {
            throw new IllegalArgumentException("parsed node is not an Literal");
        }
    }

    @Override
    public Date dateValue(Node node) {
        if(node.isLiteral()){
            Object value = node.getLiteral().getValue();
            try {
                if(value instanceof XSDDateTime){
                    return new GregorianCalendar(
                        ((XSDDateTime)value).getYears(), 
                        ((XSDDateTime)value).getMonths() -1, 
                        ((XSDDateTime)value).getDays()).getTime();
                }
            } catch (RuntimeException e) { /*ignore*/}
            return super.dateValue(node);
        } else {
            throw new IllegalArgumentException("parsed node is not an Literal");
        }
    }

    @Override
    public Date timeValue(Node node) {
        if(node.isLiteral()){
            Object value = node.getLiteral().getValue();
            try {
                if(value instanceof XSDDateTime){
                    Calendar cal = ((XSDDateTime)value).asCalendar();
                    cal.set(1900, 0, 1); //we need only the time
                    return cal.getTime();
                }
            } catch (RuntimeException e) { /*ignore*/}
            return super.timeValue(node);
        } else {
            throw new IllegalArgumentException("parsed node is not an Literal");
        }
    }

    @Override
    public Float floatValue(Node node) {
        if(node.isLiteral()){
            Object value = node.getLiteral().getValue();
            if(value instanceof Float){
                return (Float)value;
            } else {
                return super.floatValue(node);
            }
        } else {
            throw new IllegalArgumentException("parsed node is not an Literal");
        }
    }

    @Override
    public Integer intValue(Node node) {
        if(node.isLiteral()){
            Object value = node.getLiteral().getValue();
            if(value instanceof Integer){
                return (Integer)value;
            } else {
                return super.intValue(node);
            }
        } else {
            throw new IllegalArgumentException("parsed node is not an Literal");
        }
    }

//    @Override
//    public BigInteger integerValue(Node node) {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public BigDecimal decimalValue(Node node) {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public abstract Collection<Node> listObjects(Node subject, Node property);

    @Override
    public abstract Collection<Node> listSubjects(Node property, Node object);

}
