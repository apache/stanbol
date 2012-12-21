package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.opennlp;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

public class OpenNlpLabelTokenizerTest {
    
    private static OpenNlpLabelTokenizer tokenizer;
    private String label = "This is only a Test";
    private String[] expected = label.split(" ");
    
    @BeforeClass
    public static final void init() throws ConfigurationException {
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(LabelTokenizer.SUPPORTED_LANUAGES, "*");
        ComponentContext cc = new MockComponentContext(config);
        tokenizer = new OpenNlpLabelTokenizer();
        tokenizer.openNlp = new OpenNLP(new ClasspathDataFileProvider(null));
        tokenizer.activate(cc);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullLabel(){
        tokenizer.tokenize(null, "en");
    }
    @Test
    public void testNullLanguate(){
        String[] tokens = tokenizer.tokenize(label, null);
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
    }
    @Test
    public void testTokenizer(){
        String[] tokens = tokenizer.tokenize(label, "en");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
    }
    @Test
    public void testEmptyLabel(){
        String[] tokens = tokenizer.tokenize("", "en");
        Assert.assertNotNull(tokens);
        Assert.assertTrue(tokens.length == 0);
    }
    
    @AfterClass
    public static void close(){
        tokenizer.deactivate(null);
    }
}
