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
package org.apache.stanbol.enhancer.sace.util;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jrdf.graph.Graph;
import org.jrdf.parser.RdfReader;

public class Util {

    private static SAXBuilder xmlParser;

    static {
        xmlParser = new SAXBuilder();
    }

    public static Document transformToXML (String xml) {
        StringReader inReader = new StringReader(xml);
        Document doc = null;
        try {
            doc = xmlParser.build(inReader);

            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Graph parseXML (String xml) {
        RdfReader reader = new RdfReader();
        return reader.parseRdfXml(new ByteArrayInputStream(xml.getBytes()));
    }


}
