package eu.iksproject.kres.manager.session;

import java.util.Date;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.session.KReSSessionIDGenerator;
import eu.iksproject.kres.manager.util.StringUtils;

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
