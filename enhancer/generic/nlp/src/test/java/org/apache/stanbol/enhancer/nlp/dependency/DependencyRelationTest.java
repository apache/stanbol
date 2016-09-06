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

package org.apache.stanbol.enhancer.nlp.dependency;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.Assert;

public class DependencyRelationTest {
    /**
     * Empty AnalysedText instance created before each test
     */
    private static AnalysedText at;

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static final AnalysedTextFactory atFactory = AnalysedTextFactory.getDefaultInstance();

    private static ContentItem ci;
    private static Entry<IRI,Blob> textBlob;

    @BeforeClass
    public static void setup() throws IOException {
        ci = ciFactory.createContentItem(new StringSource(""));
        textBlob = ContentItemHelper.getBlob(ci, Collections.singleton("text/plain"));
        at = atFactory.createAnalysedText(textBlob.getValue());
    }

    @Test
    public void testSimpleGrammaticalRelationInit() {
        GrammaticalRelationTag gramRelationTag = new GrammaticalRelationTag(
            "agent", GrammaticalRelation.Agent);
        DependencyRelation depRelation = new DependencyRelation(gramRelationTag, false, at.addToken(0, 0));

        Assert.assertEquals("agent", depRelation.getGrammaticalRelationTag().getTag());

        GrammaticalRelation relation = depRelation.getGrammaticalRelationTag().getGrammaticalRelation();
        Assert.assertEquals(GrammaticalRelation.Agent, relation);
        Assert.assertEquals(null, relation.getParent());
        Assert.assertEquals(GrammaticalRelationCategory.Argument, relation.getCategory());
    }

    @Test
    public void testGrammaticalRelationWithHierarchyInit() {
        GrammaticalRelationTag gramRelationTag = new GrammaticalRelationTag(
            "abbrev", GrammaticalRelation.AbbreviationModifier);
        DependencyRelation depRelation = new DependencyRelation(gramRelationTag, false, at.addToken(0, 0));

        Assert.assertEquals("abbrev", depRelation.getGrammaticalRelationTag().getTag());

        GrammaticalRelation relation = depRelation.getGrammaticalRelationTag().getGrammaticalRelation();
        Assert.assertEquals(GrammaticalRelation.AbbreviationModifier, relation);
        Assert.assertEquals(GrammaticalRelation.Modifier, relation.getParent());

        Set<GrammaticalRelation> expectedHierarcy = EnumSet.of(GrammaticalRelation.Dependent,
            GrammaticalRelation.Modifier, GrammaticalRelation.AbbreviationModifier);

        Set<GrammaticalRelation> hierarchy = relation.hierarchy();
        Assert.assertEquals(expectedHierarcy, hierarchy);
    }
}
