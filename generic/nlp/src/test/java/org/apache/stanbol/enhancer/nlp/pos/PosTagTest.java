/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
