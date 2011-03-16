package org.apache.stanbol.ontologymanager.ontonet.impl.session;

import java.util.Date;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.session.KReSSessionIDGenerator;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.StringUtils;
import org.semanticweb.owlapi.model.IRI;


public class TimestampedSessionIDGenerator implements KReSSessionIDGenerator {

	private IRI baseIRI;

	public TimestampedSessionIDGenerator(IRI baseIRI) {
		this.baseIRI = baseIRI;
	}

	@Override
	public IRI createSessionID() {
		return IRI.create(StringUtils.stripIRITerminator(baseIRI) + "/session/"
				+ new Date().getTime());
	}

	@Override
	public IRI createSessionID(Set<IRI> exclude) {
		IRI id = null;
		do {
			id = createSessionID();
		} while (exclude.contains(id));
		return id;
	}

	@Override
	public IRI getBaseIRI() {
		return baseIRI;
	}

	@Override
	public void setBaseIRI(IRI baseIRI) {
		this.baseIRI = baseIRI;
	}

}
