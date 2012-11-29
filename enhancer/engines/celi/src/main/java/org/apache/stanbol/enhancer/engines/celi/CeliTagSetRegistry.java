package org.apache.stanbol.enhancer.engines.celi;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.enhancer.nlp.model.tag.Tag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.morpho.Case;
import org.apache.stanbol.enhancer.nlp.morpho.CaseTag;
import org.apache.stanbol.enhancer.nlp.morpho.Definitness;
import org.apache.stanbol.enhancer.nlp.morpho.Gender;
import org.apache.stanbol.enhancer.nlp.morpho.GenderTag;
import org.apache.stanbol.enhancer.nlp.morpho.NumberFeature;
import org.apache.stanbol.enhancer.nlp.morpho.NumberTag;
import org.apache.stanbol.enhancer.nlp.morpho.Person;
import org.apache.stanbol.enhancer.nlp.morpho.Tense;
import org.apache.stanbol.enhancer.nlp.morpho.TenseTag;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMood;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMoodTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TagSet}s for known CELI (linguagrid.org) POS and morphological features models.
 * <p>
 * 
 * @author Rupert Westenthaler
 * @author Alessio Bosca
 * 
 */
public final class CeliTagSetRegistry {

    private final static Logger log = LoggerFactory.getLogger(CeliTagSetRegistry.class);

    private static CeliTagSetRegistry instance = new CeliTagSetRegistry();

    private CeliTagSetRegistry() {}

    private final Map<String,TagSet<PosTag>> posMappingsByLanguage = new HashMap<String,TagSet<PosTag>>();
    private final Map<String,Map<String,PosTag>> unmappedPosTagsByLanguage = new HashMap<String,Map<String,PosTag>>();

    private final Map<String,TagSet<GenderTag>> genderMappingsByLanguage = new HashMap<String,TagSet<GenderTag>>();
    private final Map<String,Map<String,GenderTag>> unmappedGenderTagsByLanguage = new HashMap<String,Map<String,GenderTag>>();

    private final Map<String,TagSet<NumberTag>> numberMappingsByLanguage = new HashMap<String,TagSet<NumberTag>>();
    private final Map<String,Map<String,NumberTag>> unmappedNumberTagsByLanguage = new HashMap<String,Map<String,NumberTag>>();

    private final Map<String,Map<String,Person>> personMappingsByLanguage = new HashMap<String,Map<String,Person>>();

    private final Map<String,TagSet<CaseTag>> caseMappingsByLanguage = new HashMap<String,TagSet<CaseTag>>();
    private final Map<String,Map<String,CaseTag>> unmappedCaseTagsByLanguage = new HashMap<String,Map<String,CaseTag>>();

    private final Map<String,Map<String,Definitness>> definitenessMappingsByLanguage = new HashMap<String,Map<String,Definitness>>();

    private final Map<String,TagSet<VerbMoodTag>> verbFormMappingsByLanguage = new HashMap<String,TagSet<VerbMoodTag>>();
    private final Map<String,Map<String,VerbMoodTag>> unmappedVerbMoodTagsByLanguage = new HashMap<String,Map<String,VerbMoodTag>>();

    private final Map<String,TagSet<TenseTag>> tenseMappingsByLanguage = new HashMap<String,TagSet<TenseTag>>();
    private final Map<String,Map<String,TenseTag>> unmappedTenseTagsByLanguage = new HashMap<String,Map<String,TenseTag>>();

    public static CeliTagSetRegistry getInstance() {
        return instance;
    }

