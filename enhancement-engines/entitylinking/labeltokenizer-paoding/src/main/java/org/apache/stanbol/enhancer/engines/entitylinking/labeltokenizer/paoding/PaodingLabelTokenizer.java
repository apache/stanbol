package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.paoding;

import java.io.IOException;
import java.io.StringReader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import net.paoding.analysis.analyzer.PaodingAnalyzer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Component(
    configurationFactory=true,
    policy=ConfigurationPolicy.OPTIONAL,
    metatype=true)
@Properties(value={
        @Property(name=Constants.SERVICE_RANKING,intValue=200) //the smartcn one uses 100
})
public class PaodingLabelTokenizer implements LabelTokenizer {

    private Logger log = LoggerFactory.getLogger(PaodingAnalyzer.class);
    
    private static final String[] EMPTY = new String[]{};
    
    @Activate
    protected void activate(ComponentContext ctx){
        log.info(" ... activating {}",PaodingLabelTokenizer.class);
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        log.info(" ... deactivating {}",PaodingLabelTokenizer.class);
    }
    
    @Override
    public String[] tokenize(String label, String language) {
        if(label == null){
            throw new IllegalArgumentException("The parsed Label MUST NOT be NULL!");
        }
        if("zh".equals(language) || (language != null && language.length() > 4 && 
                language.charAt(2) == '-' && language.startsWith("zh"))){
            if(label.isEmpty()){
                return EMPTY; 
            }
            PaodingAnalyzer pa;
            try {
                pa = AccessController.doPrivileged(new PrivilegedExceptionAction<PaodingAnalyzer>() {
                    public PaodingAnalyzer run() throws Exception {
                        return new PaodingAnalyzer();
                    }
                });
            } catch (PrivilegedActionException pae){
                Exception e = pae.getException();
                log.error("Unable to initialise PoadingAnalyzer",e);
                return null;
            }
            TokenStream ts = pa.tokenStream("dummy", new StringReader(label));
            List<String> tokens = new ArrayList<String>(8);
            int lastEnd = 0;
            try {
                while(ts.incrementToken()){
                    OffsetAttribute offset = ts.addAttribute(OffsetAttribute.class);
                    //when tokenizing labels we need to preserve all chars
                    if(offset.startOffset() > lastEnd){ //add token for stopword
                        tokens.add(label.substring(lastEnd,offset.startOffset()));
                    }
                    tokens.add(label.substring(offset.startOffset(), offset.endOffset()));
                    lastEnd = offset.endOffset();
                }
                return tokens.toArray(new String[tokens.size()]);            
            } catch (IOException e) {
                log.warn("IOException while tokenizing label '"+label+"'",e);
                return null;
            }
        } else {
            return null;
        }
    }

}
