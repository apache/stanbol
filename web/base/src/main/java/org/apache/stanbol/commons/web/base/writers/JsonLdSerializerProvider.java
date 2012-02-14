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
package org.apache.stanbol.commons.web.base.writers;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdCommon;
import org.apache.stanbol.commons.jsonld.JsonLdProperty;
import org.apache.stanbol.commons.jsonld.JsonLdPropertyValue;
import org.apache.stanbol.commons.jsonld.JsonLdResource;
import org.apache.stanbol.commons.web.base.format.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements a <a href="http://json-ld.org/">JSON-LD</a> serialization of a Clerezza
 * {@link TripleCollection}.<br>
 *
 * @scr.component immediate="true"
 * @scr.service
 *                 interface="org.apache.clerezza.rdf.core.serializedform.SerializingProvider"
 */
@SupportedFormat(JsonLdSerializerProvider.SUPPORTED_FORMAT)
public class JsonLdSerializerProvider implements SerializingProvider {

    public static final String SUPPORTED_FORMAT = APPLICATION_JSON;

    private static final String RDF_NS_TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private static final Logger logger = LoggerFactory.getLogger(JsonLdSerializerProvider.class);

    // Map from Namespace -> to Prefix
    private Map<String, String> namespacePrefixMap = new HashMap<String, String>();

    private int indentation = 2;
    private boolean useTypeCoercion = true;

    @Override
    public void serialize(OutputStream serializedGraph, TripleCollection tc, String formatIdentifier) {
        String deParameterizedIdentifier;
        int semicolonPos = formatIdentifier.indexOf(';');
        if (semicolonPos > -1) {
            deParameterizedIdentifier = formatIdentifier.substring(0, semicolonPos);
        } else {
            deParameterizedIdentifier = formatIdentifier;
        }
        if (!deParameterizedIdentifier.equalsIgnoreCase(SUPPORTED_FORMAT)) {
            logger.info("serialize() the format '" + deParameterizedIdentifier + "' is not supported by this implementation");
            return;
        }

        JsonLd jsonLd = new JsonLd();
        // If there is no namespace prefix map set, we use the namespaces
        // known from the NamespaceEnum
        if (this.namespacePrefixMap.isEmpty()) {
            for (NamespaceEnum ns : NamespaceEnum.values()) {
                logger.debug("Adding JSON-LD namespace " + ns.getPrefix() + ":" + ns.getNamespace());
                this.namespacePrefixMap.put(ns.getNamespace(), ns.getPrefix());
            }
        }
        jsonLd.setNamespacePrefixMap(this.namespacePrefixMap);
        jsonLd.setUseTypeCoercion(this.useTypeCoercion);

        Map<NonLiteral, String> subjects = createSubjectsMap(tc);
        for (NonLiteral subject : subjects.keySet()) {
            JsonLdResource resource = new JsonLdResource();
            
            String strSubject = subject.toString();
            if (subject instanceof UriRef) {
                UriRef uri = (UriRef) subject;
                strSubject = uri.getUnicodeString();
            }
            resource.setSubject(strSubject);

            Iterator<Triple> triplesFromSubject = tc.filter(subject, null, null);
            while (triplesFromSubject.hasNext()) {
                Triple currentTriple = triplesFromSubject.next();
                if (currentTriple.getPredicate().getUnicodeString().equals(RDF_NS_TYPE)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("serialize() adding rdf:type: \"a\":" + currentTriple.getObject());
                    }
                    resource.addType(((UriRef) currentTriple.getObject()).getUnicodeString());
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("serializer() adding predicate " + currentTriple.getPredicate().toString() + " with object " + currentTriple.getObject().toString());
                    }

                    String property = currentTriple.getPredicate().getUnicodeString();
                    JsonLdProperty jldProperty = resource.getProperty(property);
                    if (jldProperty == null) {
                        jldProperty = new JsonLdProperty(property);
                    }
                    
                    String strValue = currentTriple.getObject().toString();
                    JsonLdPropertyValue jldValue = new JsonLdPropertyValue();

                    if (currentTriple.getObject() instanceof PlainLiteral) {
                        PlainLiteral plain = (PlainLiteral) currentTriple.getObject();
                        if (plain.getLanguage() != null) {
                            jldValue.setLanguage(plain.getLanguage().toString());
                        }
                        strValue = plain.getLexicalForm();
                    } 
                    else if (currentTriple.getObject() instanceof TypedLiteral) {
                        TypedLiteral typedObject = (TypedLiteral) currentTriple.getObject();
                        String type = typedObject.getDataType().getUnicodeString();
                        jldValue.setType(type);
                        strValue = typedObject.getLexicalForm();
                    }
                    else if (currentTriple.getObject() instanceof UriRef) {
                        UriRef uriRef = (UriRef) currentTriple.getObject();
                        jldValue.setType(JsonLdCommon.IRI);
                        strValue = uriRef.getUnicodeString();
                    }
                    
                    jldValue.setValue(convertValueType(strValue));
                    jldProperty.addValue(jldValue);
                    resource.putProperty(jldProperty);
                }
            }

