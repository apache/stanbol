package org.apache.stanbol.enhancer.nlp.dependency;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.UriRef;
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
    private static Entry<UriRef,Blob> textBlob;

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
