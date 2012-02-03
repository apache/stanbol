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
package org.apache.stanbol.contentorganizer.util;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.util.W3CDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alexdma,sinaci
 * 
 */
public class ClerezzaBackendStatic {

    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";
    final private static String xsdInteger = xsd("integer");
    final private static String xsdInt = xsd("int");
    final private static String xsdShort = xsd("short");
    final private static String xsdByte = xsd("byte");
    final private static String xsdLong = xsd("long");
    final private static String xsdDouble = xsd("double");
    final private static String xsdFloat = xsd("float");
    final private static String xsdAnyURI = xsd("anyURI");
    final private static String xsdDateTime = xsd("dateTime");
    final private static String xsdBoolean = xsd("boolean");
    final private static String xsdString = xsd("string");

    final private static String xsd(String name) {
        return XSD + name;
    }

    private static final Logger log = LoggerFactory.getLogger(ClerezzaBackendStatic.class);

    /*
     * From ContentHub Clerezza backend.
     */
    public static Resource createLiteral(String content, Locale language, URI type) {
        log.debug("creating literal with content \"{}\", language {}, datatype {}", new Object[] {content,
                                                                                                  language,
                                                                                                  type});
        if (language == null && type == null) {
            return createLiteral(content);
        } else if (type == null) {
            return new PlainLiteralImpl(content, new Language(language.getLanguage()));
        } else {
            return LiteralFactory.getInstance().createTypedLiteral(getTypedObject(content, type.toString()));
        }
    }

    /*
     * From ContentHub Clerezza backend.
     */
    public static Resource createLiteral(String content) {
        log.debug("creating literal with content \"{}\"", content);
        return LiteralFactory.getInstance().createTypedLiteral(content);
    }

    private static Object getTypedObject(String content, String type) {
        Object obj = content;
        if (type.toString().equals(xsdInteger)) {
            obj = Integer.valueOf(content);
        } else if (type.toString().equals(xsdInt)) {
            obj = Integer.valueOf(content);
        } else if (type.toString().equals(xsdShort)) {
            obj = Integer.valueOf(content);
        } else if (type.toString().equals(xsdByte)) {
            obj = Integer.valueOf(content);
        } else if (type.toString().equals(xsdLong)) {
            obj = Long.valueOf(content);
        } else if (type.toString().equals(xsdDouble)) {
            obj = Double.valueOf(content);
        } else if (type.toString().equals(xsdFloat)) {
            obj = Float.valueOf(content);
        } else if (type.toString().equals(xsdAnyURI)) {
            obj = new UriRef(content);
        } else if (type.toString().equals(xsdDateTime)) {
            DateFormat dateFormat = new W3CDateFormat();
            try {
                obj = dateFormat.parse(content);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (type.toString().equals(xsdBoolean)) {
            obj = Boolean.valueOf(content);
        } else if (type.toString().equals(xsdString)) {
            obj = content;
        }
        return obj;
    }

}
