package org.apache.stanbol.enhancer.engines.entitylinking.config;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.commons.opennlp.TextAnalyzer;
import org.apache.stanbol.enhancer.engines.entitylinking.engine.EntityLinkingEngine;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextProcessingConfig {

    private static final Logger log = LoggerFactory.getLogger(TextProcessingConfig.class);
    /**
     * If enabled only {@link Pos#ProperNoun}, {@link Pos#Foreign} and {@link Pos#Acronym} are Matched. If
     * deactivated all Tokens with the category {@link LexicalCategory#Noun} and 
     * {@link LexicalCategory#Residual} are considered for matching.<p>
     * This property allows an easy configuration of the matching that is sufficient for most usage scenarios.
     * Users that need to have more control can configure language specific mappings by using
     * {@link #PARAM_LEXICAL_CATEGORIES}, {@link #PARAM_POS_TYPES}, {@link #PARAM_POS_TAG} and
     * {@link #PARAM_POS_PROBABILITY} in combination with the {@link #PROCESSED_LANGUAGES}
     * configuration.<p>
     * The {@link #DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE default} if this is <code>false</code>
     */
    public static final String PROCESS_ONLY_PROPER_NOUNS_STATE = "enhancer.engines.linking.properNounsState";
    /**
     * Default for the {@link #PROCESS_ONLY_PROPER_NOUNS_STATE} (false)
     */
    public static final boolean DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE = false;
    /**
     * Allows to configure the processed languages by using the syntax supported by {@link LanguageConfiguration}.
     * In addition this engine supports language specific configurations for matched {@link LexicalCategory}
     * {@link Pos} and String POS tags as well as Pos annotation probabilities by using the parameters
     * {@link #PARAM_LEXICAL_CATEGORIES}, {@link #PARAM_POS_TYPES}, {@link #PARAM_POS_TAG} and
     * {@link #PARAM_POS_PROBABILITY}.<p>
     * See the documentation of {@link LanguageConfiguration} for details of the Syntax.
     */
    public static final String PROCESSED_LANGUAGES = "enhancer.engines.linking.processedLanguages";
    /*
     * Parameters used for language specific text processing configurations
     */
    // (1) PHRASE level
    /**
     * Allows to configure the processed Chunk type (the default is
     * <code>cc={@link LexicalCategory#Noun Noun}</code> to process only
     * Noun Phrases). If set to <code>cc</code> (empty value) processing
     * of chunks is deactivated.
     */
    public static final String PARAM_PHRASE_CATEGORIES = "pc";
    public static final String PARAM_PHRASE_TAG = "ptag";
    public static final String PARAM_PHRASE_PROBABILITY = "pprob";
    public static final String PARAM_LINK_MULTI_MATCHABLE_TOKEN_IN_PHRASE = "lmmtip";
    //(2) TOKEN level
    public static final String PARAM_LEXICAL_CATEGORIES = "lc";
    public static final String PARAM_POS_TYPES = "pos";
    public static final String PARAM_POS_TAG = "tag";
    public static final String PARAM_POS_PROBABILITY = "prob";
    /**
     * Parameter used to configure how to deal with upper case tokens
     */
    public static final String PARAM_UPPER_CASE = "uc";
    /**
     * Enumeration defining valued for the {@link EntityLinkingEngine#PARAM_UPPER_CASE} parameter
     */
    public static enum UPPER_CASE_MODE {NONE,MATCH,LINK};
    /**
     * The default state to dereference entities set to <code>true</code>.
     */
    public static final boolean DEFAULT_DEREFERENCE_ENTITIES_STATE = true;
    /**
     * Default set of languages. This is an empty set indicating that texts in any
     * language are processed. 
     */
    public static final Set<String> DEFAULT_LANGUAGES = Collections.emptySet();
    public static final double DEFAULT_MIN_POS_TAG_PROBABILITY = 0.6667;
    
    /**
     * The languages this engine is configured to enhance. An empty List is
     * considered as active for any language
     */
    private LanguageConfiguration languages = new LanguageConfiguration(PROCESSED_LANGUAGES, new String[]{"*"});

    private LanguageProcessingConfig defaultConfig;
    private Map<String,LanguageProcessingConfig> languageConfigs = new HashMap<String,LanguageProcessingConfig>();

    public TextProcessingConfig(){
        this.defaultConfig = new LanguageProcessingConfig();
    }
    
    public LanguageProcessingConfig getDefaults(){
        return defaultConfig;
    }
    /**
     * Getter for the language specific configuration.
     * @param language
     * @return the configuration sepcific to the parsed language or <code>null</code>
     * if none.
     */
    public LanguageProcessingConfig getLanguageSpecificConfig(String language){
        return languageConfigs.get(language);
    }
    /**
     * Creates a language specific configuration by copying the currently configured
     * defaults.
     * @param language the language
     * @return the specific configuration
     * @throws IllegalStateException if a language specific configuration for the
     * parsed language already exists.
     */
    public LanguageProcessingConfig createLanguageSpecificConfig(String language){
        if(languageConfigs.containsKey(language)){
            throw new IllegalStateException("A specific configuration for the language '"
                +language+ "' does already exist!");
        }
        LanguageProcessingConfig conf = defaultConfig.clone();
        languageConfigs.put(language, conf);
        return conf;
    }
    /**
     * Removes the language specific configuration for the parsed language
     * @param language the language
     * @return the removed configuration
     */
    public LanguageProcessingConfig removeLanguageSpecificConfig(String language){
        return languageConfigs.remove(language);
    }
    
    /**
     * The {@link LanguageProcessingConfig} for the parsed language
     * or <code>null</code> if the language is not included in the
     * configuration. This will return the {@link #getDefaults()} if
     * the parsed language does not have a specific configuration.<p>
     * To obtain just language specific configuration use
     * {@link #getLanguageSpecificConfig(String)}
     * @param language the language
     * @return the configuration or <code>null</code> if the language is
     * not configured to be processed.
     */
    public LanguageProcessingConfig getConfiguration(String language) {
        if(languages.isLanguage(language)){
            LanguageProcessingConfig lpc = languageConfigs.get(language);
            return lpc == null ? defaultConfig : lpc;
        } else {
            return null;
        }
    }
    
    
    /**
     * Initialise the {@link TextAnalyzer} component.<p>
     * Currently this includes the following configurations: <ul>
     * <li>{@link #PROCESSED_LANGUAGES}: If no configuration is present the
     * default (process all languages) is used.
     * <li> {@value #MIN_POS_TAG_PROBABILITY}: If no configuration is
     * present the #DEFAULT_MIN_POS_TAG_PROBABILITY is used
     * languages based on the value of the
     * 
     * @param configuration the OSGI component configuration
     */
    public final static TextProcessingConfig createInstance(Dictionary<String,Object> configuration) throws ConfigurationException {
        TextProcessingConfig tpc = new TextProcessingConfig();
        //Parse the default text processing configuration
        //set the default LexicalTypes
        Object value = configuration.get(PROCESS_ONLY_PROPER_NOUNS_STATE);
        boolean properNounState;
        if(value instanceof Boolean){
            properNounState = ((Boolean)value).booleanValue();
        } else if (value != null){
            properNounState = Boolean.parseBoolean(value.toString());
        } else {
            properNounState = DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE;
        }
        if(properNounState){
            tpc.defaultConfig.setLinkedLexicalCategories(Collections.EMPTY_SET);
            tpc.defaultConfig.setLinkedPos(LanguageProcessingConfig.DEFAULT_LINKED_POS);
            log.debug("> ProperNoun matching activated (matched Pos: {})",
                tpc.defaultConfig.getLinkedPos());
        } else {
            tpc.defaultConfig.setLinkedLexicalCategories(LanguageProcessingConfig.DEFAULT_LINKED_LEXICAL_CATEGORIES);
            tpc.defaultConfig.setLinkedPos(Collections.EMPTY_SET);
            log.debug("> Noun matching activated (matched LexicalCategories: {})",
                tpc.defaultConfig.getLinkedLexicalCategories());
        }
        //parse the language configuration
        value = configuration.get(PROCESSED_LANGUAGES);
        if(value instanceof String){
            throw new ConfigurationException(PROCESSED_LANGUAGES, "Comma separated String "
                + "is not supported for configurung the processed languages for the because "
                + "the comma is used as separator for values of the parameters '"
                + PARAM_LEXICAL_CATEGORIES+"', '"+ PARAM_POS_TYPES+"'and'"+PARAM_POS_TAG
                + "! Users need to use String[] or Collection<?> instead!");
        }
        tpc.languages.setConfiguration(configuration);
        Map<String,String> defaultConfig = tpc.languages.getDefaultParameters();
        //apply the default parameters (parameter set for the '*' or '' (empty) language
        if(!defaultConfig.isEmpty()){
            applyLanguageParameter(tpc.defaultConfig,null,defaultConfig);
        }
        //apply language specific configurations
        for(String lang : tpc.languages.getExplicitlyIncluded()){
            LanguageProcessingConfig lpc = tpc.defaultConfig.clone();
            applyLanguageParameter(lpc, lang, tpc.languages.getParameters(lang));
            tpc.languageConfigs.put(lang, lpc);
        }
        return tpc;
    }

    private static void applyLanguageParameter(LanguageProcessingConfig tpc, String language, Map<String,String> config) throws ConfigurationException {
        log.info(" > parse language Configuration for language: {}",
            language == null ? "default":language);
        //parse Phrase level configuration
        Set<LexicalCategory> chunkCats = parseEnumParam(config, PROCESSED_LANGUAGES, language, PARAM_PHRASE_CATEGORIES, LexicalCategory.class);
        Set<String> chunkTags = parseStringTags(config.get(PARAM_PHRASE_TAG));
        if(chunkCats.isEmpty() && config.containsKey(PARAM_PHRASE_CATEGORIES) &&
                chunkTags.isEmpty()){
            log.info("   + enable ignorePhrase");
            tpc.setIgnoreChunksState(true);
            tpc.setProcessedPhraseCategories(Collections.EMPTY_SET);
        } else {
            tpc.setIgnoreChunksState(false);
            if(!chunkCats.isEmpty()){
                log.info("   + set processable Phrase cat {}",chunkCats);
                tpc.setProcessedPhraseCategories(chunkCats);
            } else {
                log.info("   - use processable Phrase cats {}",tpc.getProcessedPhraseCategories());
            }
            if(!chunkTags.isEmpty()) {
                log.info("   + set processable Phrase tags {}",chunkTags);
                tpc.setProcessedPhraseTags(chunkTags);
            } else {
                log.info("   - use processable Phrase tags {}",tpc.getProcessedPhraseTags());
            }
        }
        Double chunkProb = parseNumber(config, PROCESSED_LANGUAGES, language, PARAM_PHRASE_PROBABILITY, Double.class);
        if(chunkProb != null || //if explicitly set
                config.containsKey(PARAM_PHRASE_PROBABILITY)){ //set to empty value (set default)
            log.info("   + set min ChunkTag probability: {}", chunkProb == null ? "default" : chunkProb);
            tpc.setMinPhraseAnnotationProbability(chunkProb);
            tpc.setMinExcludePhraseAnnotationProbability(chunkProb == null ? null : chunkProb/2);
        } else {
            log.info("   - use min PhraseTag probability: {}",tpc.getMinPhraseAnnotationProbability());
        }
        //link multiple matchable Tokens within Chunks
        Boolean lmmticState = parseState(config, PARAM_LINK_MULTI_MATCHABLE_TOKEN_IN_PHRASE);
        if(lmmticState != null){
            log.info("   + set the link multi matchable tokens in Phrase state to : {}",lmmticState);
            tpc.setLinkMultiMatchableTokensInChunkState(lmmticState);
        } else {
            log.info("   - use the link multi matchable tokens in Phrase state to : {}",tpc.isLinkMultiMatchableTokensInChunk());
        }
        
        //parse Token level configuration
        Set<LexicalCategory> lexCats = parseEnumParam(config, PROCESSED_LANGUAGES, language, PARAM_LEXICAL_CATEGORIES, LexicalCategory.class);
        Set<Pos> pos = parseEnumParam(config, PROCESSED_LANGUAGES, language,PARAM_POS_TYPES, Pos.class);
        Set<String> tags = parseStringTags(config.get(PARAM_POS_TAG));
        if(config.containsKey(PARAM_LEXICAL_CATEGORIES) ||
                config.containsKey(PARAM_POS_TYPES) ||
                config.containsKey(PARAM_POS_TAG)){
            log.info("   + set Linkable Tokens: cat: {}, pos: {}, tags {}",
                new Object[]{lexCats,pos,tags});
            tpc.setLinkedLexicalCategories(lexCats);
            tpc.setLinkedPos(pos);
            tpc.setLinkedPosTags(tags);
        } else {
            log.info("   - use Linkable Tokens: cat: {}, pos: {}, tags {}",
                new Object[]{tpc.getLinkedLexicalCategories(),
                             tpc.getLinkedPos(),
                             tpc.getLinkedPos()});
        }
        //min POS tag probability
        Double prob = parseNumber(config,PROCESSED_LANGUAGES,language, PARAM_POS_PROBABILITY,Double.class);
        if(prob != null || //explicitly set
                config.containsKey(PARAM_POS_PROBABILITY)){ //set to empty value (set default)
            log.info("   + set minimum POS tag probability: {}", prob == null ? "default" : prob);
            tpc.setMinPosAnnotationProbability(prob);
            tpc.setMinExcludePosAnnotationProbability(prob == null ? null : prob/2d);
        } else {
            log.info("   - use minimum POS tag probability: {}", tpc.getMinPosAnnotationProbability());
        }
        //parse upper case
        Set<UPPER_CASE_MODE> ucMode = parseEnumParam(config, PROCESSED_LANGUAGES,language,PARAM_UPPER_CASE,UPPER_CASE_MODE.class);
        if(ucMode.size() > 1){
            throw new ConfigurationException(PROCESSED_LANGUAGES, "Parameter 'uc' (Upper case mode) MUST NOT be multi valued (langauge: "
                +(language == null ? "default":language)+", parsed value='"+config.get(PARAM_UPPER_CASE)+"')!");
        }
        if(!ucMode.isEmpty()){
            UPPER_CASE_MODE mode = ucMode.iterator().next();
            log.info("   + set upper case token mode to {}", mode);
            switch (mode) {
                case NONE:
                    tpc.setMatchUpperCaseTokensState(false);
                    tpc.setLinkUpperCaseTokensState(false);
                    break;
                case MATCH:
                    tpc.setMatchUpperCaseTokensState(true);
                    tpc.setLinkUpperCaseTokensState(false);
                    break;
                case LINK:
                    tpc.setMatchUpperCaseTokensState(true);
                    tpc.setLinkUpperCaseTokensState(true);
                    break;
                default:
                    log.warn("Unsupported {} entry {} -> set defaults",UPPER_CASE_MODE.class.getSimpleName(),mode);
                    tpc.setMatchUpperCaseTokensState(null);
                    tpc.setLinkUpperCaseTokensState(null);
                    break;
            }
        } else {
            log.info("   - use upper case token mode: match={}, link={}", tpc.isMatchUpperCaseTokens(), tpc.isLinkUpperCaseTokens());
        }
    }

    private static Boolean parseState(Map<String,String> config, String param){
        String value = config.get(param);
        return value == null && config.containsKey(param) ? Boolean.TRUE :
            value != null ? new Boolean(value) : null;
    }
    
    private static <T extends Number> T parseNumber(Map<String,String> config, 
            String property, String language, String param, Class<T> clazz) throws ConfigurationException {
        String paramVal = config.get(PARAM_POS_PROBABILITY);
        if(paramVal != null && !paramVal.trim().isEmpty()){
            try {
                //all Number subclasses do have a String constructor!
                return clazz.getConstructor(String.class).newInstance(paramVal.trim());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(property, "Unable to parse "
                    + clazz.getSimpleName()+" from Parameter '"
                    + PARAM_POS_PROBABILITY+"="+paramVal.trim()
                    + "' from the "+(language == null ? "default" : language)
                    + " language configuration", e);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unable to create new "+clazz.getSimpleName()
                    +"("+paramVal.trim()+"::String)",e);
            } catch (SecurityException e) {
                throw new IllegalStateException("Unable to create new "+clazz.getSimpleName()
                    +"("+paramVal.trim()+"::String)",e);
            } catch (InstantiationException e) {
                throw new IllegalStateException("Unable to create new "+clazz.getSimpleName()
                    +"("+paramVal.trim()+"::String)",e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to create new "+clazz.getSimpleName()
                    +"("+paramVal.trim()+"::String)",e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Unable to create new "+clazz.getSimpleName()
                    +"("+paramVal.trim()+"::String)",e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Unable to create new "+clazz.getSimpleName()
                    +"("+paramVal.trim()+"::String)",e);
            }
        }
        return null;
    }
    
    private static Set<String> parseStringTags(String value) {
        if(value == null || value.isEmpty()){
            return Collections.emptySet();
        } else {
            Set<String> tags = new HashSet<String>();
            for(String entry : value.split(",")){
                entry = entry.trim();
                if(!entry.isEmpty()){
                    tags.add(entry);
                }
            }
            return tags;
        }
    }

    /**
     * Utility to parse Enum members out of a comma separated string
     * @param config the config
     * @param property the property (only used for error handling)
     * @param param the key of the config used to obtain the config
     * @param enumClass the {@link Enum} class
     * @return the configured members of the Enum or an empty set if none 
     * @throws ConfigurationException if a configured value was not part of the enum
     */
    private static <T extends Enum<T>> Set<T> parseEnumParam(Map<String,String> config,
        String property, String language, //params used for logging
        String param,Class<T> enumClass) throws ConfigurationException {
        Set<T> enumSet;
        String val = config.get(param);
        if(val == null){
            enumSet = Collections.emptySet();
        } else {
            enumSet = EnumSet.noneOf(enumClass);
            for(String entry : val.split(",")){
                entry = entry.trim();
                if(!entry.isEmpty()){
                    try {
                        enumSet.add(Enum.valueOf(enumClass,entry.toString()));
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException(property, 
                            "'"+entry +"' of param '"+param+"' for language '"
                            + (language == null ? "default" : language)
                            + "'is not a member of the enum "+ enumClass.getSimpleName()
                            + "(configured : '"+val+"')!" ,e);
                    }
                }
            }
        }
        return enumSet;
    }
    
}
