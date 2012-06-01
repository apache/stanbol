package org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl;

import java.util.List;

public class LexicalEntry {
	
	String wordForm;
	int from, to;
	
	List<Reading> termReadings=null;

	public LexicalEntry(String wordForm, int from, int to) {
		super();
		this.wordForm = wordForm;
		this.from = from;
		this.to = to;
	}

	public String getWordForm() {
		return wordForm;
	}

	public void setWordForm(String wordForm) {
		this.wordForm = wordForm;
	}

	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public int getTo() {
		return to;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public List<Reading> getTermReadings() {
		return termReadings;
	}

	public void setTermReadings(List<Reading> termReadings) {
		this.termReadings = termReadings;
	}
	
	
}

