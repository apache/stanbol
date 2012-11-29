package org.apache.stanbol.enhancer.nlp.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.junit.Test;

import junit.framework.Assert;

public class NIFHelperTest {

    static UriRef base = new UriRef("http://stanbol.apache.org/test/nif/nif-helper");
    static String text = "This is a test for the NLP Interchange format!";
    
    
    @Test
    public void testFragmentURI(){
        Assert.assertEquals(
            new UriRef(base.getUnicodeString()+"#char=23,26"), 
            NIFHelper.getNifFragmentURI(base, 23, 26));
    }
    @Test
    public void testOffsetURI(){
        Assert.assertEquals(
            base.getUnicodeString()+"#offset_23_26", 
            NIFHelper.getNifOffsetURI(base, 23, 26).getUnicodeString());
    }
    @Test
    public void testHashURI() throws IOException {
        String selected = text.substring(23,26);
        String context = text.substring(13,23)+'('+selected+')'+text.substring(26,36);
        byte[] contextData = context.getBytes(Charset.forName("UTF8"));
        String md5 = ContentItemHelper.streamDigest(new ByteArrayInputStream(contextData), null, "MD5");
        UriRef expected = new UriRef(base.getUnicodeString()+"#hash_10_3_"+md5+"_NLP");
        Assert.assertEquals(expected, NIFHelper.getNifHashURI(base, 23, 26, text));
    }    
}
