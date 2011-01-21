package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public class TextConstraint extends Constraint {

    public static enum PatternType {
        /**
         * Simple checks if the parsed constraint equals the value
         */
        none,
        /**
         * All kind of REGEX Patterns
         */
        regex,
        /**
         * WildCard based queries using * and ?
         */
        wildcard
        //TODO maybe add Prefix as additional type
    }
    private final PatternType wildcardType;
    private final Set<String> languages;
    private final boolean caseSensitive;
    private final String text;
    /**
     * Creates a TextConstraint for a text and languages.
     * @param text the text or <code>null</code> to search for any text in active languages
     * @param languages the set of active languages.
     */
    public TextConstraint(String text,String...languages) {
        this(text,PatternType.none,false,languages);
    }
    public TextConstraint(String text,boolean caseSensitive,String...languages) {
        this(text,PatternType.none,caseSensitive,languages);
    }
    public TextConstraint(String text,PatternType wildcardType,boolean caseSensitive,String...languages) {
        super(ConstraintType.text);
        if((text == null || text.isEmpty()) && (languages == null || languages.length<1)){
            throw new IllegalArgumentException("Text Constraint MUST define a non empty text OR a non empty list of language constraints");
        }
        this.text = text;
        if(wildcardType == null){
            this.wildcardType = PatternType.none;
        } else {
            this.wildcardType = wildcardType;
        }
        if(languages==null){
            this.languages = Collections.emptySet();
        } else {
            /*
             * Implementation NOTE:
             *   We need to use a LinkedHashSet here to
             *    1) ensure that there are no duplicates and
             *    2) ensure ordering of the parsed constraints
             *   Both is important: Duplicates might result in necessary calculations
             *   and ordering might be important for users that expect that the
             *   language parsed first is used as the preferred one
             */
            this.languages = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(languages)));
        }
        this.caseSensitive = caseSensitive;

    }
    /**
     * @return the wildcardType
     */
    public final PatternType getPatternType() {
        return wildcardType;
    }
    /**
     * @return the languages
     */
    public final Set<String> getLanguages() {
        return languages;
    }
    /**
     * @return the caseSensitive
     */
    public final boolean isCaseSensitive() {
        return caseSensitive;
    }
    /**
     * @return the text
     */
    public final String getText() {
        return text;
    }
    @Override
    public String toString() {
        return String.format("TextConstraint[value=%s|%s|case %sensitive|languages:%s]",
                text,wildcardType.name(),caseSensitive?"":"in",languages);
    }

}
