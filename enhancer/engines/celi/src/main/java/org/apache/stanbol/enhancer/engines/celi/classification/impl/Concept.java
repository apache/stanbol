package org.apache.stanbol.enhancer.engines.celi.classification.impl;

import org.apache.clerezza.rdf.core.UriRef;

public class Concept {
	
	private final String label;
	private final UriRef uri;
	private final Double confidence;
	
	public Concept(String label, UriRef uri,Double confidence) {
		super();
		this.label = label;
		this.uri = uri;
		this.confidence = confidence;
	}
	

	public Double getConfidence() {
		return confidence;
	}


    public String getLabel() {
        return label;
    }


    public UriRef getUri() {
        return uri;
    }
	
	
}
