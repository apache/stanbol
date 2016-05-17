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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator.Adapter;
import org.apache.stanbol.entityhub.model.clerezza.RdfResourceUtils;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Adapter does two things:
 * <ol>
 * <li> It filters {@link Literal}s based on the languages parsed in the
 *      constructor. If no languages are parsed, than all languages are accepted
 * <li> It converts {@link Literal}s to {@link Text}. Only {@link PlainLiteral}
 *      and {@link TypedLiteral} with an xsd data type present in the
 *      {@link RdfResourceUtils#STRING_DATATYPES} are converted. All other literals are
 *      filtered (meaning that <code>null</code> is returned)
 * </ol>
 * The difference of this Adapter to the {@link LiteralAdapter} with the generic
 * type {@link Text} is that the LiteralAdapter can not be used to filter
 * literals based on there language.
 *
 * @author Rupert Westenthaler
 */
public class Literal2TextAdapter<T extends Literal> implements Adapter<T,Text> {
    
    private Logger log = LoggerFactory.getLogger(Literal2TextAdapter.class);
    /**
     * The xsd:string data type constant used for TypedLiterals to check if the
     * represent an string value!
     */
    private static IRI xsdString = new IRI(DataTypeEnum.String.getUri());
    /**
     * Unmodifiable set of the active languages
     */
    private final Set<Language> languages;
    private final boolean containsNull;
    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    /**
     * Filters Literals in the parsed Iterator based on the parsed languages and
     * convert matching Literals to Text
     * @param it the iterator
     * @param lang the active languages. If <code>null</code> or empty, all
     * languages are active. If <code>null</code> is parsed as an element, that
     * also Literals without a language are returned
     */
    public Literal2TextAdapter(String...lang){
        if(lang != null && lang.length>0){
            Set<Language> languagesConverted = new HashSet<Language>();
            for (String lang1 : lang) {
                if (lang1 == null) {
                    languagesConverted.add(null);
                } else {
                    languagesConverted.add(new Language(lang1));
                }
            }
            this.languages = Collections.unmodifiableSet(languagesConverted);
            this.containsNull = languages.contains(null);
        } else{
            this.languages = null;
            this.containsNull = true;
        }
        //init the first element
    }

    @Override
    public final Text adapt(T value, Class<Text> type) {
        if(value.getLanguage() != null) {
            Language literalLang =  value.getLanguage();
            if(languages == null || languages.contains(literalLang)){
                return valueFactory.createText(value);
            } //else wrong language -> filter
        } else {
            if(containsNull && value.getDataType().equals(xsdString)){
                /*
                 * if the null language is active, than we can also return
                 * "normal" literals (with no known language).
                 * But first we need to check the Datatype!
                 */
                return valueFactory.createText(value);
            } // else no xsd:string dataType and therefore not a text with default lang!
        } 
        return null;
    }

}
