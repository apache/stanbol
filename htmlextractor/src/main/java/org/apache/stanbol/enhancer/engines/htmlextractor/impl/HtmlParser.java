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
package org.apache.stanbol.enhancer.engines.htmlextractor.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

/**
 * HtmlParser.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 *
 */
public class HtmlParser {

    /**
     * This contains the logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HtmlParser.class);

    private Tidy htmlToXmlParser;

    public HtmlParser() {
        this.htmlToXmlParser = new Tidy();
        this.htmlToXmlParser.setTidyMark(false);
        this.htmlToXmlParser.setDropEmptyParas(true);
        this.htmlToXmlParser.setQuiet(true);
        this.htmlToXmlParser.setQuoteAmpersand(true);
        this.htmlToXmlParser.setShowWarnings(false);
        this.htmlToXmlParser.setShowErrors(0);
        this.htmlToXmlParser.setNumEntities(true);
        this.htmlToXmlParser.setHideComments(true);
        this.htmlToXmlParser.setOutputEncoding("UTF-8");
        this.htmlToXmlParser.setXmlOut(true);
    }


    public Document getDOM(String html) {
        if (html != null) {
            return getDOM(new ByteArrayInputStream(html.getBytes()), null);
        }
        return null;
    }


    public synchronized Document getDOM(InputStream html, String charset) {
        if (charset != null) {
            htmlToXmlParser.setInputEncoding(charset);
        }
        Document doc = htmlToXmlParser.parseDOM(html, null);
        return doc;
    }
}
