package eu.iksproject.kres.api.rules;

import java.net.URI;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public interface URIResource {
	
	public URI getURI();
	public Resource createJenaResource(Model model);

}
