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
package org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

import org.apache.clerezza.rdf.core.impl.util.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LemmatizerClientHTTP {
	
    /**
     * The UTF-8 {@link Charset}
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * The content type "text/xml; charset={@link #UTF8}"
     */
    private static final String CONTENT_TYPE = "text/xml; charset="+UTF8.name();

    private static final String SOAP_REQUEST_PREFIX = "<soapenv:Envelope " +
    		"xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
    		"xmlns:mor=\"http://research.celi.it/MorphologicalAnalyzer\">" +
    		"<soapenv:Header/><soapenv:Body>";
    private static final String SOAP_REQUEST_SUFFIX = "</soapenv:Body></soapenv:Envelope>";
    
    private URL serviceEP;
	private String licenseKey;
	private final int conTimeout;
    private final Map<String,String> requestHeaders;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public LemmatizerClientHTTP(URL serviceUrl, String licenseKey, int conTimeout){
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
	
	public List<LexicalEntry> performMorfologicalAnalysis(String text,String lang) throws IOException, SOAPException {

        //create the POST request
        HttpURLConnection con = Utils.createPostRequest(serviceEP, requestHeaders,conTimeout);
        //write content
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(),UTF8));
        //write the SOAP envelope, header and start the body
        writer.write(SOAP_REQUEST_PREFIX);
        //write the data (language and text)
        writer.write("<mor:inputText lang=\"");
        writer.write(lang);
        writer.write("\" text=\"");
        StringEscapeUtils.escapeXml(writer,text);
        writer.write("\"/>");
        //close the SOAP body and envelope
        writer.write(SOAP_REQUEST_SUFFIX);
        writer.close();
        //Call the service
        long start = System.currentTimeMillis();
        InputStream stream = con.getInputStream();
        log.debug("Request to {} took {}ms",serviceEP,System.currentTimeMillis()-start);
        if(log.isTraceEnabled()){
            //log the response if trace is enabled
            String soapResponse = IOUtils.toString(stream,"UTF-8");
            log.trace("SoapResponse: \n{}\n",soapResponse);
            stream = new ByteArrayInputStream(soapResponse.getBytes(Charset.forName("UTF-8")));
        }
		// Create SoapMessage
		MessageFactory msgFactory = MessageFactory.newInstance();
		SOAPMessage message = msgFactory.createMessage();
		SOAPPart soapPart = message.getSOAPPart();

		// Load the SOAP text into a stream source
		StreamSource source = new StreamSource(stream);

		// Set contents of message
		soapPart.setContent(source);
		
		SOAPBody soapBody = message.getSOAPBody();

		
		List<LexicalEntry> lista=new Vector<LexicalEntry>(); 
		NodeList nlist = soapBody.getElementsByTagNameNS("*","LexicalEntry");
		for (int i = 0; i < nlist.getLength() ; i++) {
			Element result = (Element) nlist.item(i);
			String wordForm = result.getAttribute("WordForm");
			int from = Integer.parseInt(result.getAttribute("OffsetFrom"));
			int to = Integer.parseInt(result.getAttribute("OffsetTo"));
			LexicalEntry le=new LexicalEntry(wordForm, from, to);
			
			List<Reading> readings = new Vector<Reading>();
			NodeList lemmasList = result.getElementsByTagNameNS("*","Lemma");
			if(lemmasList!=null && lemmasList.getLength()>0){
				for(int j=0;j<lemmasList.getLength();j++){
					Element lemmaElm = (Element) lemmasList.item(j);
					String lemma = lemmaElm.getTextContent();
					NodeList features = ((Element)lemmaElm.getParentNode()).getElementsByTagNameNS("*","LexicalFeature");
					Hashtable<String,List<String>> featuresMap=new Hashtable<String,List<String>>();
					for(int k=0;features!=null && k<features.getLength();k++){
						Element feat = (Element) features.item(k);
						String name = feat.getAttribute("name");
						String value = feat.getTextContent();
						List<String> values=null;
						if(featuresMap.containsKey(name))
							values=featuresMap.get(name);
						else
							values=new Vector<String>();
						values.add(value);
						featuresMap.put(name, values);
					}
					Reading r=new Reading(lemma, featuresMap);
					readings.add(r);
				}
			}
			
			le.setTermReadings(readings);
			lista.add(le);
		}
		return lista;
	}


	public String lemmatizeContents(String text,String lang) throws SOAPException, IOException {
        //create the POST request
        HttpURLConnection con = Utils.createPostRequest(serviceEP, requestHeaders,conTimeout);
        //write content
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(),UTF8));
        //write the SOAP envelope, header and start the body
        writer.write(SOAP_REQUEST_PREFIX);
        writer.write("<mor:inputText lang=\"");
        writer.write(lang);
        writer.write("\" text=\"");
        StringEscapeUtils.escapeXml(writer, text);
        writer.write("\"/>");
        writer.write(SOAP_REQUEST_SUFFIX);
        writer.close();
        //Call the service
        long start = System.currentTimeMillis();
        InputStream stream = con.getInputStream();
        log.debug("Request to {} took {}ms",serviceEP,System.currentTimeMillis()-start);

		// Create SoapMessage
		MessageFactory msgFactory = MessageFactory.newInstance();
		SOAPMessage message = msgFactory.createMessage();
		SOAPPart soapPart = message.getSOAPPart();

		// Load the SOAP text into a stream source
		StreamSource source = new StreamSource(stream);

		// Set contents of message
		soapPart.setContent(source);
		
		SOAPBody soapBody = message.getSOAPBody();
        StringBuilder buff= new StringBuilder();
		NodeList nlist = soapBody.getElementsByTagNameNS("*","LexicalEntry");
		for (int i = 0; i < nlist.getLength() ; i++) {
			Element result = (Element) nlist.item(i);
			NodeList lemmasList = result.getElementsByTagNameNS("*","Lemma");
			if(lemmasList!=null && lemmasList.getLength()>0){
				HashSet<String> lemmas=new HashSet<String>();
				for(int j=0;j<lemmasList.getLength();j++){
					lemmas.add(lemmasList.item(j).getTextContent());
				}
				for(String lemma: lemmas){
				    buff.append(lemma).append(' ');
				}
			} else {
				buff.append(result.getAttributeNS("*","WordForm")).append(' ');
			}
		}
		return buff.toString().trim();
	}

}
