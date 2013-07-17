package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;

/**
 * Provides callbacks form the {@link EntityLinker} about the currently
 * processed Tokens. 
 * @author Rupert Westenthaler
 *
 */
public interface LinkingStateAware {
    /**
     * Callback notifying that the {@link EntityLinker} has completed the
     * linking for the parsed {@link Section} (as {@link Sentence} in case
     * sentence annotations are present in the {@link AnalysedText}).
     * @param sentence the completed section
     */
    void endSection(Section sentence);

    /**
     * Callback notifying that the {@link EntityLinker} has started to link a
     * new section of the text
     * @param sentence the completed section
     */
    void startSection(Section sentence);
    /**
     * The next {@link Token} to be processed by the {@link EntityLinker}
     * @param token the token that will be processed next
     */
    void startToken(Token token);
    /**
     * The next {@link Token} to be processed by the {@link EntityLinker}
     * @param token the token that will be processed next
     */
    void endToken(Token token);

}
