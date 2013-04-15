package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import org.junit.Assert;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;

public class LanguageLiteralFilterTest {

    @Test(expected=IllegalArgumentException.class)
    public void testIncludeExcludeConfig1(){
        new LiteralLanguageFilter("en,de,!de");
    }
    @Test(expected=IllegalArgumentException.class)
    public void testIncludeExcludeConfig2(){
        new LiteralLanguageFilter("en,!de,de");
    }
    @Test
    public void testDataTypes(){
        RdfImportFilter filter = new LiteralLanguageFilter("en,de");
        
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "en", false)));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test")));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("10",XSDDatatype.XSDint)));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createAnon()));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createURI("urn:test")));
    }
    @Test
    public void testIncludeTest(){
        RdfImportFilter filter = new LiteralLanguageFilter("en,de");
        
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "en", false)));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "de", false)));
        Assert.assertFalse(filter.accept(null, null, 
            Node.createLiteral("test", "fr", false)));
    }
    @Test
    public void testExcludeTest(){
        RdfImportFilter filter = new LiteralLanguageFilter("*,en,!de");
        
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "en", false)));
        Assert.assertFalse(filter.accept(null, null, 
            Node.createLiteral("test", "de", false)));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "fr", false)));
    }
    
}
