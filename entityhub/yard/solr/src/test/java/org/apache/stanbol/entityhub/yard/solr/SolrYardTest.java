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
package org.apache.stanbol.entityhub.yard.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.SolrServerAdapter;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.managed.standalone.StandaloneEmbeddedSolrServerProvider;
import org.apache.stanbol.commons.solr.managed.standalone.StandaloneManagedSolrServer;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.SimilarityConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.test.yard.YardTest;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test uses the system property "basedir" to configure an embedded Solr Server. This property is set by
 * the mvn surefire plugin. When using this Unit Test within a build environment that does not set this
 * property one need to set it manually to the base directory of this module.
 * <p>
 * 
 * @author Rupert Westenthaler
 * 
 */
public class SolrYardTest extends YardTest {
    /**
     * The SolrYard used for the tests
     */
    private static Yard yard;
    /**
     * The SolrDirectoryManager also tested within this unit test
     */
    public static final String TEST_YARD_ID = "testYard";
    public static final String TEST_SOLR_CORE_NAME = "test";
    protected static final String TEST_INDEX_REL_PATH = File.separatorChar + "target" + File.separatorChar
                                                        + ManagedSolrServer.DEFAULT_SOLR_DATA_DIR;
    private static final Logger log = LoggerFactory.getLogger(SolrYardTest.class);
    
    private static StandaloneEmbeddedSolrServerProvider solrServerProvider;

    @BeforeClass
    public static final void initYard() throws YardException, IOException {
        // get the working directory
        // use property substitution to test this feature!
        String prefix = System.getProperty("basedir") == null ? "." : "${basedir}";
        String solrServerDir = prefix + TEST_INDEX_REL_PATH;
        log.info("Test Solr Server Directory: " + solrServerDir);
        SolrYardConfig config = new SolrYardConfig(TEST_YARD_ID, TEST_SOLR_CORE_NAME);
        config.setName("Solr Yard Test");
        config.setDescription("The Solr Yard instance used to execute the Unit Tests defined for the Yard Interface");
        config.setAllowInitialisation(true);
        //for unit testing we want immidiate commits (required after STANBOL-1092
        // as the default changed to false)
        config.setImmediateCommit(true); 
        //init the ManagedSolrServer used for the UnitTest
        System.setProperty(ManagedSolrServer.MANAGED_SOLR_DIR_PROPERTY, solrServerDir);
        IndexReference solrServerRef = IndexReference.parse(config.getSolrServerLocation());
        solrServerProvider = StandaloneEmbeddedSolrServerProvider.getInstance();
        SolrServer server = solrServerProvider.getSolrServer(solrServerRef,
            config.isAllowInitialisation() ? config.getIndexConfigurationName() : null);
        //Optional support for the nsPrefix service
        final NamespacePrefixService nsPrefixService;
        ServiceLoader<NamespacePrefixService> spsl = ServiceLoader.load(NamespacePrefixService.class);
        Iterator<NamespacePrefixService> it = spsl.iterator();
        if(it.hasNext()){
            nsPrefixService = it.next();
        } else {
            nsPrefixService = null;
        }
        yard = new SolrYard(server, config, nsPrefixService);
    }

    @Override
    protected Yard getYard() {
        return yard;
    }

    /*
     * Three unit tests that check that SolrYardConfig does throw IllegalArgumentExceptions when parsing an
     * illegal parameters.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullParams() {
        new SolrYardConfig(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullUrl() {
        new SolrYardConfig(TEST_YARD_ID, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullID() {
        new SolrYardConfig(null, TEST_SOLR_CORE_NAME);
    }
    /**
     * Additional test for <a href="https://issues.apache.org/jira/browse/STANBOL-1065">
     * STANBOL-1065</a>
     * @throws YardException
     */
    @Test
    public void testUriWithSpaces() throws YardException {
        Yard yard = getYard();
        String id1 = "http://www.example.com/with space";
        String id2 = "http://www.example.com/other";
        Representation test1 = create(id1,true);
        Representation test2 = create(id2,true);
        //now add a label containing space to id2
        test1.addNaturalText(NamespaceEnum.rdfs+"label","expected result","en");
        test2.addNaturalText(NamespaceEnum.rdfs+"label","space","en");
        test2.addNaturalText(NamespaceEnum.rdfs+"comment","URIs with space got separated "
            + "in queries causing parts in URIs after spaces to form full text " 
            + "queries instead!","en");
        yard.update(test1);
        yard.update(test2);
        //now try to query for some combination
        assertNull("No Entity with ID 'http://www.example.com/with URIs' expected",
            yard.getRepresentation("http://www.example.com/with URIs"));
        assertNull("No Entity with ID 'http://www.example.com/with' expected",
            yard.getRepresentation("http://www.example.com/with"));
        //no check that lookups do work withspace uris
        Representation result = yard.getRepresentation(id1);
        assertNotNull("Entity with ID 'http://www.example.com/with space' expected",
            result);
        assertEquals("Entity with id '"+id1+"' expected, but got '"
            + result.getId() + "' instead", id1, result.getId());
        //finally test removal of Entities with space
        yard.remove(id1);
        assertNull("Entity with ID 'http://www.example.com/with space' got not deleted",
            yard.getRepresentation(id1));
        //and also clean up the 2nd entity used for the test
        yard.remove(id2);
    }
    
