package org.apache.stanbol.autotagging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.stanbol.autotagging.Autotagger;
import org.apache.stanbol.autotagging.TagInfo;
import org.apache.stanbol.autotagging.jena.ModelIndexer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class AutotaggingTest {

    private Model model;

    private RAMDirectory ramDirectory;

    private StandardAnalyzer analyzer;

    private IndexWriter writer;

    protected File defaultIndexDirectory;

    public static InputStream getResource(String name) {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                name);
        assertNotNull("failed to load resource " + name, stream);
        return stream;
    }

    @BeforeClass
    public static void setUpDefaultIndex() throws Exception {
        // create index from scratch
        ModelIndexer.buildDefaultIndex(null, true);
    }

    @Before
    public void setUp() throws CorruptIndexException,
            LockObtainFailedException, IOException {
        model = ModelFactory.createDefaultModel();
        ramDirectory = new RAMDirectory();
        analyzer = new StandardAnalyzer(Version.LUCENE_30);
        writer = new IndexWriter(ramDirectory, analyzer, true,
                new IndexWriter.MaxFieldLength(25000));
        model.read(getResource("dbpedia_3.4_instancetype_en.nt"), null,
                "N-TRIPLE");
        model.read(getResource("dbpedia_3.4_longabstract_en.nt"), null,
                "N-TRIPLE");
        // will reuse the index built by setUpDefaultIndex
        defaultIndexDirectory = ModelIndexer.buildDefaultIndex();
    }

    @Test
    public void testIndexing() throws IOException, ParseException {
        // index model without closing it since it is memory only
        ModelIndexer.index(model, writer, false);
        writer.close();

        // perform a query on the fulltext content of the abstracts in the model
        IndexSearcher isearcher = new IndexSearcher(ramDirectory, true); // read-only=true
        QueryParser parser = new QueryParser(Version.LUCENE_30,
                "http://dbpedia.org/property/abstract", analyzer);
        Query query = parser.parse("1981");
        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        assertEquals(1, hits.length);

        // check that the match point to the expected entity
        Document hitDoc = isearcher.doc(hits[0].doc);
        assertEquals("http://dbpedia.org/resource/%21Action_Pact%21",
                hitDoc.get(ModelIndexer.URI_FIELD));
        String[] types = hitDoc.getValues("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        assertEquals(3, types.length);
        isearcher.close();
    }

    @Test
    public void testAutotaggingWithCustomIndex() throws IOException, ParseException {
        // index the model
        testIndexing();

        // perform a suggestion query
        Autotagger autotagger = new Autotagger(ramDirectory);
        List<TagInfo> tags = autotagger.suggest("The punk side in me is telling me to listen to the british band Action Pact.");
        assertTrue(!tags.isEmpty());

        assertEquals("http://dbpedia.org/resource/%21Action_Pact%21",
                tags.get(0).getId());
        assertEquals("!Action Pact!", tags.get(0).getLabel());
        assertEquals(3, tags.get(0).getType().length);
        assertEquals(0.59, tags.get(0).getConfidence(), 0.1f);
        assertEquals("http://dbpedia.org/ontology/Band", tags.get(0).getType()[0]);
        assertEquals("http://dbpedia.org/ontology/Organisation",
                tags.get(0).getType()[1]);
        assertEquals("http://www.w3.org/2002/07/owl#Thing", tags.get(0).getType()[2]);
    }

    @Test
    public void testAutotaggingWithDefaultIndex() throws IOException,
            ParseException {

        // build a tagger using a the default DBpedia based index
        Directory dir = FSDirectory.open(defaultIndexDirectory);
        Autotagger autotagger = new Autotagger(dir);

        // perform a context similarity search for a Person
        String context = "Let the autotagger guess who was a Jamaican"
                + " musician, a lead singer and guitarist"
                + " for a well known reggae band.";

        List<TagInfo> tags = autotagger.suggestForType(context, "Person");
        assertTrue(!tags.isEmpty());
        TagInfo bestGuess = tags.get(0);

        assertEquals("http://dbpedia.org/resource/Bob_Marley", bestGuess.getId());
        assertEquals("Bob Marley", bestGuess.getLabel());

        List<String> types = Arrays.asList(bestGuess.getType());
        assertEquals(4, types.size());
        assertTrue(types.contains("http://www.w3.org/2002/07/owl#Thing"));
        assertTrue(types.contains("http://dbpedia.org/ontology/Person"));
        assertTrue(types.contains("http://dbpedia.org/ontology/Artist"));
        assertTrue(types.contains("http://dbpedia.org/ontology/MusicalArtist"));
    }

    @Test
    public void testEntityByNameWithContext() throws IOException {

        // build a tagger using a the default DBpedia based index
        Directory dir = FSDirectory.open(defaultIndexDirectory);
        Autotagger autotagger = new Autotagger(dir);

        // fuzzy lookup by entity name
        String name = "the city of Paris";

        // TODO: find an entity where the context can help filter out ambiguity
        String context = "The river Seine flows in the city of Paris ";

        // strict name lookup (by default)
        List<TagInfo> tags = autotagger.suggestForType(name, context, "Place");
        assertTrue(tags.isEmpty());

        // lax name lookup
        tags = autotagger.withStrictNameLookup(false).suggestForType(name,
                context, "Place");
        assertTrue(!tags.isEmpty());
        assertEquals("http://dbpedia.org/resource/Paris", tags.get(0).getId());
        assertEquals("Paris", tags.get(0).getLabel());
    }

    @Test
    public void testEntityByNameWithoutContext() throws IOException {

        // build a tagger using a the default DBpedia based index
        Directory dir = FSDirectory.open(defaultIndexDirectory);
        Autotagger autotagger = new Autotagger(dir).withStrictNameLookup(false);

        // at least of one of the terms is matching
        String name = "The city of Paris";

        List<TagInfo> tags = autotagger.suggestForType(name, "Place");
        assertTrue(!tags.isEmpty());
        assertEquals("http://dbpedia.org/resource/Paris", tags.get(0).getId());

        // try with non existing name
        name = "somethingnot referencedin theindex";
        tags = autotagger.suggestForType(name, "Place");
        assertTrue(tags.isEmpty());
    }

}
