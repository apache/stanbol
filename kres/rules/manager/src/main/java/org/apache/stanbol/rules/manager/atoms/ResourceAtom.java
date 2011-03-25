package org.apache.stanbol.rules.manager.atoms;

import java.net.URI;

import org.apache.stanbol.rules.base.api.URIResource;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class ResourceAtom implements URIResource {

	private URI uri;
	
	public ResourceAtom(URI uri){
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
		return "<" + uri.toString() + ">";
	}

}
