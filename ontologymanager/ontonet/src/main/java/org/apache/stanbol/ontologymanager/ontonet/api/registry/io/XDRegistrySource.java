package org.apache.stanbol.ontologymanager.ontonet.api.registry.io;

import java.io.InputStream;
import java.io.Reader;

import org.semanticweb.owlapi.model.IRI;

public interface XDRegistrySource {
	/**
	 * Each invocation will return a new InputStream.
	 * 
	 * @return
	 */
    InputStream getInputStream();

	IRI getPhysicalIRI();

	/**
	 * Each invocation will return a new Reader.
	 * 
	 * @return
	 */
    Reader getReader();

	boolean isInputStreamAvailable();

	boolean isReaderAvailable();

}