    @Test
    public void testFieldQuery() throws YardException {
        // NOTE: this does not test if the updated view of the representation is
        // stored, but only that the update method works correctly
        Yard yard = getYard();

        String id1 = "urn:yard.test.testFieldQuery:representation.id1";
        String id2 = "urn:yard.test.testFieldQuery:representation.id2";
        String field = "urn:the.field:used.for.testFieldQuery";
        Representation test1 = create(id1, true);
        Representation test2 = create(id2, true);
        // change the representations to be sure to force an update even if the
        // implementation checks for changes before updating a representation
        test1.add(field, "This is the text content of a field with value1.");
        test2.add(field, "This is the text content of a field with value2.");
        Iterable<Representation> updatedIterable = yard.update(Arrays.asList(test1, test2));
        assertNotNull(updatedIterable);

        FieldQuery query = yard.getQueryFactory().createFieldQuery();
        query.setConstraint(field, new TextConstraint(Arrays.asList("text content")));
        QueryResultList<Representation> results = yard.find(query);
        assertEquals(2, results.size());

        // fetch the light / minimal representation
        query = yard.getQueryFactory().createFieldQuery();
        query.setConstraint(field, new TextConstraint(Arrays.asList("value2")));
        results = yard.find(query);
        assertEquals(1, results.size());
        Representation result = results.iterator().next();
        assertEquals("urn:yard.test.testFieldQuery:representation.id2", result.getId());
        assertEquals(null, result.getFirst(field));

        // fetch the full representation
        results = yard.findRepresentation(query);
        assertEquals(1, results.size());
        result = results.iterator().next();
        assertEquals("urn:yard.test.testFieldQuery:representation.id2", result.getId());
        assertEquals("This is the text content of a field with value2.", result.getFirst(field));
    }

    @Test
    public void testFieldQueryWithSimilarityConstraint() throws YardException {
        // NOTE: this does not test if the updated view of the representation is
        // stored, but only that the update method works correctly
        Yard yard = getYard();
        String id1 = "urn:yard.test.testFieldQueryWithSimilarityConstraint:representation.id1";
        String id2 = "urn:yard.test.testFieldQueryWithSimilarityConstraint:representation.id2";
        String id3 = "urn:yard.test.testFieldQueryWithSimilarityConstraint:representation.id3";
        String similarityfield = NamespaceEnum.rdfs+"comment";
        String filterfield = "urn:the.field:used.for.testFieldQueryWithSimilarityConstraint.filter";
        Representation test1 = create(id1, true);
        Representation test2 = create(id2, true);
        Representation test3 = create(id3, true);
        // change the representations to be sure to force an update even if the
        // implementation checks for changes before updating a representation
        test1.add(similarityfield, "aaaa aaaa aaaa bbbb bbbb cccc cccc dddd dddd");
        test1.add(filterfield, "Some text content");

        test2.add(similarityfield, "aaaa bbbb bbbb bbbb bbbb eeee");
        test2.add(filterfield, "Some other content");

        test3.add(similarityfield, "eeee eeee ffff gggg");
        test3.add(filterfield, "Different content");

        Iterable<Representation> updatedIterable = yard.update(Arrays.asList(test1, test2, test3));
        assertNotNull(updatedIterable);

        // Perform a first similarity query that looks a lot like the first document
        FieldQuery query = yard.getQueryFactory().createFieldQuery();
        query.setConstraint(similarityfield, new SimilarityConstraint("aaaa aaaa aaaa aaaa zzzz yyyy"));
        QueryResultList<Representation> results = yard.find(query);
        assertEquals(2, results.size());
        Iterator<Representation> it = results.iterator();
        Representation first = it.next();
        assertEquals("urn:yard.test.testFieldQueryWithSimilarityConstraint:representation.id1", first.getId());
        // assertEquals(0.99, first.getFirst("http://www.iks-project.eu/ontology/rick/query/score"));

        Representation second = it.next();
        assertEquals("urn:yard.test.testFieldQueryWithSimilarityConstraint:representation.id2",
            second.getId());
        // assertEquals(0.80, first.getFirst("http://www.iks-project.eu/ontology/rick/query/score"));

        // combine similarity with traditional filtering
        query = yard.getQueryFactory().createFieldQuery();
        query.setConstraint(similarityfield, new SimilarityConstraint("aaaa aaaa aaaa aaaa zzzz yyyy"));
        query.setConstraint(filterfield, new TextConstraint(Arrays.asList("other")));
        results = yard.find(query);
        assertEquals(1, results.size());
        it = results.iterator();
        first = it.next();
        assertEquals("urn:yard.test.testFieldQueryWithSimilarityConstraint:representation.id2", first.getId());
    }

    /**
     * This Method removes all Representations create via {@link #create()} or
     * {@link #create(String, boolean)} from the tested {@link Yard}. It also removes all Representations
     * there ID was manually added to the {@link #representationIds} list.
     */
    @After
    public final void clearUpRepresentations() throws YardException {
        if (!representationIds.isEmpty()) {
            yard.remove(representationIds);
        }
    }

}
