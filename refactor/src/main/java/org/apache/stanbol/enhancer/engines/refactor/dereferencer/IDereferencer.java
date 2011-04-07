package org.apache.stanbol.enhancer.engines.refactor.dereferencer;

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
	 * Locations can be local to the file system or remote URIs.
	 * 
	 * @param location
	 * @return {@link InputStream} if the location is resolved. Otherwise a {@link FileNotFoundException} is thrown.
	 * @throws FileNotFoundException
	 */
    InputStream resolve(String location) throws FileNotFoundException;
}
