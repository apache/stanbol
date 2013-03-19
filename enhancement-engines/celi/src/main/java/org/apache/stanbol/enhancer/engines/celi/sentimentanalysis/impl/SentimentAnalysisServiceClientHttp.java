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
package org.apache.stanbol.enhancer.engines.celi.sentimentanalysis.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import org.apache.clerezza.rdf.core.impl.util.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SentimentAnalysisServiceClientHttp {
	static public final String positive="POSITIVE", negative="NEGATIVE";
	
	/**
     * The UTF-8 {@link Charset}
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * The content type "text/xml; charset={@link #UTF8}"
     */
	private static final String	CONTENT_TYPE = "text/xml; charset="+UTF8.name();
	/**
	 * The XML version, encoding; SOAP envelope, heder and starting element of the body;
	 * processTextRequest and text starting element.
	 */
    private static final String SOAP_PREFIX = "<?xml version=\"1.0\" encoding=\""+UTF8.name()+"\"?>" +
    		"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
    		"xmlns:sen=\"http://SentimentAnalysis.service.celi.it/\"><soapenv:Header/>" +
    		"<soapenv:Body>";
    /**
     * closes the text, processTextRequest, SOAP body and envelope
     */
    private static final String SOAP_SUFFIX = "</soapenv:Body></soapenv:Envelope>";
	
	private final URL serviceEP;
	
	private final String licenseKey;
	
	private final Map<String,String> requestHeaders;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

    private int connectionTimeout;
	
	public SentimentAnalysisServiceClientHttp(URL serviceUrl, String licenseKey, int connectionTimeout){
		this.serviceEP=serviceUrl;
		this.licenseKey = licenseKey;
		this.connectionTimeout = connectionTimeout;
		Map<String,String> headers = new HashMap<String,String>();
		headers.put("Content-Type", CONTENT_TYPE);
		if(licenseKey != null){
		    String encoded = Base64.encode(this.licenseKey.getBytes(UTF8));
		    headers.put("Authorization", "Basic "+encoded);
		}
		this.requestHeaders = Collections.unmodifiableMap(headers);
	}


	public List<SentimentExpression> extractSentimentExpressions(String text, String lang) throws SOAPException, IOException {
	    if(text == null || text.isEmpty()){
	        //no text -> no extractions
	        return Collections.emptyList();
	    }

	    //create the POST request
		HttpURLConnection con = Utils.createPostRequest(serviceEP, requestHeaders,connectionTimeout);
		//write content
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(),UTF8));
		writer.write(SOAP_PREFIX);
		writer.write("<sen:analyzeText><arg0><![CDATA["+text+"]]></arg0><arg1>"+lang+"</arg1></sen:analyzeText>");
		writer.write(SOAP_SUFFIX);
		writer.close();
		//Call the service
		long start = System.currentTimeMillis();
		InputStream stream = con.getInputStream();
		
		log.debug("Request to {} took {}ms",serviceEP,System.currentTimeMillis()-start);

		// Create SoapMessage and parse the results
		MessageFactory msgFactory = MessageFactory.newInstance();
		SOAPMessage message = msgFactory.createMessage();
		SOAPPart soapPart = message.getSOAPPart();
		
		// Load the SOAP text into a stream source
		StreamSource source = new StreamSource(new InputStreamReader(stream,UTF8));

		// Set contents of message
		soapPart.setContent(source);

		SOAPBody soapBody = message.getSOAPBody();
		
		//extract the results
        List<SentimentExpression> sentExpressions = new Vector<SentimentExpression>();
		NodeList nlist = soapBody.getElementsByTagName("relation");
		
		String snippetStr=null;	
		for (int i = 0; i < nlist.getLength(); i++) {
			Element relation = (Element) nlist.item(i);
			String sentimentType=relation.getAttribute("type");
			int startSnippet = Integer.parseInt( relation.getAttribute("start") );
			int endSnippet = Integer.parseInt( relation.getAttribute("end") );
			
			NodeList snippet = relation.getElementsByTagName("snippet");
			if(snippet.getLength()>0){
				snippetStr=snippet.item(0).getTextContent();
			}
			
			List<String> arguments=new Vector<String>();
			NodeList argsList = relation.getElementsByTagName("arguments");
			for (int x = 0; x < argsList.getLength(); x++) {
				NodeList lemmas=((Element)argsList.item(x)).getElementsByTagName("lemma");
				for (int y = 0; y < lemmas.getLength(); y++) {
					Element lemma=(Element)lemmas.item(y);
					String lemmaStr = lemma.getAttribute("lemma");
					if(lemmaStr!=null && lemmaStr.length()>0)
						arguments.add(lemmaStr);
				}
			}
			
			SentimentExpression expr=new SentimentExpression(sentimentType, snippetStr,startSnippet,endSnippet,arguments);
			sentExpressions.add(expr);
		}

		return sentExpressions;
	}
}
