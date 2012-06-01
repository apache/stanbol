package org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl;

import java.util.Hashtable;

public class Reading {
	
	String lemma;
	Hashtable<String,String> lexicalFeatures;
	
	public Reading(String lemma, Hashtable<String, String> lexicalFeatures) {
		super();
		this.lemma = lemma;
		this.lexicalFeatures = lexicalFeatures;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public Hashtable<String, String> getLexicalFeatures() {
		return lexicalFeatures;
	}

	public void setLexicalFeatures(Hashtable<String, String> lexicalFeatures) {
		this.lexicalFeatures = lexicalFeatures;
	}
	
	
}
