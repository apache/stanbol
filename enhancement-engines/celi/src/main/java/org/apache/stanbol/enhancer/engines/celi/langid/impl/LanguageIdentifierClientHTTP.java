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
package org.apache.stanbol.enhancer.engines.celi.langid.impl;

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

import org.apache.clerezza.rdf.core.impl.util.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LanguageIdentifierClientHTTP {
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
    private static final String SOAP_PREFIX = "<soapenv:Envelope " 
            + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "xmlns:lan=\"http://research.celi.it/LanguageIdentifierWS\">"
            + "<soapenv:Header/><soapenv:Body>";
    /**
     * closes the text, processTextRequest, SOAP body and envelope
     */
    private static final String SOAP_SUFFIX = "</soapenv:Body></soapenv:Envelope>";
    
	private URL serviceEP;
    private final Map<String,String> requestHeaders;
	private final int conTimeout;
	private final Logger log = LoggerFactory.getLogger(getClass());

	
	public LanguageIdentifierClientHTTP(URL serviceUrl, String licenseKey, int conTimeout){
		this.serviceEP=serviceUrl;
		this.conTimeout = conTimeout;
        Map<String,String> headers = new HashMap<String,String>();
        headers.put("Content-Type", CONTENT_TYPE);
        if(licenseKey != null){
            String encoded = Base64.encode(licenseKey.getBytes(UTF8));
            headers.put("Authorization", "Basic "+encoded);
        }
        this.requestHeaders = Collections.unmodifiableMap(headers);
	}
		
	


	//NOTE (rwesten): I rather do the error handling in the EnhancementEngine!
	public List<GuessedLanguage> guessQueryLanguage(String text) throws IOException, SOAPException{
	    if(text == null || text.isEmpty()){ // no text
	        return Collections.emptyList(); //no language
	    }
        //create the POST request
        HttpURLConnection con = Utils.createPostRequest(serviceEP, requestHeaders,conTimeout);
        //write content
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(),UTF8));
        writer.write(SOAP_PREFIX);
        writer.write("<lan:guessQueryLanguage><textToGuess>");
        StringEscapeUtils.escapeXml(writer, text);
        writer.write("</textToGuess></lan:guessQueryLanguage>");
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
        StreamSource source = new StreamSource(stream);

        // Set contents of message
        soapPart.setContent(source);

        SOAPBody soapBody = message.getSOAPBody();

        List<GuessedLanguage> guesses = new Vector<GuessedLanguage>();
		NodeList nlist = soapBody.getElementsByTagNameNS("*","return");
		for (int i = 0; i < nlist.getLength(); i++) {
			try {
				Element result = (Element) nlist.item(i);
				String lang = result.getAttribute("language");
				double d=Double.parseDouble(result.getAttribute("guessConfidence"));
				
				guesses.add(new GuessedLanguage(lang, d));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return guesses;
	}
	
    //NOTE (rwesten): I rather do the error handling in the EnhancementEngine!
	public List<GuessedLanguage> guessLanguage(String text) throws IOException,SOAPException {
       if(text == null || text.isEmpty()){
            //no text -> no language
            return Collections.emptyList();
        }
        //create the POST request
        HttpURLConnection con = Utils.createPostRequest(serviceEP, requestHeaders,conTimeout);
        //write content
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(),UTF8));
        writer.write(SOAP_PREFIX);
        writer.write("<lan:guessLanguage><textToGuess>");
        StringEscapeUtils.escapeXml(writer, text);
        writer.write("</textToGuess></lan:guessLanguage>");
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
        StreamSource source = new StreamSource(stream);

        // Set contents of message
        soapPart.setContent(source);

        SOAPBody soapBody = message.getSOAPBody();

        List<GuessedLanguage> guesses = new Vector<GuessedLanguage>();

		NodeList nlist = soapBody.getElementsByTagNameNS("*","return");
		for (int i = 0; i < nlist.getLength(); i++) {
			try {
				Element result = (Element) nlist.item(i);
				String lang = result.getAttribute("language");
				double d=Double.parseDouble(result.getAttribute("guessConfidence"));
				
				guesses.add(new GuessedLanguage(lang, d));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return guesses;
	}
}
