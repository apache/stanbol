package org.apache.stanbol.entityhub.ldpath.backend;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.*;

import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.model.programs.Program;

public class BackendTest {

    private static final Logger log = LoggerFactory.getLogger(BackendTest.class);
    /**
     * The SolrYard used for the tests
     */
    private static Yard yard;
    private static YardBackend backend;
    /**
     * The SolrDirectoryManager also tested within this unit test
     */
    public static final String TEST_YARD_ID = "dbpedia";
    public static final String TEST_SOLR_CORE_NAME = "dbpedia_43k";
    protected static final String TEST_INDEX_REL_PATH = File.separatorChar + "target" + File.separatorChar
                                                        + ManagedSolrServer.DEFAULT_SOLR_DATA_DIR;

    private static final String DBPEDIA = "http://dbpedia.org/resource/";
    private static final String CONTEXT_PARIS = DBPEDIA+"Paris";
    private static final String DBPEDIA_TEST_PROGRAM;
    static {
        StringBuilder builder = new StringBuilder();
        //TODO:write LDPath test statement (or load it from test resources
        builder.append("title = rdfs:label :: xsd:string;");
        builder.append("title_en = rdfs:label[@en] :: xsd:string;");
        builder.append("type = rdf:type :: xsd:anyURI;");
        builder.append("all = * :: xsd:string;");
        DBPEDIA_TEST_PROGRAM = builder.toString();
    }

    private static final Map<String,Collection<?>> EXPECTED_RESULTS_PARIS;
    static {
        EXPECTED_RESULTS_PARIS = new HashMap<String,Collection<?>>();
        EXPECTED_RESULTS_PARIS.put("title_en", new HashSet<String>(Arrays.asList("Paris")));
        EXPECTED_RESULTS_PARIS.put("title", new HashSet<String>(
                Arrays.asList("Paris","Parijs","Parigi","Pariisi","巴黎","Париж")));
// LDPath uses String to represent anyUri
        EXPECTED_RESULTS_PARIS.put("type", new HashSet<String>(Arrays.asList(
            "http://www.w3.org/2002/07/owl#Thing",
            "http://dbpedia.org/ontology/Place",
            "http://dbpedia.org/ontology/PopulatedPlace",
            "http://dbpedia.org/ontology/Settlement",
            "http://www.opengis.net/gml/_Feature",
            "http://dbpedia.org/ontology/Settlement",
            "http://www.opengis.net/gml/_Feature")));
//        EXPECTED_RESULTS_PARIS.put("type", new HashSet<URI>(Arrays.asList(
//            URI.create("http://www.w3.org/2002/07/owl#Thing"),
//            URI.create("http://dbpedia.org/ontology/Place"),
//            URI.create("http://dbpedia.org/ontology/PopulatedPlace"),
//            URI.create("http://dbpedia.org/ontology/Settlement"),
//            URI.create("http://www.opengis.net/gml/_Feature"),
//            URI.create("http://dbpedia.org/ontology/Settlement"),
//            URI.create("http://www.opengis.net/gml/_Feature"))));
        //Add all previouse and some additional to test the WIldcard implementation
        Collection<Object> allValues = new HashSet<Object>();
        for(Collection<?> values : EXPECTED_RESULTS_PARIS.values()){
            allValues.addAll(values);
        }
        allValues.addAll(Arrays.asList(
            "http://dbpedia.org/resource/Category:Capitals_in_Europe",
            "http://dbpedia.org/resource/Category:Host_cities_of_the_Summer_Olympic_Games",
            "2.350833","0.81884754","2193031"));
        EXPECTED_RESULTS_PARIS.put("all", allValues);
    }
    
    private static final String CONTEXT_HARVARD_ALUMNI = DBPEDIA+"Category:Harvard_University_alumni";
    private static final String CATEGORIES_TEST_PROGRAM;
    static {
        StringBuilder builder = new StringBuilder();
        //TODO:write LDPath test statement (or load it from test resources
        builder.append("name = rdfs:label :: xsd:string;");
        builder.append("parent = skos:broader :: xsd:anyURI;");
        builder.append("childs = ^skos:broader :: xsd:anyURI;");
        builder.append("members = ^<http://purl.org/dc/terms/subject> :: xsd:anyURI;");
        CATEGORIES_TEST_PROGRAM = builder.toString();
    }

