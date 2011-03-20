package org.apache.stanbol.kres.jersey.writers;

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
