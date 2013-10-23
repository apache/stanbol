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
package org.apache.stanbol.enhancer.engines.dbpspotlight.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parses the XML results given by DBPedia Spotlight.
 * 
 * @author <a href="mailto:iavor.jelev@babelmonkeys.com">Iavor Jelev</a>
 */
public final class XMLParser {

	/**
	 * Do not create instances of Utility Classes
	 */
	private XMLParser(){};
	
	public static NodeList getElementsByTagName(Document doc, String tagName) {

		return doc.getElementsByTagName(tagName);
	}

	public static Document loadXMLFromString(String xml) throws SAXException,
			IOException {
		Document doc = loadXMLFromInputStream(new ByteArrayInputStream(
				xml.getBytes()));
		doc.getDocumentElement().normalize();

		return doc;
	}

	public static Document loadXMLFromInputStream(InputStream is) throws SAXException,
			IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
		}
		Document doc = builder.parse(is);
		is.close();
		doc.getDocumentElement().normalize();

		return doc;
	}

	public static Document loadXMLFromFile(String filePath)
			throws ParserConfigurationException, SAXException, IOException {
		File fXmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();

		return doc;
	}
}