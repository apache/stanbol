package org.apache.stanbol.enhancer.engines.celi;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.CeliLemmatizerEnhancementEngine;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.morpho.Case;
import org.apache.stanbol.enhancer.nlp.morpho.Definitness;
import org.apache.stanbol.enhancer.nlp.morpho.Gender;
import org.apache.stanbol.enhancer.nlp.morpho.NumberFeature;
import org.apache.stanbol.enhancer.nlp.morpho.Person;
import org.apache.stanbol.enhancer.nlp.morpho.Tense;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMood;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a morphological interpretation of a {@link Token word}. Words might have different interpretations (typically depending on the POS) so this Tag allows to add information about all possible interpretations to a single word. This is
 * needed if no POS information are present or if POS tags are ambiguous or of low confidence.
 * <p>
 * <b>TODO</b>s:
 * <ul>
 * <li>I would like to have {@link Case}, {@link Tense}, ... as own Annotations. However AFAIK those are all grouped to a single interpretation of the Token (driven by the POS tag).</li>
 * <li>Maybe add a possibility to add unmapped information as <code>Map&lt;String,List&lt;String&gt;&gt;</code>
 * </ul>
 * 
 * @author Alessio Bosca
 * 
 */
public class CeliMorphoFeatures {

	public static final UriRef HAS_NUMBER = new UriRef("http://purl.org/olia/olia.owl#hasNumber");
	public static final UriRef HAS_GENDER = new UriRef("http://purl.org/olia/olia.owl#hasGender");
	public static final UriRef HAS_PERSON = new UriRef("http://purl.org/olia/olia.owl#hasPerson");
	public static final UriRef HAS_CASE = new UriRef("http://purl.org/olia/olia.owl#hasCase");
	public static final UriRef HAS_DEFINITENESS = new UriRef("http://purl.org/olia/olia.owl#hasDefiniteness");
	public static final UriRef HAS_MOOD = new UriRef("http://purl.org/olia/olia.owl#hasMood");
	public static final UriRef HAS_TENSE = new UriRef("http://purl.org/olia/olia.owl#hasTense");

	private static final Logger log = LoggerFactory.getLogger(CeliMorphoFeatures.class);

	private String lemma;

	private Set<LexicalCategory> posSet=new HashSet<LexicalCategory>();
	private Set<Gender> genderSet= new HashSet<Gender>();
	private Set<NumberFeature> numberSet=new HashSet<NumberFeature>();
	private Set<Case> caseFeatureSet=new HashSet<Case>();
	private Set<Person> personSet=new HashSet<Person>();
	private Set<Definitness> definitnessSet=new HashSet<Definitness>();
	private Set<VerbMood> verbFormSet=new HashSet<VerbMood>();
	private Set<Tense> tenseSet=new HashSet<Tense>();
	
	public CeliMorphoFeatures(String lemma) {
		if (lemma == null) {
			throw new IllegalArgumentException("The parsed lemma MUST NOT be NULL!");
		}
		this.lemma = lemma;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CeliMorphoFeatures && lemma.equals(((CeliMorphoFeatures) o).lemma)) {
			CeliMorphoFeatures lt = (CeliMorphoFeatures) o;
			return ((genderSet != null && genderSet.equals(lt.genderSet)) || (genderSet == null && lt.genderSet == null)) && ((caseFeatureSet != null && caseFeatureSet.equals(lt.caseFeatureSet)) || (caseFeatureSet == null && lt.caseFeatureSet == null))
					&& ((tenseSet != null && tenseSet.equals(lt.tenseSet)) || (tenseSet == null && lt.tenseSet == null)) && ((numberSet != null && numberSet.equals(lt.numberSet)) || (numberSet == null && lt.numberSet == null))
					&& ((definitnessSet != null && definitnessSet.equals(lt.definitnessSet)) || (definitnessSet == null && lt.definitnessSet == null)) && ((personSet != null && personSet.equals(lt.personSet)) || (personSet == null && lt.personSet == null))
					&& ((verbFormSet != null && verbFormSet.equals(lt.verbFormSet)) || (verbFormSet == null && lt.verbFormSet == null));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return lemma.hashCode() + posSet.hashCode() + genderSet.hashCode() + personSet.hashCode() + caseFeatureSet.hashCode() + definitnessSet.hashCode() + verbFormSet.hashCode() + tenseSet.hashCode();
	}

	public final void addCase(Case caseFeature) {
		this.caseFeatureSet.add(caseFeature);
	}

	public final void addDefinitness(Definitness definitness) {
		this.definitnessSet.add(definitness);
	}

	public final void addGender(Gender gender) {
		this.genderSet.add(gender);
	}

	public final void addNumber(NumberFeature number) {
		this.numberSet.add(number);
	}

	public void addPerson(Person person) {
		this.personSet.add(person);
	}

	public void addPos(LexicalCategory pos) {
		this.posSet.add(pos);
	}

	public void addTense(Tense tense) {
		this.tenseSet.add(tense);
	}

	public void addVerbForm(VerbMood verbForm) {
		this.verbFormSet.add(verbForm);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("MorphoTag(");
		sb.append(lemma);
		for(LexicalCategory pos: posSet){
			sb.append("|pos:").append(pos);
		}
		for(Gender gender: genderSet){
			sb.append("|gender:").append(gender);
		}
		for(NumberFeature num:numberSet) {
			sb.append("|number:").append(num);
		}
		for(Person pers: personSet) {
			sb.append("|person:").append(pers);
		}
		for(Definitness def: definitnessSet) {
			sb.append("|definitness:").append(def);
		}
		for(Case caseFeat:caseFeatureSet) {
			sb.append("|case:").append(caseFeat);
		}
		for (VerbMood vf:verbFormSet) {
			sb.append("|verbForm:").append(vf);
		}
		for(Tense t:tenseSet) {
			sb.append("|tense:").append(t);
		}
		sb.append(')');
		return sb.toString();
	}

	public Collection<? extends Triple> featuresAsTriples(UriRef textAnnotation, Language lang) {
		Collection<TripleImpl> result = new Vector<TripleImpl>();
		result.add(new TripleImpl(textAnnotation, CeliLemmatizerEnhancementEngine.hasLemmaForm, new PlainLiteralImpl(this.lemma, lang)));
		for(LexicalCategory pos: posSet){
			result.add(new TripleImpl(textAnnotation, RDF_TYPE, pos.getUri()));
		}
		for(NumberFeature num: numberSet){
			result.add(new TripleImpl(textAnnotation, HAS_NUMBER, num.getUri()));
		}
		for(Person pers: personSet){
			result.add(new TripleImpl(textAnnotation, HAS_PERSON, pers.getUri()));
		}
		for(Gender gender: genderSet){
			result.add(new TripleImpl(textAnnotation, HAS_GENDER, gender.getUri()));
		}
		for(Definitness def: definitnessSet){
			result.add(new TripleImpl(textAnnotation, HAS_DEFINITENESS, def.getUri()));
		}
		for(Case caseFeat:caseFeatureSet){
			result.add(new TripleImpl(textAnnotation, HAS_CASE, caseFeat.getUri()));
		}
		for (VerbMood vf:verbFormSet){
			result.add(new TripleImpl(textAnnotation, HAS_MOOD, vf.getUri()));
		}
		for(Tense tense:tenseSet){
			result.add(new TripleImpl(textAnnotation, HAS_TENSE, tense.getUri()));
		}
		return result;
	}
}
