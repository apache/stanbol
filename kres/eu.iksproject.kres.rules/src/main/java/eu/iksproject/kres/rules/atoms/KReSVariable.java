package eu.iksproject.kres.rules.atoms;

import java.net.URI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.api.rules.URIResource;
import eu.iksproject.kres.ontologies.SWRL;

public class KReSVariable implements URIResource {

	URI uri;
	
	public KReSVariable(URI uri) {
		this.uri = uri;
	}

	@Override
	public URI getURI() {
		// TODO Auto-generated method stub
		return null;
	}

		@Override
	public Resource createJenaResource(Model model) {
		return model.createResource(uri.toString(), SWRL.Variable);
	}
		
	@Override
	public String toString() {
		return uri.toString();
	}
	
}
