package org.stlab.xd.registry.io;

import java.io.InputStream;
import java.io.Reader;

import org.semanticweb.owlapi.model.IRI;

public class IRIRegistrySource implements XDRegistrySource {

	protected IRI iri;

	public IRIRegistrySource(IRI physicalIRI) {
		if (physicalIRI == null)
			throw new RuntimeException(
					"Cannot instantiate IRI registry source on null IRI.");
		this.iri = physicalIRI;
	}

	/*
	 * (non-Javadoc)
	 * @see org.stlab.xd.registry.io.XDRegistrySource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.stlab.xd.registry.io.XDRegistrySource#getPhysicalIRI()
	 */
	@Override
	public IRI getPhysicalIRI() {
		return iri;
	}

	/*
	 * (non-Javadoc)
	 * @see org.stlab.xd.registry.io.XDRegistrySource#getReader()
	 */
	@Override
	public Reader getReader() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.stlab.xd.registry.io.XDRegistrySource#isInputStreamAvailable()
	 */
	@Override
	public boolean isInputStreamAvailable() {
		return false;
	}

	@Override
	public boolean isReaderAvailable() {
		return false;
	}

}