    private static final Map<String,Collection<?>> EXPECTED_HARVARD_ALUMNI;
    static {
        EXPECTED_HARVARD_ALUMNI = new HashMap<String,Collection<?>>();
        EXPECTED_HARVARD_ALUMNI.put("name", new HashSet<String>(
                Arrays.asList("Harvard University alumni")
                ));
        EXPECTED_HARVARD_ALUMNI.put("parent", new HashSet<String>(Arrays.asList(
            "http://dbpedia.org/resource/Category:Harvard_University_people",
            "http://dbpedia.org/resource/Category:Alumni_by_university_or_college_in_Massachusetts",
            "http://dbpedia.org/resource/Category:Ivy_League_alumni")
            ));
        EXPECTED_HARVARD_ALUMNI.put("childs", new HashSet<String>(Arrays.asList(
            "http://dbpedia.org/resource/Category:John_F._Kennedy_School_of_Government_alumni",
            "http://dbpedia.org/resource/Category:Harvard_Law_School_alumni",
            "http://dbpedia.org/resource/Category:Harvard_Medical_School_alumni",
            "http://dbpedia.org/resource/Category:Harvard_Business_School_alumni")
            ));
        EXPECTED_HARVARD_ALUMNI.put("members", new HashSet<String>(Arrays.asList(
            "http://dbpedia.org/resource/Edward_Said",
            "http://dbpedia.org/resource/Cole_Porter", 
            "http://dbpedia.org/resource/Theodore_Roosevelt",
            "http://dbpedia.org/resource/Al_Gore",
            "http://dbpedia.org/resource/T._S._Eliot",
            "http://dbpedia.org/resource/Henry_Kissinger",
            "http://dbpedia.org/resource/Robert_F._Kennedy",
            "http://dbpedia.org/resource/Benjamin_Netanyahu",
            "http://dbpedia.org/resource/Natalie_Portman",
            "http://dbpedia.org/resource/John_F._Kennedy",
            "http://dbpedia.org/resource/Michelle_Obama",
            "http://dbpedia.org/resource/Jacques_Chirac",
            "http://dbpedia.org/resource/Pierre_Trudeau",
            "http://dbpedia.org/resource/Jack_Lemmon",
            "http://dbpedia.org/resource/Franklin_D._Roosevelt",
            "http://dbpedia.org/resource/John_Adams") // and manny more
            ));
        //Add all previouse and some additional to test the WIldcard implementation
        Collection<Object> allValues = new HashSet<Object>();
        for(Collection<?> values : EXPECTED_RESULTS_PARIS.values()){
            allValues.addAll(values);
        }
        allValues.addAll(Arrays.asList(
            "http://dbpedia.org/resource/Category:Capitals_in_Europe",
            "http://dbpedia.org/resource/Category:Host_cities_of_the_Summer_Olympic_Games",
            "2.350833","0.81884754","2193031"));
        EXPECTED_RESULTS_PARIS.put("all", allValues);
    }    
    @BeforeClass
    public static void setup() throws Exception {
        // get the working directory
        // use property substitution to test this feature!
        String prefix = System.getProperty("basedir") == null ? "." : "${basedir}";
        String solrServerDir = prefix + TEST_INDEX_REL_PATH;
        log.info("Test Solr Server Directory: {}", solrServerDir);
        System.setProperty(ManagedSolrServer.MANAGED_SOLR_DIR_PROPERTY, solrServerDir);
        SolrYardConfig config = new SolrYardConfig(TEST_YARD_ID, TEST_SOLR_CORE_NAME);
        config.setDefaultInitialisation(false);
        config.setName("DBpedia.org default data");
        config.setDescription("Data used for the LDPath setup");
        // create the Yard used for the tests
        yard = new SolrYard(config);
        backend = new YardBackend(yard);
    }
    
