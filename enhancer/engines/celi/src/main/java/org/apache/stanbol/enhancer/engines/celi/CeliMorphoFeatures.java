package org.apache.stanbol.enhancer.engines.celi;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.CeliLemmatizerEnhancementEngine;
import org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.Reading;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.morpho.Case;
import org.apache.stanbol.enhancer.nlp.morpho.CaseTag;
import org.apache.stanbol.enhancer.nlp.morpho.Definitness;
import org.apache.stanbol.enhancer.nlp.morpho.GenderTag;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.morpho.NumberTag;
import org.apache.stanbol.enhancer.nlp.morpho.Person;
import org.apache.stanbol.enhancer.nlp.morpho.Tense;
import org.apache.stanbol.enhancer.nlp.morpho.TenseTag;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMoodTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

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
public class CeliMorphoFeatures extends MorphoFeatures{

    private static CeliTagSetRegistry tagRegistry = CeliTagSetRegistry.getInstance();

    public static final UriRef HAS_NUMBER = new UriRef("http://purl.org/olia/olia.owl#hasNumber");
    public static final UriRef HAS_GENDER = new UriRef("http://purl.org/olia/olia.owl#hasGender");
    public static final UriRef HAS_PERSON = new UriRef("http://purl.org/olia/olia.owl#hasPerson");
    public static final UriRef HAS_CASE = new UriRef("http://purl.org/olia/olia.owl#hasCase");
    public static final UriRef HAS_DEFINITENESS = new UriRef("http://purl.org/olia/olia.owl#hasDefiniteness");
    public static final UriRef HAS_MOOD = new UriRef("http://purl.org/olia/olia.owl#hasMood");
    public static final UriRef HAS_TENSE = new UriRef("http://purl.org/olia/olia.owl#hasTense");

    public static CeliMorphoFeatures parseFrom(Reading reading, String lang){
        if(reading == null){
            return null;
        }
        CeliMorphoFeatures morphoFeature = new CeliMorphoFeatures(reading.getLemma());
        //parse the key,value pairs of the reading using the language as context
        for (Entry<String, List<String>> entry : reading.getLexicalFeatures().entrySet()) {
            String feature = entry.getKey();
            for (String value : entry.getValue()) {
                if (feature.equals("POS")) {
                    morphoFeature.addPos(tagRegistry.getPosTag(lang,value));
                } else if (feature.equals("CASE")) {
                    morphoFeature.addCase(tagRegistry.getCaseTag(lang,value));
                } else if (feature.equals("GENDER")) {
                    morphoFeature.addGender(tagRegistry.getGenderTag(lang,value));
                } else if (feature.equals("NUMBER")) {
                    morphoFeature.addNumber(tagRegistry.getNumber(lang,value));
                } else if (feature.equals("PERSON")) {
                    morphoFeature.addPerson(tagRegistry.getPerson(lang,value));
                } else if (feature.equals("VERB_FORM") || feature.equals("VFORM")) {
                    morphoFeature.addVerbForm(tagRegistry.getVerbMoodTag(lang,value));
                } else if (feature.equals("TENSE") || feature.equals("VERB_TENSE")) {
                    morphoFeature.addTense(tagRegistry.getTenseTag(lang,value));
                }
            }
        }
        return morphoFeature;
    }
    /**
     * Use {@link #parseFrom(Reading, String)} to instantiate
     * @param lemma
     */
    private CeliMorphoFeatures(String lemma) {
	    super(lemma);
	}

	public Collection<? extends Triple> featuresAsTriples(UriRef textAnnotation, Language lang) {
		Collection<TripleImpl> result = new Vector<TripleImpl>();
		result.add(new TripleImpl(textAnnotation, CeliLemmatizerEnhancementEngine.hasLemmaForm, 
		    new PlainLiteralImpl(getLemma(), lang)));
		for(PosTag pos: getPosList()){
		    if(pos.isMapped()){
		        for(LexicalCategory cat : pos.getCategories()){
		            result.add(new TripleImpl(textAnnotation, RDF_TYPE, cat.getUri()));
		        }
		    }
		}
		for(NumberTag num : getNumberList()){
		    if(num.getNumber() != null){
		        result.add(new TripleImpl(textAnnotation, HAS_NUMBER, num.getNumber().getUri()));
		    }
		}
		for(Person pers : getPersonList()){
	        result.add(new TripleImpl(textAnnotation, HAS_PERSON, pers.getUri()));
		}
		for(GenderTag gender : getGenderList()){
		    if(gender.getGender() != null){
		        result.add(new TripleImpl(textAnnotation, HAS_GENDER, gender.getGender().getUri()));
		    }
		}
		for(Definitness def : getDefinitnessList()){
	        result.add(new TripleImpl(textAnnotation, HAS_DEFINITENESS, def.getUri()));
		}
		for(CaseTag caseFeat : getCaseList()){
		    if(caseFeat.getCase() != null){
		        result.add(new TripleImpl(textAnnotation, HAS_CASE, caseFeat.getCase().getUri()));
		    }
		}
		for (VerbMoodTag vf : getVerbMoodList()){
		    if(vf.getVerbForm() != null){
		        result.add(new TripleImpl(textAnnotation, HAS_MOOD, vf.getVerbForm().getUri()));
		    }
		}
		for(TenseTag tense : getTenseList()){
		    if(tense.getTense() != null){
		        result.add(new TripleImpl(textAnnotation, HAS_TENSE, tense.getTense().getUri()));
		    }
		}
		return result;
	}
}
