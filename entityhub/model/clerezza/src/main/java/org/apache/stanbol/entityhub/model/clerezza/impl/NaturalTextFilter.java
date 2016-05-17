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
import org.apache.stanbol.entityhub.servicesapi.util.FilteringIterator;
import org.apache.stanbol.entityhub.servicesapi.util.FilteringIterator.Filter;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter implementation to be used in combination with {@link FilteringIterator}
 * to return only {@link Literal} values (may be {@link PlainLiteral}s and/or
 * {@link TypedLiteral}s) that confirm to the parsed set of languages.<p>
 * Parsing <code>null</code>, an empty array is interpreted such that any
 * language is accepted. Parsing "" or <code>null</code> as one element of the
 * array indicated that Literals without any language tag are included. This also
 * includes {@link TypedLiteral}s with the data type <code>xsd:string</code>.<p>
 * Note that parsing:<ul>
 * <li> an empty array will result in all Literals (regardless of the language)
 *      are returned
 * <li> an array that contains only the <code>null</code> element will result in
 *      only Literals without any language tag are returned.
 * </ul>
 * 
 * @author Rupert Westenthaler
 *
 */
public class NaturalTextFilter implements Filter<Literal> {
    private Logger log = LoggerFactory.getLogger(NaturalTextFilter.class);
    /**
     * The xsd:string data type constant used for TypedLiterals to check if the
     * represent an string value!
     */
    private static IRI xsdString = new IRI(DataTypeEnum.String.getUri());
    private final Set<Language> languages;
    private final boolean containsNull;

    public NaturalTextFilter(String...languages){
        if(languages == null || languages.length == 0){
            this.languages = null;
            this.containsNull = true; // if no language is parse accept any (also the default)
        } else {
            Set<Language> languagesConverted = new HashSet<Language>();
            for (String lang1 : languages) {
                
                if (lang1 == null || lang1.equals("")) {
                     languagesConverted.add(null);
                } else {
                    languagesConverted.add(new Language(lang1));
                }
            }
            this.languages = Collections.unmodifiableSet(languagesConverted);
            
            this.containsNull = this.languages.contains(null);
        }
    }
    @Override
    public final boolean isValid(Literal value) {
        if (value.getLanguage() != null){
           if(languages == null) { //no language restrictions
                return true; //return any Plain Literal
            } else {
                Language literalLang = value.getLanguage();
                return languages.contains(literalLang);
            }
        } else if(value.getDataType().equals(xsdString)) {
            /*
             * if the null language is active, than we can also return
             * "normal" literals (with no known language). This includes
             * Types literals with the data type xsd:string
             */
            return containsNull;
        } else {// unknown Literal type -> filter + warning
            log.warn(String.format("Unknown LiteralType %s (lexicalForm=\"%s\") -> ignored! Pleas adapt this implementation to support this type!",
                value.getClass(),value.getLexicalForm()));
            return false;
        }
    }
}
