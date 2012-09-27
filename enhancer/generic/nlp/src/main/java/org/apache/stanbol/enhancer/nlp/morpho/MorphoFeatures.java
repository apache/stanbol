package org.apache.stanbol.enhancer.nlp.morpho;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a morphological interpretation of a {@link Token word}. 
 * Words might have different interpretations (typically depending on the POS)
 * so this Tag allows to add information about all possible interpretations
 * to a single word. 
 * This is needed if no POS information are present or if POS tags are
 * ambiguous or of low confidence.<p>
 * <b>TODO</b>s:<ul>
 * <li> I would like to have {@link Case}, {@link Tense}, ... 
 * as own Annotations. However AFAIK those are all grouped to a
 * single interpretation of the Token (driven by the POS tag).</li>
 * <li> Maybe add a possibility to add unmapped information as
 * <code>Map&lt;String,List&lt;String&gt;&gt;</code>
 * </ul>
 * @author Rupert Westenthaler
 *
 */
public class MorphoFeatures {
    
    private static final Logger log = LoggerFactory.getLogger(MorphoFeatures.class);

    /**
     * 
     */
    private final Collection<PosTag> posTags;
    private final Set<String> posStrings;
    private final Set<LexicalCategory> posLc;
    private final String lemma;
    
    private Gender gender;
    
    private Number number;
    
    private Case caseFeature;
    
    private Tense tense;
    
    private Definitness definitness;

    public MorphoFeatures(String lemma, PosTag...posTags) {
        if(lemma == null){
            throw new IllegalArgumentException("The parsed lemma MUST NOT be NULL!");
        }
        if(posTags == null || posTags.length == 0){
            throw new IllegalArgumentException("The parsed POS tag(s) MUST NOT be NULL nor empty!");
        }
        this.lemma = lemma;
        Set<PosTag> tagSet = new HashSet<PosTag>(Arrays.asList(posTags));
        if(tagSet.remove(null) && tagSet.isEmpty()){
            throw new IllegalArgumentException("The parsed POS tag(s) MUST NOT contain only NULL values!");
        }
        this.posTags = Collections.unmodifiableSet(tagSet);
        //init fast lookup for category and string
        this.posStrings = new HashSet<String>(posTags.length);
        this.posLc = EnumSet.noneOf(LexicalCategory.class);
        for(PosTag pos : tagSet){
            this.posStrings.add(pos.getTag());
            if(pos.getCategory() != null){
                this.posLc.add(pos.getCategory());
            }
        }
    }
    
    /**
     * Checks if the parsed {@link LexicalCategory} is present
     * for this Lemma.<p>
     * This method will only work if the POS tag set used by
     * the Lemmatizer is mapped to lexical categories.
     */
    public final boolean isLexicalCategory(LexicalCategory lc){
        return posLc.contains(lc);
    }

    /**
     * Checks if the parsed POS tag is present for this Lemma<p>
     * NOTE that this method will only work if the POS tag set
     * used by the POS tagger is the same of as of the 
     * Lemmatizer.
     * @param tag the tag
     * @return if the parsed POS tag is valid for this Lemma
     */
    public final boolean isPosTag(String tag){
        return posStrings.contains(tag);
    }
    public final Collection<PosTag> getPosTags(){
        return posTags;
    }
    
    public final Gender getGender() {
        return gender;
    }
    
    public final void setGender(Gender gender) {
        this.gender = gender;
    }

    public final Number getNumber() {
        return number;
    }
    
    public final void setNumber(Number number) {
        this.number = number;
    }
    
    public final String getLemma() {
        return lemma;
    }
    public final Definitness getDefinitness() {
        return definitness;
    }
    public final void setDefinitness(Definitness definitness) {
        this.definitness = definitness;
    }
    public void setTense(Tense tense) {
        this.tense = tense;
    }
    public Tense getTense() {
        return tense;
    }
    public final void setCase(Case caseFeature) {
        this.caseFeature = caseFeature;
    }
    public final Case getCase() {
        return caseFeature;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MorphoTag(");
        sb.append(lemma);
        sb.append("|pos:").append(posStrings);
        if(gender != null){
            sb.append("|gender:").append(gender);
        }
        if(caseFeature != null){
            sb.append("|case:").append(caseFeature);
        }
        if(tense != null){
            sb.append("|tense:").append(tense);
        }
        if(number != null){
            sb.append("|number:").append(number);
        }
        if(definitness != null){
            sb.append("|definitness:").append(definitness);
        }
        sb.append(')');
        return sb.toString();
    }
    @Override
    public int hashCode() {
        return lemma.hashCode()+posStrings.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof MorphoFeatures && lemma.equals(((MorphoFeatures)o).lemma) &&
            posStrings.equals(((MorphoFeatures)o).posStrings)){
            MorphoFeatures lt = (MorphoFeatures)o;
            return ((gender != null && gender.equals(lt.gender)) ||
                    (gender == null && lt.gender == null)) &&
                   ((caseFeature != null && caseFeature.equals(lt.caseFeature)) ||
                    (caseFeature == null && lt.caseFeature == null)) &&
                   ((tense != null && tense.equals(lt.tense)) ||
                    (tense == null && lt.tense == null)) &&
                   ((number != null && number.equals(lt.number)) ||
                    (number == null && lt.number == null)) &&
                   ((definitness != null && definitness.equals(lt.definitness)) ||
                    (definitness == null && lt.definitness == null));
        } else {
            return false;
        }
    }
}