    /**
     * Tests that the yard is setup correctly
     * @throws Exception
     */
    @Test
    public void testSetup() throws Exception {
        Representation rep = yard.getRepresentation(CONTEXT_PARIS);
        assertNotNull(rep);
        assertEquals(rep.getId(),CONTEXT_PARIS);
        log.info(ModelUtils.getRepresentationInfo(rep));
        rep = yard.getRepresentation(CONTEXT_HARVARD_ALUMNI);
        assertNotNull(rep);
        assertEquals(rep.getId(),CONTEXT_HARVARD_ALUMNI);
        log.info(ModelUtils.getRepresentationInfo(rep));
    }
    /**
     * Test {@link RDFBackend} implementation including WildCard
     * @throws Exception
     */
    @Test
    public void testLDPath() throws Exception {
        LDPath<Object> ldPath = new LDPath<Object>(backend);
        Program<Object> program = ldPath.parseProgram(new InputStreamReader(
            new ByteArrayInputStream(
                DBPEDIA_TEST_PROGRAM.getBytes("utf-8")
                ), "utf-8"));
        assertNotNull("parsed Programm is null (Input: "+
            DBPEDIA_TEST_PROGRAM+")", program);
        log.info("LDPath Programm:\n{}",program.getPathExpression(backend));
        Object context = backend.createURI(CONTEXT_PARIS);
        Map<String,Collection<?>> result = program.execute(backend, context);
        log.info("Results for {}:\n{}",CONTEXT_PARIS,result);
        assertNotNull("The result of the LDPath execution MUST NOT be NULL " +
        		"(entity: %s)",context);
        assertLDPathResult(result,EXPECTED_RESULTS_PARIS);
    }
    @Test
    public void testInversePath() throws Exception {
        LDPath<Object> ldPath = new LDPath<Object>(backend);
        Program<Object> program = ldPath.parseProgram(new InputStreamReader(
            new ByteArrayInputStream(
                CATEGORIES_TEST_PROGRAM.getBytes("utf-8")
                ), "utf-8"));
        assertNotNull("parsed Programm is null (Input: "+
            CATEGORIES_TEST_PROGRAM+")", program);
        log.info("LDPath Programm:\n{}",program.getPathExpression(backend));
        Object context = backend.createURI(CONTEXT_HARVARD_ALUMNI);
        Map<String,Collection<?>> result = program.execute(backend, context);
        log.info("Results for {}:\n{}",CONTEXT_HARVARD_ALUMNI,result);
        assertNotNull("The result of the LDPath execution MUST NOT be NULL " +
                "(entity: %s)",context);
        assertLDPathResult(result,EXPECTED_HARVARD_ALUMNI);
    }

    /**
     * @param result
     * @param expected
     */
    private void assertLDPathResult(Map<String,Collection<?>> result, Map<String,Collection<?>> expected) {
        log.info("Assert LDPath Result for {}:", CONTEXT_PARIS);
        Map<String,Collection<?>> expectedClone = new HashMap<String,Collection<?>>();
        for(Entry<String,Collection<?>> expectedEntries : expected.entrySet()){
            expectedClone.put(expectedEntries.getKey(), new HashSet<Object>(expectedEntries.getValue()));
        }
        for(Entry<String,Collection<?>> entry : result.entrySet()){
            log.info("{}: {}",entry.getKey(),entry.getValue());
            Collection<?> expectedValues = expectedClone.remove(entry.getKey());
            assertNotNull("Current field '"+entry.getKey()+"' is not expected (expected: " +
            		expectedClone.keySet()+"!",
                expectedValues);
            expectedValues.removeAll(entry.getValue());
            assertTrue("Missing expected Result '"+expectedValues+"' (present: '"
                +entry.getValue()+"'", expectedValues.isEmpty());
        }
        assertTrue("Missing expected Field '"+expectedClone.keySet()+"' (present: '"+
            result.keySet()+"'!",expectedClone.isEmpty());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        yard = null;
    }
}
