package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSimpleLabelTokenizer {

    
    private static SimpleLabelTokenizer tokenizer;
    private String label = "This is only a Test";
    private String[] expected = label.split(" ");

    @BeforeClass
    public static final void init(){
        tokenizer = new SimpleLabelTokenizer();
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
}
