package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESSED_LANGUAGES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CREATOR;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_EXTRACTED_FROM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.managed.standalone.StandaloneEmbeddedSolrServerProvider;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.CorpusCreationTask;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.CorpusInfo;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.FieldEncodingEnum;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.FstLinkingEngine;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.IndexConfiguration;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.cache.FastLRUCacheManager;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.cache.SolrEntityCache;
import org.apache.stanbol.enhancer.nlp.json.AnalyzedTextParser;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextUtils;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FstLinkingEngineTest {
    
    private final static Logger log = LoggerFactory.getLogger(FstLinkingEngineTest.class);
    
    /**
     * The SolrYard used for {@link #testSetup()} to check if {@link #REQUIRED_ENTITIES}
     * are present in the data.<p>
     * NOTE that the {@link FstLinkingEngine} DOES NOT require a SolrYard, but directly
     * operates on the #core
     */
    protected static SolrYard yard;
    protected static SolrCore core;
    private static IndexConfiguration fstConfig;
    /**
     * The SolrDirectoryManager also tested within this unit test
     */
    public static final String TEST_YARD_ID = "dbpedia";
    public static final String TEST_SOLR_CORE_NAME = "dbpedia";
    public static final String TEST_SOLR_CORE_CONFIGURATION = "dbpedia_26k.solrindex.bz2";
    protected static final String TEST_INDEX_REL_PATH = File.separatorChar + "target" + File.separatorChar
                                                        + ManagedSolrServer.DEFAULT_SOLR_DATA_DIR;
    
    public static final String TEST_TEXT_FILE = "merkel.txt";
    public static final String TEST_TEXT_NLP_FILE = "merkel_nlp.json";
    
    private static final Literal EN_LANGUAGE = LiteralFactory.getInstance().createTypedLiteral("en");

    protected static final String DBPEDIA = "http://dbpedia.org/resource/";
    
    /**
     * List used in {@link #testSetup()} to validate that all expected entities
     * are contained in the SolrYard initialised based on the 
     * {@link #TEST_SOLR_CORE_CONFIGURATION}.
     */
    private static final List<String> REQUIRED_ENTITIES = Arrays.asList(
        DBPEDIA+"Christian_Democratic_Union_(Germany)", DBPEDIA+"Angela_Merkel", 
        DBPEDIA+"Germany", DBPEDIA+"Social_Democratic_Party_of_Germany",
        DBPEDIA+"Greece"); 

    private ContentItemFactory cif = InMemoryContentItemFactory.getInstance();
    private AnalysedTextFactory atf = AnalysedTextFactory.getDefaultInstance();
    private ContentItem ci;
    private String content;

    /**
     * Used with the {@link EnhancementStructureHelper} to validate Enhancement 
     * results
     */
    private static Map<UriRef,Resource> EXPECTED_ENHANCEMENT_VALUES;
    static{
        EXPECTED_ENHANCEMENT_VALUES = new HashMap<UriRef,Resource>();
        EXPECTED_ENHANCEMENT_VALUES.put(DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
            FstLinkingEngine.class.getName()));
        //adding null as expected for confidence makes it a required property
        EXPECTED_ENHANCEMENT_VALUES.put(Properties.ENHANCER_CONFIDENCE, null);
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
        config.setAllowInitialisation(false);
        config.setIndexConfigurationName(TEST_SOLR_CORE_CONFIGURATION); //the dbpedia default data
        config.setAllowInitialisation(true); //init from datafile provider
        config.setName("DBpedia.org default data");
        config.setDescription("Data used for FstLinkingEngie tests");
        // create the Yard used for the tests
        IndexReference solrIndexRef = IndexReference.parse(config.getSolrServerLocation());
        
        SolrServer server = StandaloneEmbeddedSolrServerProvider.getInstance().getSolrServer(
            solrIndexRef, config.getIndexConfigurationName());
        Assert.assertNotNull("Unable to initialise SolrServer for testing",server);
        core = ((EmbeddedSolrServer)server).getCoreContainer().getCore(
            solrIndexRef.getIndex());
        Assert.assertNotNull("Unable to get SolrCore '" + config.getIndexConfigurationName()
            + "' from SolrServer "+server, core);
        yard = new SolrYard(server,config,null);
        //setup the index configuration
        LanguageConfiguration langConf = new LanguageConfiguration("not.used", 
            new String[]{"en;field=rdfs:label;generate=true"});
        fstConfig = new IndexConfiguration(langConf, core, FieldEncodingEnum.SolrYard);
        fstConfig.setExecutorService(Executors.newFixedThreadPool(1));
        fstConfig.setTypeField("rdf:type");
        fstConfig.setRankingField("entityhub:entityRank");
        //fstConfig.setEntityCacheManager(new FastLRUCacheManager(2048));
        //activate the FST config
        fstConfig.activate(); //activate this configuration
        
        //now create the FST modles and wait until finished
        List<Future<?>> creationTasks = new ArrayList<Future<?>>();
        for(CorpusInfo corpus : fstConfig.getCorpora()){
            //check if the fst does not exist and the fstInfo allows creation
            if(!corpus.isFstFile() && corpus.allowCreation){
                //create a task on the FST corpus creation service
                creationTasks.add(fstConfig.getExecutorService().submit(
                    new CorpusCreationTask(fstConfig, corpus)));
            }
        }
        for(Future<?> future : creationTasks){ //wait for completion
            future.get();
        }
        //validate that the index contains the expected entities
        validateTestIndex();
    }
    
    private static void validateTestIndex() throws Exception {
        log.info("check availability of {} entities", REQUIRED_ENTITIES.size());
        for(String context : REQUIRED_ENTITIES){
            log.debug("  > check Entity {}",context);
            Representation rep = yard.getRepresentation(context);
            assertNotNull(rep);
            assertEquals(rep.getId(),context);
            if(log.isDebugEnabled()){
                log.debug("Data for Entity {}: \n {}",rep.getId(), 
                    ModelUtils.getRepresentationInfo(rep));
            }
        }
        log.info("   ... all Entities present");
    }    
    
    
    @AfterClass
    public static void cleanup() throws Exception {
        if(yard != null){
            yard.close();
        }
        yard = null;
    }
    
    /**
     * Initialises the {@link #ci} and {@link #content} fields for tests.
     * It creates a ContentItem containing a '<code>plain/text</code>' 
     * {@link Blob} for the {@value #TEST_TEXT_FILE} and an {@link AnalysedText}
     * filled with the NLP analysis results stored in 
     * {@link #TEST_TEXT_NLP_FILE}
     * @return the {@link ContentItem} as used for the tests
     * @throws IOException on any IO releated error while reading the test files
     */
    @Before
    public void setupTest() throws IOException {
        //create a contentItem for the plain text used for testing
        InputStream is = FstLinkingEngineTest.class.getClassLoader().getResourceAsStream(TEST_TEXT_FILE);
        Assert.assertNotNull("Unable to load '"+TEST_TEXT_FILE+"' via classpath",is);
        ContentItem ci = cif.createContentItem(new StreamSource(is,"text/plain"));
        AnalysedText at = atf.createAnalysedText(ci, ci.getBlob());
        is.close();
        //parse the prepared NLP results and add it to the ContentItem
        is = FstLinkingEngineTest.class.getClassLoader().getResourceAsStream(TEST_TEXT_NLP_FILE);
        Assert.assertNotNull("Unable to load '"+TEST_TEXT_NLP_FILE+"' via classpath",is);
        AnalyzedTextParser.getDefaultInstance().parse(is, Charset.forName("UTF-8"), at);
        is.close();
        //set the language of the contentItem
        ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, 
            EN_LANGUAGE));
        //set the contentItem and also the content
        this.ci = ci;
        this.content = at.getText().toString();
    }
    @After
    public void cleanupTest() {
        ci = null;
        content = null;
    }
 
    @Test
    public void testFstLinkingWithProperNouns() throws Exception {
        Dictionary<String,Object> dict = new Hashtable<String,Object>();
        dict.put(PROCESSED_LANGUAGES, Arrays.asList("en;lmmtip;uc=LINK;prob=0.75;pprob=0.75"));
        dict.put(PROCESS_ONLY_PROPER_NOUNS_STATE, true);
        TextProcessingConfig tpc = TextProcessingConfig.createInstance(dict);
        EntityLinkerConfig elc = new EntityLinkerConfig();
        elc.setMinFoundTokens(2);//this is assumed by this test
        elc.setRedirectProcessingMode(RedirectProcessingMode.FOLLOW);
        FstLinkingEngine engine = new FstLinkingEngine("proper-noun-linking", 
            fstConfig, tpc, elc);
        processConentItem(engine);
        validateEnhancements();
    }

    @Test
    public void testFstLinkingWithNouns() throws Exception {
        Dictionary<String,Object> dict = new Hashtable<String,Object>();
        dict.put(PROCESSED_LANGUAGES, Arrays.asList("en;lmmtip;uc=LINK;prob=0.75;pprob=0.75"));
        dict.put(PROCESS_ONLY_PROPER_NOUNS_STATE, false);
        TextProcessingConfig tpc = TextProcessingConfig.createInstance(dict);
        EntityLinkerConfig elc = new EntityLinkerConfig();
        elc.setMinFoundTokens(2);//this is assumed by this test
        elc.setRedirectProcessingMode(RedirectProcessingMode.FOLLOW);
        FstLinkingEngine engine = new FstLinkingEngine("proper-noun-linking", 
            fstConfig, tpc, elc);
        processConentItem(engine);
        validateEnhancements();
        
    }

    /**
     * @param expected
     */
    private int[] validateEnhancements() {
        Map<UriRef,Resource> expected = new HashMap<UriRef,Resource>(EXPECTED_ENHANCEMENT_VALUES);
        expected.put(ENHANCER_EXTRACTED_FROM, ci.getUri());
        int[] num = new int[2];
        num[0] = EnhancementStructureHelper.validateAllTextAnnotations(ci.getMetadata(), 
            content, expected);
        log.info("  ... validated {} fise:TextAnnotation",num[0]);
        num[1] = EnhancementStructureHelper.validateAllEntityAnnotations(ci.getMetadata(), 
            expected);
        log.info("  ... validated {} fise:EntityAnnotation",num[1]);
        return num;
    }
    
    /**
     * Processes the {@link #ci} with the parsed engine.
     * @param engine
     * @return returns {@link #ci} as convenience
     * @throws EngineException
     */
    private ContentItem processConentItem(FstLinkingEngine engine) throws EngineException {
        Assert.assertEquals("The FST Linking engine is expected to enhance the "
            + "test ContentItem EnhancementEngine.ENHANCE_ASYNC",
            EnhancementEngine.ENHANCE_ASYNC, engine.canEnhance(ci));
        engine.computeEnhancements(ci);
        return ci;
    }
}
