package org.apache.stanbol.enhancer.engines.celi.langid.impl;

public class GuessedLanguage {

	private String lang;
	private double confidence;

	public GuessedLanguage(String lang, double d) {
		this.lang=lang;
		this.confidence=d;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	
}
