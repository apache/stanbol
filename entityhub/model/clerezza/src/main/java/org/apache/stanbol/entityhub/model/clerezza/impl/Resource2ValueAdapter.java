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
package org.apache.stanbol.entityhub.model.clerezza.impl;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator.Adapter;
import org.apache.stanbol.entityhub.model.clerezza.RdfResourceUtils;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.model.clerezza.RdfResourceUtils.XsdDataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Name;


/**
 * Converts the Resources (used to store field values in the Clerezza triple store) back to values as defined
 * by the {@link Representation} interface
 * 
 * @author Rupert Westenthaler
 * 
 * @param <T>
 *            the type of the RDFTerm that can be converted to values
 */
public class Resource2ValueAdapter<T extends RDFTerm> implements Adapter<T,Object> {

    private static Logger log = LoggerFactory.getLogger(Resource2ValueAdapter.class);

    private final LiteralFactory literalFactory = LiteralFactory.getInstance();

    private RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    @Override
    public final Object adapt(T value, Class<Object> type) {
        if (value instanceof IRI) {
            return valueFactory.createReference(value);
        } else if (value instanceof Literal) {
            Literal literal = (Literal) value;
            if (literal.getDataType() == null) { // if no dataType is defined
                // return a Text without a language
                if (literal.getLanguage() != null){
                    return valueFactory.createText(literal.getLexicalForm(), literal.getLanguage().toString());
                } else {
                    return valueFactory.createText(literal);
                }
            } else {
                XsdDataTypeEnum mapping = RdfResourceUtils.XSD_DATATYPE_VALUE_MAPPING.get(literal.getDataType());
                if (mapping == null &&
                        literal.getDataType().getUnicodeString().equals(NamespaceEnum.rdf+"langString") &&
                        literal.getLexicalForm() != null &&
                        !literal.getLexicalForm().isEmpty()){
                    if (literal.getLanguage() != null) {
                        return valueFactory.createText(literal.getLexicalForm(), literal.getLanguage().toString());
                    } else {
                        return valueFactory.createText(literal.getLexicalForm(), null);
                    }
                }
                if (mapping != null) {
                    if (mapping.getMappedClass() != null) {
                        try {
                            return literalFactory.createObject(mapping.getMappedClass(), literal);
                        } catch (RuntimeException e){
                            log.info("Unable to convert Literal value {} to Java Class {} because of {} with message {}",
                                new Object[]{literal,mapping.getMappedClass().getSimpleName(),
                                             e.getClass().getSimpleName(),e.getMessage()});
                            log.trace("Exception:",e);
                            //STANBOL-698: Decide what to do in such cases
                            //(a) throw an exception
                            // throw e;
                            //(b) ignore illegal values
                            //return null;
                            //(c) use the lexical form
                            return literal.getLexicalForm();
                        }
                    } else { // if no mapped class
                        // bypass the LiteralFactory and return the string
                        // representation
                        return literal.getLexicalForm();
                    }
                } else { // if dataType is not supported
                    /*
                     * this could indicate two things: 1) the SimpleLiteralFactory supports a new DataType and
                     * because of that it creates Literals with this Type 2) Literals with this data type
                     * where created by other applications. In the first case one need to update the
                     * enumeration. In the second case using the LexicalForm should be OK Rupert Westenthaler
                     * 2010.10.28
                     */
                    log.warn("Missing Mapping for DataType {} -> return String representation",
                        literal.getDataType().getUnicodeString());
                    return literal.getLexicalForm();
                }
            }
        } else {
            log.warn("Unsupported RDFTerm Type {} -> return String by using the toString method",
                value.getClass());
            return value.toString();
        }
    }

}
