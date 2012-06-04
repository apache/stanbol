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
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.util.Base64;
import org.apache.commons.io.IOUtils;
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
	
	//NOTE: the request headers are the same for all request - so they can be
	//      initialized in the constructor.
	private final Map<String,String> requestHeaders;
	
	
	public ClassificationClientHTTP(URL serviceUrl, String licenseKey){
		this.serviceEP=serviceUrl;
		this.licenseKey=licenseKey;
        Map<String,String> headers = new HashMap<String,String>();
        headers.put("Content-Type", CONTENT_TYPE);
        if(licenseKey != null){
            String encoded = Base64.encode(this.licenseKey.getBytes(UTF8));
            headers.put("Authorization", "Basic "+encoded);
        }
        this.requestHeaders = Collections.unmodifiableMap(headers);
	}
	
	/*
	 * NOTE: parsing/returning a String requires to create in-memory copies
	 *       of the sent/received data. Imaging users that send the text of
	 *       100 pages PDF files to the Stanbol Enhancer.
	 *       Because of that an implementation that directly streams the
	 *       StringEscapeUtils.escapeXml(..) to the request is preferable 
	 *       
	 *       This will no longer allow to debug the data of the request and
	 *       response. See the commented main method at the end for alternatives
	 */
//	public String doPostRequest(URL url, String body) throws IOException {
//		
//		HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
//		urlConn.setRequestMethod("POST");
//		urlConn.setDoInput(true);
//		if (null != body) {
//			urlConn.setDoOutput(true);
//		} else {
//			urlConn.setDoOutput(false);
//		}
//		urlConn.setUseCaches(false);
//		String	contentType = "text/xml; charset=utf-8";
//		urlConn.setRequestProperty("Content-Type", contentType);
//		if(this.licenseKey!=null){
//			String encoded = Base64.encode(this.licenseKey.getBytes("UTF-8"));
//			urlConn.setRequestProperty("Authorization", "Basic "+encoded);
//		}
//		
//		// send POST output
//		if (null != body) {
//			OutputStreamWriter printout = new OutputStreamWriter(urlConn.getOutputStream(), "UTF-8");
//			printout.write(body);
//			printout.flush();
//			printout.close();
//		}
//		
//		//close connection
//		urlConn.disconnect();
//		
//		// get response data
//		return IOUtils.toString(urlConn.getInputStream(), "UTF-8");
//	}


	//NOTE: forward IOException and SOAPExceptions to allow correct error handling
	//      by the EnhancementJobManager.
	//      Also RuntimeExceptions MUST NOT be cached out of the same reason!
	public List<Concept> extractConcepts(String text,String lang) throws IOException, SOAPException {
        if(text == null || text.isEmpty()){
            //no text -> no classification
            return Collections.emptyList();
        }

        //create the POST request
        HttpURLConnection con = Utils.createPostRequest(serviceEP, requestHeaders);
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

        //NOTE: forward IOException and SOAPExceptions to allow correct error handling
        //      by the EnhancementJobManager.
        //      Also RuntimeExceptions MUST NOT be cached out of the same reason!

//		try {

		// Create SoapMessage
		MessageFactory msgFactory = MessageFactory.newInstance();
		SOAPMessage message = msgFactory.createMessage();
		SOAPPart soapPart = message.getSOAPPart();

		// NOTE: directly use the InputStream provided by the URLConnection!
//			ByteArrayInputStream stream = new ByteArrayInputStream(responseXml.getBytes("UTF-8"));
		StreamSource source = new StreamSource(stream);

		// Set contents of message
		soapPart.setContent(source);

		SOAPBody soapBody = message.getSOAPBody();
        List<Concept> extractedConcepts = new Vector<Concept>();
		NodeList nlist = soapBody.getElementsByTagNameNS("*","return");
		HashSet<String> inserted=new HashSet<String>();
		for (int i = 0; i < nlist.getLength() && i<maxResultToReturn; i++) {
		    //NOTE: do not catch RuntimeExceptions. Error handling is done by
		    //      the EnhancementJobManager!
//			try {
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
			UriRef modelConcept = selectClassificationClass(model);
			String conf=result.getElementsByTagNameNS("*","score").item(0).getTextContent();
			Double confidence= new Double(conf);
			extractedConcepts.add(new Concept(model,modelConcept,confidence));
//			} catch (Exception e) {
//				e.printStackTrace();
//			}

		}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
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
    private UriRef selectClassificationClass(String classificationLabels) {
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
        return new UriRef(NamespaceEnum.dbpedia_ont.getNamespace()+ //the namespace
            (tmps.length > 1 ? tmps[1] : tmps[0])); //the Class for the label
    }	
	
	//NOTE: If you stream the contents directly to the stream, you can no longer
	//      debug the request/response. Because of that it is sometimes
	//      helpful to have a main method for those tests
	//      An even better variant would be to write a UnitTest for that!!
	//      This would be recommended of the called service is still in beta
	//      and may change at any time
    public static void main(String[] args) throws Exception {
        String lang = "fr";
        String text = "Brigitte Bardot, née  le 28 septembre " +
                "1934 à Paris, est une actrice de cinéma et chanteuse française.";
        
        //For request testing
        //Writer request = new StringWriter();
        
        //For response testing
        HttpURLConnection con = Utils.createPostRequest(
            new URL("http://linguagrid.org/LSGrid/ws/dbpedia-classification"),
            Collections.singletonMap("Content-Type", CONTENT_TYPE));
        Writer request = new OutputStreamWriter(con.getOutputStream(),UTF8);
        
        //"stream" the request content directly to the buffered writer
        BufferedWriter writer = new BufferedWriter(request);
        
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
        
        //log the Request (if request testing)
        //log.info("Request \n{}",request.toString());
        
        //for response testing we need to call the service
        //Call the service
        long start = System.currentTimeMillis();
        InputStream stream = con.getInputStream();
        log.info("Request to took {}ms",System.currentTimeMillis()-start);
        log.info("Response:\n{}",IOUtils.toString(stream));
        stream.close();
    }
}
