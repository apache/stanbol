package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.lucene;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.lucene.LuceneLabelTokenizer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

public class TokenizerAndTokenFIlterTest {

    private static final Object TOKENIZER_FACTORY_CLASS = "org.apache.solr.analysis.SmartChineseSentenceTokenizerFactory";
    private static final String[] TOKEN_FILTER_FACTORY_CLASSES = new String[]{
        "org.apache.solr.analysis.SmartChineseWordTokenFilterFactory"
    };
    private static LuceneLabelTokenizer luceneLabelTokenizer;

    @BeforeClass
    public static void init() throws ConfigurationException {
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(LuceneLabelTokenizer.PROPERTY_TOKENIZER_FACTORY, TOKENIZER_FACTORY_CLASS);
        config.put(LuceneLabelTokenizer.PROPERTY_TOKEN_FILTER_FACTORY,TOKEN_FILTER_FACTORY_CLASSES);
        config.put(LabelTokenizer.SUPPORTED_LANUAGES, "zh");
        ComponentContext cc = new MockComponentContext(config);
        luceneLabelTokenizer = new LuceneLabelTokenizer();
        luceneLabelTokenizer.activate(cc);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullLabel(){
        luceneLabelTokenizer.tokenize(null, "zh");
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
        //As I do have no Idea of Chinese those test validate only results I
        //was getting when testing. So this ensures only that the behavioure
        //does not change
        //BBC
        String label = "英国广播公司";
        String[] expected = new String[]{"英国","广播","公司"};
        String[] tokens = luceneLabelTokenizer.tokenize(label, "zh");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
        //Yellow Sea (one word??)
        label = "黄海";
        expected = new String[]{"黄海"};
        tokens = luceneLabelTokenizer.tokenize(label, "zh");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
        //Barack Obama
        label = "贝拉克·奥巴马";
        expected = new String[]{"贝","拉","克","·","奥","巴马"};
        tokens = luceneLabelTokenizer.tokenize(label, "zh");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
    }
    @Test
    public void testEmptyLabel(){
        String[] tokens = luceneLabelTokenizer.tokenize("", "zh");
        Assert.assertNotNull(tokens);
        Assert.assertTrue(tokens.length == 0);
    }
    
    @AfterClass
    public static void close(){
        luceneLabelTokenizer.deactivate(null);
    }
}
