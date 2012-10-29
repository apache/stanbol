package org.apache.stanbol.enhancer.engines.keywordextraction.linking;

import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Manages {@link LabelTokenizer} services. Also provides
 * an {@link #tokenize(String, String)} method that will use
 * the {@link LabelTokenizer} with the highest
 * {@link Constants#SERVICE_RANKING} configured to support
 * labels with the parsed language.
 */
public interface LabelTokenizerManager extends LabelTokenizer{

    /**
     * To be used to get the actual instance for
     * references returned by {@link #getTokenizers(String)}.
     * @param ref the {@link ServiceReference}
     * @return the tokenizer or <code>null</code> if the
     * referenced Service is no longer available
     */
    LabelTokenizer getService(ServiceReference ref);
    /**
     * The sorted (by ranking) list of tokenizers that are able
     * to tokenize labels for the parsed language.
     * @param language the language. <code>null</code>
     * indicated the defautl (unknown) language.
     * @return the list of Tokenizers. An empty list if none.
     */
    List<ServiceReference> getTokenizers(String language);

    /**
     * Tokenizes the parsed label with the {@link LabelTokenizer}
     * having the highest ranking for the parsed language.<p>
     * If no Tokenizer is found this method returns <code>null</code>
     * @param label the label to tokenize
     * @param language the language
     * @return the tokenized label or <code>null</code> if no
     * tokenizer is present.
     */
    String[] tokenize(String label, String language);

}