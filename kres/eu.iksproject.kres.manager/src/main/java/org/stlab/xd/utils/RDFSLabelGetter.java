package org.stlab.xd.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLStringLiteral;
import org.semanticweb.owlapi.model.OWLTypedLiteral;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.stlab.xd.lang.Language;

import eu.iksproject.kres.manager.ONManager;

/**
 * Extracts rdfs:label(s) for entity
 * 
 * @author Enrico Daga
 * 
 */
public class RDFSLabelGetter {
	private Map<String, String> allLabels;
	private IRI subject;
	private boolean strict;
	private OWLDataFactory owlFactory = OWLManager.getOWLDataFactory();

	public RDFSLabelGetter(OWLOntology ontology, IRI subject, boolean strict) {
		this.subject = subject;
		this.strict = strict;
		Set<OWLAnnotationAssertionAxiom> individualAnnotations = ontology
				.getAnnotationAssertionAxioms(subject);
		allLabels = new HashMap<String, String>();
		for (OWLAnnotationAssertionAxiom annotation : individualAnnotations) {
			if (annotation.getProperty().equals(
					owlFactory
							.getOWLAnnotationProperty(
									OWLRDFVocabulary.RDFS_LABEL.getIRI()))) {
				OWLAnnotationValue value = annotation.getValue();
				if (value instanceof IRI) {
					IRI asIRI = (IRI) value;
					allLabels.put(asIRI.toQuotedString(), null);
				} else if (value instanceof OWLStringLiteral) {
					OWLStringLiteral sLiteral = (OWLStringLiteral) value;
					allLabels.put(sLiteral.getLiteral(), sLiteral.getLang());
				} else if (value instanceof OWLTypedLiteral) {
					OWLTypedLiteral tLiteral = (OWLTypedLiteral) value;
					allLabels.put(tLiteral.getLiteral(), tLiteral.getDatatype()
							.getIRI().toQuotedString());
				}

			}
		}
	}

	public String getPreferred() {
		String[] s = getForLang(Language.EN.getValue());
		return (s.length == 0) ? "" : s[0];
	}

	public String[] getForLang(String lang) {
		if (lang == null)
			lang = "";
		List<String> forLang = new ArrayList<String>();
		for (Entry<String, String> entry : allLabels.entrySet()) {
			if (lang.equals(entry.getValue())) {
				forLang.add(entry.getKey());
			}
		}

		if (forLang.isEmpty() && allLabels.isEmpty()) {
			// This entity has no labels at all:(
			// If we are not in strict mode, we must return something!
			// If it has a fragment we assume it is human readable
			if (!strict) {
				if (subject.toURI().getFragment() != null
						&& (!"".equals(subject.toURI().getFragment()))) {
					forLang.add(subject.toURI().getFragment());
				} else
					forLang.add(subject.toQuotedString());
			}
		} else {
			for (Entry<String, String> entry : allLabels.entrySet()) {
				forLang.add(entry.getKey());
				break;
			}
		}
		return forLang.toArray(new String[forLang.size()]);

	}
}
