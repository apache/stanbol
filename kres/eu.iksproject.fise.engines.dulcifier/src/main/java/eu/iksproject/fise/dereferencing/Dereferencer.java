package eu.iksproject.fise.dereferencing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
		try {
			URL url = new URL(location);
			URLConnection connection= url.openConnection();
			connection.addRequestProperty("Accept","application/rdf+xml");
			return connection.getInputStream();
		} catch (MalformedURLException e) {
			throw new FileNotFoundException();
		} catch (IOException e) {
			throw new FileNotFoundException();
		}
		
	}

}
