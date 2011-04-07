package org.apache.stanbol.enhancer.engines.refactor.dereferencer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;


/**
 * 
 * @author andrea.nuzzolese
 *
 */
@Component(immediate = true, metatype = true)
@Service(IDereferencer.class)
public class Dereferencer implements IDereferencer {

	@Override
	public InputStream resolve(String location) throws FileNotFoundException {
		InputStream inputStream = null;
		try {
			URI uri = new URI(location);
			if(uri.isAbsolute()){
				System.out.println("URL : absolute");
				URL url = new URL(location);
				
				URLConnection connection= url.openConnection();
				inputStream = connection.getInputStream();
			}
			else{
				System.out.println("URL : not absolute "+location);
				inputStream = new FileInputStream(location);
			}
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new FileNotFoundException();
		} catch (IOException e) {
			throw new FileNotFoundException();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new FileNotFoundException();
		}
		
		return inputStream;
		
	}
	
	public boolean isAbsoluteLocation(String location){
		URI uri;
		
		try {
			uri = new URI(location);
			return uri.isAbsolute();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		return false;
		
	}
	
	
	public String getLocalName(String location) throws FileNotFoundException {
		String localName = null;
		try {
			URI uri = new URI(location);
			if(uri.isAbsolute()){
				localName = location;
			}
			else{
				System.out.println("URL : not absolute "+location);
				File file = new File(location);
				localName = file.getName();
			}
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new FileNotFoundException();
		}
		
		return localName;
		
	}

}
