package org.apache.stanbol.rules.manager.atoms;

import java.net.URI;

import org.apache.stanbol.rules.base.api.URIResource;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class KReSBlankNode implements URIResource {

	private String bNode;
	
	public KReSBlankNode(String bNode) {
		this.bNode = bNode;
	}
	
	@Override
	public URI getURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource createJenaResource(Model model) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	public String toString() {
		return bNode;
	}

}