            jsonLd.put(resource.getSubject(), resource);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(serializedGraph,"utf-8"));
            writer.write(jsonLd.toString(this.indentation));
            writer.flush();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    private Map<NonLiteral, String> createSubjectsMap(TripleCollection tc) {
        Map<NonLiteral, String> subjects = new HashMap<NonLiteral, String>();
        int bNodeCounter = 0;
        for (Triple triple : tc) {
            NonLiteral subject = triple.getSubject();
            if (!subjects.containsKey(subject)) {
                if (subject instanceof UriRef) {
                    subjects.put(subject, subject.toString());
                } else if (subject instanceof BNode) {
                    bNodeCounter++;
                    subjects.put(subject, "_:bnode" + bNodeCounter);
                }
            }
        }
        return subjects;
    }
    
    private Object convertValueType(String strValue) {
        String trimedValue = strValue.replaceAll("\"", "");
        
        // check if value can be interpreted as Long
        try {
            return Long.valueOf(trimedValue);
        }
        catch (Throwable t) {};
        
        // check if value can be interpreted as Integer
        try {
            return Integer.valueOf(trimedValue);
        }
        catch (Throwable t) {};
        
        // check if it is a Float value
        try {
            return Float.valueOf(trimedValue);
        }
        catch (Throwable t) {};
        
        // check if it is a Double value
        try {
            return Double.valueOf(trimedValue);
        }
        catch (Throwable t) {};
        
        // check if value can be interpreted as boolean
        if (trimedValue.equalsIgnoreCase("true") || trimedValue.equalsIgnoreCase("false")) {
            return Boolean.valueOf(trimedValue);
        }
        
        // nothing matched - leave untouched
        return strValue;
    }    

    /**
     * Get the known namespace to prefix mapping.
     *
     * @return A {@link Map} from namespace String to prefix String.
     */
    public Map<String, String> getNamespacePrefixMap() {
        return namespacePrefixMap;
    }

    /**
     * Sets the known namespaces for the serializer.
     *
     * @param knownNamespaces A {@link Map} from namespace String to prefix String.
     */
    public void setNamespacePrefixMap(Map<String, String> knownNamespaces) {
        this.namespacePrefixMap = knownNamespaces;
    }

    /**
     * Returns the current number of space characters which are used
     * to indent the serialized output.
     *
     * @return Number of space characters used for indentation.
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * Sets the number of characters used per indentation level for the serialized output.<br />
     * Set this value to zero (0) if you don't want indentation. Default value is 2.
     *
     * @param indentation Number of space characters used for indentation.
     */
    public void setIndentation(int indentation) {
        this.indentation = indentation;
    }

    /**
     * Check if JSON-LD type coercion is applied on serialization.
     * 
     * @return
     */
    public boolean isUseTypeCoercion() {
        return useTypeCoercion;
    }

    /**
     * If JSON-LD type coercion should be applied set this
     * to <code>true</code>.
     * 
     * @param useTypeCoercion
     */
    public void setUseTypeCoercion(boolean useTypeCoercion) {
        this.useTypeCoercion = useTypeCoercion;
    }
    
}
