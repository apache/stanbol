package eu.iksproject.fise.dereferencing;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 
 * @author andrea.nuzzolese
 *
 */
public interface IDereferencer {

	/**
	 * 
	 * The resolve method dereferences location and returns input streams.
	 * 
	 * @param location
	 * @return {@link InputStream} if the location is resolved. Otherwise a {@link FileNotFoundException} is thrown.
	 * @throws FileNotFoundException
	 */
	public InputStream resolve(String location) throws FileNotFoundException;
}
