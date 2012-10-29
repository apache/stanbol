package org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.LabelTokenizer;
import org.osgi.framework.Constants;

/**
 * Implementation of a LabelTokenizer based on OpenNLP.
 * Note that {@link OpenNLP} is optionally. If this service
 * is not available that the {@link SimpleTokenizer} is
 * used to tokenize parsed labels.<p>
 * As this implemtnation does not set the {@link LabelTokenizer#SUPPORTED_LANUAGES}
 * property it register itself for any language with a {@link Constants#SERVICE_RANKING}
 * of '<code>-1000</code>'
 * <p>
 * <b>NOTE</b> that this implementation introduces a dependency to the
 * Stanbol OpenNLP commons bundle.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true)
@Service
@Properties(value={@Property(name=Constants.SERVICE_RANKING,intValue=-1000)})
public class OpenNlpLabelTokenizer implements LabelTokenizer {

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    private OpenNLP openNlp;

    public OpenNlpLabelTokenizer() {
    }

    public OpenNlpLabelTokenizer(OpenNLP openNlp){
        this.openNlp = openNlp;
    }
    
    @Override
    public String[] tokenize(String label, String language) {
        Tokenizer tokenizer;
        if(openNlp != null){
            tokenizer = openNlp.getTokenizer(language);
        } else {
            tokenizer = SimpleTokenizer.INSTANCE;
        }
        return tokenizer.tokenize(label);
    }

}
