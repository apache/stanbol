package org.apache.stanbol.enhancer.engines.entityhublinking.labeltokenizer.lucene;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

public class LuceneLabelTokenizerTest {

    
    private static final Object TOKENIZER_FACTORY_CLASS = "org.apache.solr.analysis.WhitespaceTokenizerFactory";
    private static LuceneLabelTokenizer luceneLabelTokenizer;

    @BeforeClass
    public static void init() throws ConfigurationException {
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(LuceneLabelTokenizer.PROPERTY_TOKENIZER_FACTORY, TOKENIZER_FACTORY_CLASS);
        config.put(LabelTokenizer.SUPPORTED_LANUAGES, "en");
        ComponentContext cc = new MockComponentContext(config);
        luceneLabelTokenizer = new LuceneLabelTokenizer();
        luceneLabelTokenizer.activate(cc);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullLabel(){
        luceneLabelTokenizer.tokenize(null, "en");
    }
    @Test
    public void testNullLanguate(){
        Assert.assertNull(luceneLabelTokenizer.tokenize("test", null));
    }
    @Test
    public void testUnsupportedLanguage(){
        Assert.assertNull(luceneLabelTokenizer.tokenize("test", "de"));
    }
    @Test
    public void testLuceneLabelTokenizer(){
        String label = "This is only a Test";
        String[] expected = label.split(" ");
        String[] tokens = luceneLabelTokenizer.tokenize(label, "en");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
    }
    @Test
    public void testEmptyLabel(){
        String[] tokens = luceneLabelTokenizer.tokenize("", "en");
        Assert.assertNotNull(tokens);
        Assert.assertTrue(tokens.length == 0);
    }
    
    @AfterClass
    public static void close(){
        luceneLabelTokenizer.deactivate(null);
    }
}
