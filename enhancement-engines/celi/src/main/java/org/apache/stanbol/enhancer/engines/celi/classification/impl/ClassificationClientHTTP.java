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
package org.apache.stanbol.enhancer.engines.celi.classification.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.impl.util.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ClassificationClientHTTP {
	
	private final static Logger log = LoggerFactory.getLogger(ClassificationClientHTTP.class);
	//NOTE: Defining charset, content-type and SOAP prefix/suffix as
	//      constants does make more easy to configure those things
    /**
     * The UTF-8 {@link Charset}
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * The content type "text/xml; charset={@link #UTF8}"
     */
    private static final String CONTENT_TYPE = "text/xml; charset="+UTF8.name();
    /**
     * The XML version, encoding; SOAP envelope, heder and starting element of the body;
     * processTextRequest and text starting element.
     */
    private static final String SOAP_PREFIX = "<?xml version=\"1.0\" encoding=\""+UTF8.name()+"\"?>" 
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "xmlns:clas=\"http://linguagrid.org/v20110204/classification\"><soapenv:Header/><soapenv:Body>";
    /**
     * closes the text, processTextRequest, SOAP body and envelope
     */
    private static final String SOAP_SUFFIX = "</soapenv:Body></soapenv:Envelope>";
    
    //TODO: This should be configurable
	private static final int maxResultToReturn = 3;
	
	private final URL serviceEP;
	private final String licenseKey;
	private final int conTimeout;
	
	private final Map<String,String> requestHeaders;
	
	
	public ClassificationClientHTTP(URL serviceUrl, String licenseKey, int conTimeout){
		this.serviceEP=serviceUrl;
		this.licenseKey=licenseKey;
		this.conTimeout = conTimeout;
        Map<String,String> headers = new HashMap<String,String>();
        headers.put("Content-Type", CONTENT_TYPE);
        if(licenseKey != null){
            String encoded = Base64.encode(this.licenseKey.getBytes(UTF8));
            headers.put("Authorization", "Basic "+encoded);
        }
        this.requestHeaders = Collections.unmodifiableMap(headers);
	}
	
	public List<Concept> extractConcepts(String text,String lang) throws IOException, SOAPException {
        if(text == null || text.isEmpty()){
            //no text -> no classification
            return Collections.emptyList();
        }

        //create the POST request
        HttpURLConnection con = Utils.createPostRequest(serviceEP, requestHeaders,conTimeout);
        //"stream" the request content directly to the buffered writer
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(),UTF8));
        writer.write(SOAP_PREFIX);
        writer.write("<clas:classify>");
        writer.write("<clas:user>wiki</clas:user>");//TODO: should the user be configurable?
        writer.write("<clas:model>");
        writer.write(lang);
        writer.write("</clas:model>");
        writer.write("<clas:text>");
        StringEscapeUtils.escapeXml(writer, text); //write the escaped text directly to the request
        writer.write("</clas:text>");
        writer.write("</clas:classify>");
        writer.write(SOAP_SUFFIX);
        writer.close();

        //Call the service
        long start = System.currentTimeMillis();
        InputStream stream = con.getInputStream();
        log.debug("Request to {} took {}ms",serviceEP,System.currentTimeMillis()-start);

		MessageFactory msgFactory = MessageFactory.newInstance();
		SOAPMessage message = msgFactory.createMessage();
		SOAPPart soapPart = message.getSOAPPart();

		StreamSource source = new StreamSource(stream);

		// Set contents of message
		soapPart.setContent(source);

		SOAPBody soapBody = message.getSOAPBody();
        List<Concept> extractedConcepts = new Vector<Concept>();
		NodeList nlist = soapBody.getElementsByTagNameNS("*","return");
		for (int i = 0; i < nlist.getLength() && i<maxResultToReturn; i++) {
			Element result = (Element) nlist.item(i);

			//NOTE: (rwesten) implemented a mapping from the CELI classification
			//      to the Stanbol fise:TopicEnhancements (STANBOL-617) that
			//        * one fise:TopicAnnotation is generated per "model"
			//        * the whole label string is used as fise:entity-label
			//        * the uri of the most specific dbpedia ontology type (see
			//          selectClassificationClass) is used as fise:entity-reference
			//      This has the intuition that for users it is easier to grasp
			//      the meaning of the whole lable, while for machines the link
			//      to the most specific dbpedia ontology class is best suited.
			String model = result.getElementsByTagNameNS("*","label").item(0).getTextContent();
			model=model.substring(1, model.length()-1);
			IRI modelConcept = selectClassificationClass(model);
			String conf=result.getElementsByTagNameNS("*","score").item(0).getTextContent();
			Double confidence= new Double(conf);
			extractedConcepts.add(new Concept(model,modelConcept,confidence));
		}
		return extractedConcepts;
	}
    /**
     * TopicClassifications require only a single fise:entity-reference.
     * However the CELI classification service delivers <p>
     * <code><pre>
     *     <ns2:label>[Organisation HockeyTeam SportsTeam]</ns2:label>
     * </pre></code>
     * because of that this method needs to select one of the labels.<p>
     * This method currently selects the 2nd token if there are more than one
     * concept suggestions included. NOTE that the whole literal is used as
     * fise:entity-label!
     * @param classificationLabels the label string
     * @return the selected label
     */
    private IRI selectClassificationClass(String classificationLabels) {
        //NOTE: (rwesten) In general it would be better if CELI could provide
        //      de-referenceable URLs for those suggestions.
        //      If that is possible one would no longer need to link to the
        //      most specific dbpedia ontology class for a category e.g.
        //          http://dbpedia.org/ontology/HockeyTeam
        //      for
        //          [Organisation HockeyTeam SportsTeam]
        //      but e.g.
        //          http://linguagrid.org/category/HockeyTeam
        //      meaning the linguagrid could provide categories as skos thesaurus
        //      via it's web interface
        int start = classificationLabels.charAt(0) == '[' ? 1 : 0;
        int end = classificationLabels.charAt(classificationLabels.length()-1) == ']' ?
                classificationLabels.length() - 1 : classificationLabels.length();
        String[] tmps = classificationLabels.substring(start, end).split(" ");
        return new IRI(NamespaceEnum.dbpedia_ont.getNamespace()+ //the namespace
            (tmps.length > 1 ? tmps[1] : tmps[0])); //the Class for the label
    }	
	
}