    /**
     * Setter for the mappings of {@link TagSet} by language.
     * 
     * @param mappings
     *            expressed with a {@link TagSet}
     */
    private void addPosTagset(TagSet<PosTag> model) {
        for (String lang : model.getLanguages()) {
            if (posMappingsByLanguage.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Models for Language '" + lang
                                                + "'! This is an error in the static confituration of "
                                                + "this class. Please report this to the stanbol-dev mailing"
                                                + "list!");
            }
        }
    }

    /**
     * Getter for a {@link PosTag} based on the Registry configuration
     * 
     * @param language
     *            the language
     * @param tag
     *            the {@link String} tag as returned by CELI
     * @return the {@link PosTag}
     */
    public PosTag getPosTag(String language, String tag) {
        return getTag(posMappingsByLanguage, unmappedPosTagsByLanguage, PosTag.class, language, tag);
    }

    /**
     * Setter for the mappings of {@link TagSet} by language.
     * 
     * @param mappings
     *            expressed with a {@link TagSet}
     */
    private void addGenderTagset(TagSet<GenderTag> model) {
        for (String lang : model.getLanguages()) {
            if (genderMappingsByLanguage.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Models for Language '" + lang
                                                + "'! This is an error in the static confituration of "
                                                + "this class. Please report this to the stanbol-dev mailing"
                                                + "list!");
            }
        }
    }

    /**
     * Getter for a {@link GenderTag} based on the Registry configuration
     * 
     * @param language
     *            the language
     * @param tag
     *            the {@link String} tag as returned by CELI
     * @return the {@link GenderTag}
     */
    public GenderTag getGenderTag(String language, String tag) {
        return getTag(genderMappingsByLanguage, unmappedGenderTagsByLanguage, GenderTag.class, language, tag);
    }

    /**
     * Setter for the mappings of {@link TagSet} by language.
     * 
     * @param mappings
     *            expressed with a {@link TagSet}
     */
    private void addNumberTagset(TagSet<NumberTag> model) {
        for (String lang : model.getLanguages()) {
            if (numberMappingsByLanguage.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Models for Language '" + lang
                                                + "'! This is an error in the static confituration of "
                                                + "this class. Please report this to the stanbol-dev mailing"
                                                + "list!");
            }
        }
    }

    /**
     * Getter for a {@link NumberTag} based on the Registry configuration
     * 
     * @param language
     *            the language
     * @param tag
     *            the {@link String} tag as returned by CELI
     * @return the {@link NumberTag}
     */
    public NumberTag getNumber(String language, String tag) {
        return getTag(numberMappingsByLanguage, unmappedNumberTagsByLanguage, NumberTag.class, language, tag);
    }

    /**
     * Setter for the mappings of {@link TagSet} by language.
     * 
     * @param mappings
     *            expressed with a {@link TagSet}
     */
    private void addPersonMappings(Map<String,Person> model, String...langs) {
        for (String lang : langs) {
            if (personMappingsByLanguage.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Models for Language '" + lang
                                                + "'! This is an error in the static confituration of "
                                                + "this class. Please report this to the stanbol-dev mailing"
                                                + "list!");
            }
        }
    }

    /**
     * Getter for a {@link PersonTag} based on the Registry configuration
     * 
     * @param language
     *            the language
     * @param tag
     *            the {@link String} tag as returned by CELI
     * @return the {@link PersonTag}
     */
    public Person getPerson(String language, String tag) {
        Map<String,Person> langMappings = personMappingsByLanguage.get(language);
        return langMappings == null ? null : langMappings.get(tag);
    }

    /**
     * Setter for the mappings of {@link TagSet} by language.
     * 
     * @param mappings
     *            expressed with a {@link TagSet}
     */
    private void addCaseTagset(TagSet<CaseTag> model) {
        for (String lang : model.getLanguages()) {
            if (caseMappingsByLanguage.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Models for Language '" + lang
                                                + "'! This is an error in the static confituration of "
                                                + "this class. Please report this to the stanbol-dev mailing"
                                                + "list!");
            }
        }
    }

    /**
     * Getter for a {@link CaseTag} based on the Registry configuration
     * 
     * @param language
     *            the language
     * @param tag
     *            the {@link String} tag as returned by CELI
     * @return the {@link PersonTag}
     */
    public CaseTag getCaseTag(String language, String tag) {
        return getTag(caseMappingsByLanguage, unmappedCaseTagsByLanguage, CaseTag.class, language, tag);
    }

    /**
     * Setter for the mappings of {@link TagSet} by language.
     * 
     * @param mappings
     *            expressed with a {@link TagSet}
     */
    private void addDefinitnessTagset(Map<String,Definitness> model,String...langs) {
        for (String lang : langs) {
            if (definitenessMappingsByLanguage.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Models for Language '" + lang
                                                + "'! This is an error in the static confituration of "
                                                + "this class. Please report this to the stanbol-dev mailing"
                                                + "list!");
            }
        }
    }

    /**
     * Getter for a {@link DefinitnessTag} based on the Registry configuration
     * 
     * @param language
     *            the language
     * @param tag
     *            the {@link String} tag as returned by CELI
     * @return the {@link DefinitnessTag}
     */
    public Definitness getDefinitnessTag(String language, String tag) {
        Map<String,Definitness> langMappings = definitenessMappingsByLanguage.get(language);
        return langMappings == null ? null : langMappings.get(tag);
    }

    /**
     * Setter for the mappings of {@link TagSet} by language.
     * 
     * @param mappings
     *            expressed with a {@link TagSet}
     */
    private void addVerbFormTagset(TagSet<VerbMoodTag> model) {
        for (String lang : model.getLanguages()) {
            if (verbFormMappingsByLanguage.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Models for Language '" + lang
                                                + "'! This is an error in the static confituration of "
                                                + "this class. Please report this to the stanbol-dev mailing"
                                                + "list!");
            }
        }
    }

    /**
     * Getter for a {@link VerbMoodTag} based on the Registry configuration
     * 
     * @param language
     *            the language
     * @param tag
     *            the {@link String} tag as returned by CELI
     * @return the {@link VerbMoodTag}
     */
    public VerbMoodTag getVerbMoodTag(String language, String tag) {
        return getTag(verbFormMappingsByLanguage, unmappedVerbMoodTagsByLanguage, VerbMoodTag.class,
            language, tag);
    }

    /**
     * Setter for the mappings of {@link TagSet} by language.
     * 
     * @param mappings
     *            expressed with a {@link TagSet}
     */
    private void addTenseTagset(TagSet<TenseTag> model) {
        for (String lang : model.getLanguages()) {
            if (tenseMappingsByLanguage.put(lang, model) != null) {
                throw new IllegalStateException("Multiple Models for Language '" + lang
                                                + "'! This is an error in the static confituration of "
                                                + "this class. Please report this to the stanbol-dev mailing"
                                                + "list!");
            }
        }
    }

    /**
     * Getter for a {@link TenseTag} based on the Registry configuration
     * 
     * @param language
     *            the language
     * @param tag
     *            the {@link String} tag as returned by CELI
     * @return the {@link TenseTag}
     */
    public TenseTag getTenseTag(String language, String tag) {
        return getTag(tenseMappingsByLanguage, unmappedTenseTagsByLanguage, TenseTag.class, language, tag);
    }

    /**
     * Utility that uses generics and reflection to
     * <ul>
     * <li>lookup mapped {@link Tag}s
     * <li>lookup already created unmapped {@link Tag}s
     * <li>create unmapped {@link Tag}s for {@link String} tags that are encountered the first time
     * </ul>
     * 
     * @param tagSets
     *            the {@link TagSet} with the mapped Tags. The key represents the language
     * @param unmapped
     *            unmapped {@link Tag}s. The key of the outer map is the language. The key of the inner map is
     *            the {@link String} tag.
     * @param clazz
     *            the {@link Class} of the {@link Tag}. Used to create an instance via reflection.
     * @param language
     *            the language
     * @param tag
     *            the string tag
     * @return the Tag
     */
    private static <T extends Tag<T>> T getTag(Map<String,TagSet<T>> tagSets,
                                               Map<String,Map<String,T>> unmapped,
                                               Class<T> clazz,
                                               String language,
                                               String tag) {
        T t = null;
        TagSet<T> tagSet = tagSets.get(language);
        if (tagSet != null) {
            t = tagSet.getTag(tag);
        }
        if (t == null) {
            // warn about missing mappings for Tags to Olia!
            Map<String,T> u = unmapped.get(language);
            if (u == null) {
                u = new HashMap<String,T>();
                unmapped.put(language, u);
            }
            t = u.get(tag);
            if (t == null) {
                try {
                    t = clazz.getConstructor(String.class).newInstance(tag);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Unable to instantiate " + clazz.getSimpleName()
                                                    + "with String tag '" + tag + "'!", e);
                } catch (SecurityException e) {
                    throw new IllegalStateException("Unable to instantiate " + clazz.getSimpleName()
                                                    + "with String tag '" + tag + "'!", e);
                } catch (InstantiationException e) {
                    throw new IllegalStateException("Unable to instantiate " + clazz.getSimpleName()
                                                    + "with String tag '" + tag + "'!", e);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to instantiate " + clazz.getSimpleName()
                                                    + "with String tag '" + tag + "'!", e);
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException("Unable to instantiate " + clazz.getSimpleName()
                                                    + "with String tag '" + tag + "'!", e);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("Unable to instantiate " + clazz.getSimpleName()
                                                    + "with String tag '" + tag + "'!", e);
                }
                log.warn("added unrecognized {} '{}' for Language {}", new Object[] {clazz.getSimpleName(), tag,
                                                                                language});
                u.put(tag, t);
            }
        }
        return t;
    }

    /*****************************************************************
     * POS TAGSETS MAPPINGS TO OLIA ONTOLOGY *
     ****************************************************************/

    public static final TagSet<PosTag> ITALIAN = new TagSet<PosTag>("CELI Italian POS tags", "it");
    static {
        ITALIAN.addTag(new PosTag("N", LexicalCategory.Noun));
        ITALIAN.addTag(new PosTag("NF", LexicalCategory.Noun));
        ITALIAN.addTag(new PosTag("NM", LexicalCategory.Noun));
        ITALIAN.addTag(new PosTag("ADJ", LexicalCategory.Adjective));
        ITALIAN.addTag(new PosTag("ADV", LexicalCategory.Adverb));
        ITALIAN.addTag(new PosTag("ART", LexicalCategory.PronounOrDeterminer));
        ITALIAN.addTag(new PosTag("PRON", LexicalCategory.PronounOrDeterminer));
        ITALIAN.addTag(new PosTag("PRONWH", LexicalCategory.PronounOrDeterminer));
        ITALIAN.addTag(new PosTag("DETDEMO", LexicalCategory.PronounOrDeterminer));
        ITALIAN.addTag(new PosTag("DETINDEF", LexicalCategory.PronounOrDeterminer));
        ITALIAN.addTag(new PosTag("DETPOSS", LexicalCategory.PronounOrDeterminer));
        ITALIAN.addTag(new PosTag("DETWH", LexicalCategory.PronounOrDeterminer));
        ITALIAN.addTag(new PosTag("CHEWH", LexicalCategory.PronounOrDeterminer));
        ITALIAN.addTag(new PosTag("CLI")); //not mapped
        ITALIAN.addTag(new PosTag("CONJ", LexicalCategory.Conjuction));
        ITALIAN.addTag(new PosTag("CONGWH", LexicalCategory.Conjuction));
        ITALIAN.addTag(new PosTag("PREP", LexicalCategory.Adposition));
        ITALIAN.addTag(new PosTag("V", LexicalCategory.Verb));
        ITALIAN.addTag(new PosTag("INT", LexicalCategory.Interjection));
        ITALIAN.addTag(new PosTag("NEG", LexicalCategory.Adverb));
        ITALIAN.addTag(new PosTag("NUM", Pos.Numeral));
        getInstance().addPosTagset(ITALIAN);
    }

    public static final TagSet<PosTag> GERMAN = new TagSet<PosTag>("CELI German POS tags", "de");
    static {
        GERMAN.addTag(new PosTag("N", LexicalCategory.Noun));
        GERMAN.addTag(new PosTag("ADJ", LexicalCategory.Adjective));
        GERMAN.addTag(new PosTag("ADV", LexicalCategory.Adverb));
        GERMAN.addTag(new PosTag("ART", LexicalCategory.PronounOrDeterminer));
        GERMAN.addTag(new PosTag("PRON", LexicalCategory.PronounOrDeterminer));
        GERMAN.addTag(new PosTag("CONJ", LexicalCategory.Conjuction));
        GERMAN.addTag(new PosTag("PREP", LexicalCategory.Adposition));
        GERMAN.addTag(new PosTag("PREPART", LexicalCategory.Adposition));
        GERMAN.addTag(new PosTag("V", LexicalCategory.Verb));
        GERMAN.addTag(new PosTag("INT", LexicalCategory.Interjection));
        GERMAN.addTag(new PosTag("NUM", Pos.Numeral));
        getInstance().addPosTagset(GERMAN);
    }

    public static final TagSet<PosTag> DANISH = new TagSet<PosTag>("CELI Danish POS tags", "da");
    static {
        DANISH.addTag(new PosTag("N", LexicalCategory.Noun));
        DANISH.addTag(new PosTag("NF", LexicalCategory.Noun));
        DANISH.addTag(new PosTag("NN", LexicalCategory.Noun));
        DANISH.addTag(new PosTag("NP", LexicalCategory.Noun));//ProperNoun));
        DANISH.addTag(new PosTag("ADJ", LexicalCategory.Adjective));
        DANISH.addTag(new PosTag("ADV", LexicalCategory.Adverb));
        DANISH.addTag(new PosTag("ART", LexicalCategory.PronounOrDeterminer));
        DANISH.addTag(new PosTag("PRON", LexicalCategory.PronounOrDeterminer));
        DANISH.addTag(new PosTag("CONJ", LexicalCategory.Conjuction));
        DANISH.addTag(new PosTag("PREP", LexicalCategory.Adposition));
        DANISH.addTag(new PosTag("V", LexicalCategory.Verb));
        DANISH.addTag(new PosTag("INT", LexicalCategory.Interjection));
        DANISH.addTag(new PosTag("INVAR", LexicalCategory.Residual));
        DANISH.addTag(new PosTag("NUM", Pos.Numeral));
        getInstance().addPosTagset(DANISH);
    }

    public static final TagSet<PosTag> RUSSIAN = new TagSet<PosTag>("CELI Russian POS tags", "ru");
    static {
        RUSSIAN.addTag(new PosTag("N", LexicalCategory.Noun));
        RUSSIAN.addTag(new PosTag("NF", LexicalCategory.Noun));
        RUSSIAN.addTag(new PosTag("NM", LexicalCategory.Noun));
        RUSSIAN.addTag(new PosTag("NN", LexicalCategory.Noun));
        RUSSIAN.addTag(new PosTag("NP", LexicalCategory.Noun));//ProperNoun));
        RUSSIAN.addTag(new PosTag("NPLUR", LexicalCategory.Noun));
        RUSSIAN.addTag(new PosTag("ADJ", LexicalCategory.Adjective));
        RUSSIAN.addTag(new PosTag("ADV", LexicalCategory.Adverb));
        RUSSIAN.addTag(new PosTag("PRON", LexicalCategory.PronounOrDeterminer));
        RUSSIAN.addTag(new PosTag("CONJ", LexicalCategory.Conjuction));
        RUSSIAN.addTag(new PosTag("PREP", LexicalCategory.Adposition));
        RUSSIAN.addTag(new PosTag("V", LexicalCategory.Verb));
        RUSSIAN.addTag(new PosTag("INT", LexicalCategory.Interjection));
        RUSSIAN.addTag(new PosTag("NUM", Pos.Numeral));
        RUSSIAN.addTag(new PosTag("NUMNON", LexicalCategory.PronounOrDeterminer));
        RUSSIAN.addTag(new PosTag("PART", LexicalCategory.Unique));
        getInstance().addPosTagset(RUSSIAN);
    }

    public static final TagSet<PosTag> ROMANIAN = new TagSet<PosTag>("CELI Romanian POS tags", "ro");
    static {
        ROMANIAN.addTag(new PosTag("N", LexicalCategory.Noun));
        ROMANIAN.addTag(new PosTag("AJ", LexicalCategory.Adjective));
        ROMANIAN.addTag(new PosTag("AV", LexicalCategory.Adverb));
        ROMANIAN.addTag(new PosTag("AT", LexicalCategory.PronounOrDeterminer));
        ROMANIAN.addTag(new PosTag("PD", LexicalCategory.PronounOrDeterminer));
        ROMANIAN.addTag(new PosTag("C", LexicalCategory.Conjuction));
        ROMANIAN.addTag(new PosTag("AP", LexicalCategory.Adposition));
        ROMANIAN.addTag(new PosTag("V", LexicalCategory.Verb));
        ROMANIAN.addTag(new PosTag("I", LexicalCategory.Interjection));
        ROMANIAN.addTag(new PosTag("INVAR", LexicalCategory.Residual));
        ROMANIAN.addTag(new PosTag("NU", Pos.Numeral));
        ROMANIAN.addTag(new PosTag("R", LexicalCategory.Residual));
        ROMANIAN.addTag(new PosTag("U", LexicalCategory.Unique));
        getInstance().addPosTagset(ROMANIAN);
    }

    /*****************************************************************
     * MORPHOLOGICAL FEATURES TAGSETS MAPPINGS TO OLIA ONTOLOGY: GENDER, NUMBER, PERSON, CASE, DEFINITENESS,
     * VERB_FORM, TENSE
     ****************************************************************/

    public static final TagSet<GenderTag> GENDER = new TagSet<GenderTag>("CELI GENDER tags", "da", "de",
            "it", "ro", "ru");
    static {
        GENDER.addTag(new GenderTag("F", Gender.Feminine));
        GENDER.addTag(new GenderTag("FEM", Gender.Feminine));
        GENDER.addTag(new GenderTag("M", Gender.Masculine));
        GENDER.addTag(new GenderTag("MAS", Gender.Masculine));
        GENDER.addTag(new GenderTag("MASC", Gender.Masculine));
        GENDER.addTag(new GenderTag("NE", Gender.Neuter));
        GENDER.addTag(new GenderTag("NEU", Gender.Neuter));
        GENDER.addTag(new GenderTag("UTR", Gender.Common));
        getInstance().addGenderTagset(GENDER);
    }

    public static final TagSet<NumberTag> NUMBER = new TagSet<NumberTag>("CELI NUMBER tags", "da", "de",
            "it", "ro", "ru");
    static {
        NUMBER.addTag(new NumberTag("SGL", NumberFeature.Singular));
        NUMBER.addTag(new NumberTag("SIN", NumberFeature.Singular));
        NUMBER.addTag(new NumberTag("SING", NumberFeature.Singular));
        NUMBER.addTag(new NumberTag("PLU", NumberFeature.Plural));
        getInstance().addNumberTagset(NUMBER);
    }

    //add the person models
    static {
        Map<String,Person> model = new HashMap<String,Person>();
        model.put("FIRST", Person.First);
        model.put("SECOND", Person.Second);
        model.put("THIRD", Person.Third);
        getInstance().addPersonMappings(
            Collections.unmodifiableMap(model), 
            "da", "de","it", "ro", "ru");
    }

    public static final TagSet<CaseTag> CASE = new TagSet<CaseTag>("CELI CASE tags", "da", "de", "it", "ro",
            "ru");
    static {
        CASE.addTag(new CaseTag("NOMORPH", Case.Uninflected));
        CASE.addTag(new CaseTag("NOM", Case.Nominative));
        CASE.addTag(new CaseTag("GEN", Case.Genitive));
        CASE.addTag(new CaseTag("GEN2", Case.Genitive));
        CASE.addTag(new CaseTag("ACC", Case.Accusative));
        CASE.addTag(new CaseTag("ACCAN", Case.Accusative));
        CASE.addTag(new CaseTag("ACCNAN", Case.Accusative));
        CASE.addTag(new CaseTag("ACC2", Case.Accusative));
        CASE.addTag(new CaseTag("DAT", Case.Dative));
        CASE.addTag(new CaseTag("DAT22", Case.Dative));
        CASE.addTag(new CaseTag("INS", Case.Instrumental));
        CASE.addTag(new CaseTag("INS2", Case.Instrumental));
        CASE.addTag(new CaseTag("LOC", Case.Locative));
        getInstance().addCaseTagset(CASE);
    }

    //definitness models
    static {
        Map<String,Definitness> model = new HashMap<String,Definitness>();
        model.put("DEF", Definitness.Definite);
        model.put("INDEF", Definitness.Indefinite);
        getInstance().addDefinitnessTagset(
            Collections.unmodifiableMap(model), 
            "da", "de", "it", "ro", "ru");
    }

    public static final TagSet<VerbMoodTag> VERB_FORM = new TagSet<VerbMoodTag>("CELI VERB FORM tags", "da",
            "de", "it", "ro", "ru");
    static {
        VERB_FORM.addTag(new VerbMoodTag("GERUND", VerbMood.Gerund));
        VERB_FORM.addTag(new VerbMoodTag("GEROUNDPRS", VerbMood.Gerund));//
        VERB_FORM.addTag(new VerbMoodTag("IMPERATIVE", VerbMood.ImperativeVerb));
        VERB_FORM.addTag(new VerbMoodTag("IMP", VerbMood.ImperativeVerb));
        VERB_FORM.addTag(new VerbMoodTag("INDIC", VerbMood.IndicativeVerb));
        VERB_FORM.addTag(new VerbMoodTag("IND", VerbMood.IndicativeVerb));
        VERB_FORM.addTag(new VerbMoodTag("CONGIUNT", VerbMood.SubjunctiveVerb));
        VERB_FORM.addTag(new VerbMoodTag("SUBJ", VerbMood.SubjunctiveVerb));
        VERB_FORM.addTag(new VerbMoodTag("SUB", VerbMood.SubjunctiveVerb));
        VERB_FORM.addTag(new VerbMoodTag("INFIN", VerbMood.Infinitive));
        VERB_FORM.addTag(new VerbMoodTag("INF", VerbMood.Infinitive));
        VERB_FORM.addTag(new VerbMoodTag("PASTPART", VerbMood.Participle));
        VERB_FORM.addTag(new VerbMoodTag("PASPART", VerbMood.Participle));
        VERB_FORM.addTag(new VerbMoodTag("PCPRF", VerbMood.Participle));
        VERB_FORM.addTag(new VerbMoodTag("PRESPART", VerbMood.Participle));
        VERB_FORM.addTag(new VerbMoodTag("PRSPART", VerbMood.Participle));
        VERB_FORM.addTag(new VerbMoodTag("PCPRS", VerbMood.Participle));
        VERB_FORM.addTag(new VerbMoodTag("PART", VerbMood.Participle));
        VERB_FORM.addTag(new VerbMoodTag("PTC", VerbMood.Participle));
        VERB_FORM.addTag(new VerbMoodTag("CONDIZ", VerbMood.ConditionalVerb));
        VERB_FORM.addTag(new VerbMoodTag("SUP", VerbMood.Supine));
        getInstance().addVerbFormTagset(VERB_FORM);
    }

    public static final TagSet<TenseTag> TENSE = new TagSet<TenseTag>("CELI TENSE tags", "da", "de", "it",
            "ro", "ru");
    static {
        TENSE.addTag(new TenseTag("PRS", Tense.Present));
        TENSE.addTag(new TenseTag("PRES", Tense.Present));
        TENSE.addTag(new TenseTag("IMPER", Tense.Imperfect));
        TENSE.addTag(new TenseTag("PER", Tense.Perfect));
        TENSE.addTag(new TenseTag("PASSREM", Tense.RemotePast));
        TENSE.addTag(new TenseTag("PSTPER", Tense.PastPerfect));
        TENSE.addTag(new TenseTag("PST", Tense.Past));
        TENSE.addTag(new TenseTag("FUT", Tense.Future));
        TENSE.addTag(new TenseTag("PCPRF", Tense.Past));
        TENSE.addTag(new TenseTag("PCPRS", Tense.Present));
        TENSE.addTag(new TenseTag("PRT", Tense.Past));
        TENSE.addTag(new TenseTag("PRSFUT", Tense.Present));
        getInstance().addTenseTagset(TENSE);
    }

}