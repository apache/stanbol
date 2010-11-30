package org.stlab.xd.registry.io;

import java.io.InputStream;
import java.io.Reader;

import org.semanticweb.owlapi.model.IRI;

public interface XDRegistrySource {
	/**
	 * Each invocation will return a new InputStream.
	 * 
	 * @return
	 */
	public InputStream getInputStream();

	public IRI getPhysicalIRI();

	/**
	 * Each invocation will return a new Reader.
	 * 
	 * @return
	 */
	public Reader getReader();

	public boolean isInputStreamAvailable();

	public boolean isReaderAvailable();

}
