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
package org.apache.stanbol.commons.web.base.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;

public class JenaModelTransformer {

	
	public String toText(Model model){
		
		OutputStream outputStream = getStringOutputStream();
		
		model.write(outputStream);
		
		return outputStream.toString();
	}
	
	public Document toDocument(Model model){
		Document dom = null;
		
		OutputStream outputStream = getStringOutputStream();
		
		model.write(outputStream);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			InputSource is = new InputSource( new StringReader(outputStream.toString()));
		    dom = builder.parse( is );
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return dom;
	    
	}
	
	private OutputStream getStringOutputStream(){
		OutputStream outputStream = new OutputStream() {
			
			private StringBuilder string = new StringBuilder();
			
			@Override
			public void write(int b) throws IOException {
				this.string.append((char) b );
	        }

			
	        public String toString(){
	            return this.string.toString();
	        }
		};
		
		return outputStream;
	}
	
}
