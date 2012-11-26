package org.apache.stanbol.enhancer.nlp.pos;

import java.util.EnumSet;

import junit.framework.Assert;

import org.junit.Test;

public class PosTagTest {

    
    @Test
    public void testLexicalCategoryInit(){
        PosTag tag = new PosTag("test", LexicalCategory.Noun);
        Assert.assertEquals("test", tag.getTag());
        Assert.assertEquals(EnumSet.of(LexicalCategory.Noun), tag.getCategories());
        Assert.assertTrue(tag.getPos().isEmpty());
    }
    @Test
    public void testSinglePosInit(){
        PosTag tag = new PosTag("test", Pos.ProperNoun);
        Assert.assertEquals("test", tag.getTag());
        Assert.assertEquals(EnumSet.of(LexicalCategory.Noun), tag.getCategories());
        Assert.assertEquals(EnumSet.of(Pos.ProperNoun), tag.getPos());
        Assert.assertEquals(EnumSet.of(Pos.ProperNoun), tag.getPosHierarchy());
    }
    @Test
    public void testMultiplePosInit(){
        PosTag tag = new PosTag("test", Pos.ProperNoun,Pos.PluralQuantifier);
        Assert.assertEquals("test", tag.getTag());
        Assert.assertEquals(EnumSet.of(LexicalCategory.Noun,LexicalCategory.Quantifier), tag.getCategories());
        Assert.assertEquals(EnumSet.of(Pos.ProperNoun,Pos.PluralQuantifier), tag.getPos());
        Assert.assertEquals(EnumSet.of(Pos.ProperNoun,Pos.PluralQuantifier), tag.getPosHierarchy());
    }
    @Test
    public void testLexCatAndMultiplePosInit(){
        PosTag tag = new PosTag("test", LexicalCategory.Noun,Pos.ProperNoun,Pos.PluralQuantifier);
        Assert.assertEquals("test", tag.getTag());
        Assert.assertEquals(EnumSet.of(LexicalCategory.Noun,LexicalCategory.Quantifier), tag.getCategories());
        Assert.assertEquals(EnumSet.of(Pos.ProperNoun,Pos.PluralQuantifier), tag.getPos());
        Assert.assertEquals(EnumSet.of(Pos.ProperNoun,Pos.PluralQuantifier), tag.getPosHierarchy());
    }
    @Test
    public void testPosWithHierarchyInit(){
        PosTag tag = new PosTag("test", Pos.Gerund);
        Assert.assertEquals("test", tag.getTag());
        Assert.assertEquals(EnumSet.of(LexicalCategory.Verb), tag.getCategories());
        Assert.assertEquals(EnumSet.of(Pos.Gerund), tag.getPos());
        Assert.assertEquals(EnumSet.of(Pos.NonFiniteVerb,Pos.Gerund), tag.getPosHierarchy());
    }
   
    
}
