package eu.iksproject.kres.rules.atoms;

import java.net.URI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.api.rules.URIResource;

public class KReSResource implements URIResource {

	private URI uri;
	
	public KReSResource(URI uri){
		this.uri = uri;
	}
	
	@Override
	public URI getURI() {
		return uri;		
	}

	@Override
	public Resource createJenaResource(Model model) {		
		return model.createResource(uri.toString());
	}
	
	@Override
	public String toString() {
		return uri.toString();
	}

}
