package eu.iksproject.fise.engines.geonames.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.fise.servicesapi.ContentItem;
import eu.iksproject.fise.servicesapi.EngineException;
import eu.iksproject.fise.servicesapi.TextAnnotation;
import eu.iksproject.fise.servicesapi.helper.RdfEntityFactory;
import eu.iksproject.fise.servicesapi.rdf.OntologicalClasses;
import eu.iksproject.fise.servicesapi.rdf.Properties;
import eu.iksproject.fise.servicesapi.rdf.TechnicalClasses;

public class TestLocationEnhancementEngine {

    private Logger log = LoggerFactory.getLogger(TestLocationEnhancementEngine.class);

    /**
     * The context for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String CONTEXT = "Dr. Patrick Marshall (1869 - November 1950) was a"
        + " geologist who lived in New Zealand and worked at the University of Otago.";

    /**
     * The person for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String PERSON = "Patrick Marshall";

    /**
     * The organisation for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String ORGANISATION ="University of Otago";

    /**
     * The place for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String PLACE = "New Zealand";


    static LocationEnhancementEngine locationEnhancementEngine = new LocationEnhancementEngine();

    @BeforeClass
    public static void setUpServices() throws IOException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        MockComponentContext context = new MockComponentContext(properties);
        locationEnhancementEngine.activate(context);
    }

    @AfterClass
    public static void shutdownServices() {
        locationEnhancementEngine.deactivate(null);
    }

    public static ContentItem getContentItem(final String id,
            final String text) {
        return new ContentItem() {

            SimpleMGraph metadata = new SimpleMGraph();

            public InputStream getStream() {
                return new ByteArrayInputStream(text.getBytes());
            }

            public String getMimeType() {
                return "text/plain";
            }

            public MGraph getMetadata() {
                return metadata;
            }

            public String getId() {
                return id;
            }
        };
    }

    public static void getTextAnnotation(ContentItem ci, String name,String context,UriRef type){
        String content;
        try {
            content = IOUtils.toString(ci.getStream());
        } catch (IOException e) {
            //should never happen anyway!
            content = "";
        }
        RdfEntityFactory factory = RdfEntityFactory.createInstance(ci.getMetadata());
        TextAnnotation testAnnotation = factory.getProxy(new UriRef("urn:iks-project:fise:test:text-annotation:person"), TextAnnotation.class);
        testAnnotation.setCreator(new UriRef("urn:iks-project:fise:test:dummyEngine"));
        testAnnotation.setCreated(new Date());
        testAnnotation.setSelectedText(name);
        testAnnotation.setSelectionContext(context);
        testAnnotation.getDcType().add(type);
        Integer start = content.indexOf(name);
        if(start < 0){ //if not found in the content
            //set some random numbers for start/end
            start = (int)Math.random()*100;
        }
        testAnnotation.setStart(start);
        testAnnotation.setEnd(start+name.length());
    }

    @Test
    public void testLocationEnhancementEngine() {//throws Exception{
        //create a content item
        ContentItem ci = getContentItem("urn:iks-project:fise:text:content-item:person", CONTEXT);
        //add three text annotations to be consumed by this test
        getTextAnnotation(ci, PERSON, CONTEXT, OntologicalClasses.DBPEDIA_PERSON);
        getTextAnnotation(ci, ORGANISATION, CONTEXT, OntologicalClasses.DBPEDIA_ORGANISATION);
        getTextAnnotation(ci, PLACE, CONTEXT, OntologicalClasses.DBPEDIA_PLACE);
        //perform the computation of the enhancements
        try {
            locationEnhancementEngine.computeEnhancements(ci);
        } catch (EngineException e) {
            if(e.getCause() instanceof UnknownHostException) {
                log.warn("Unable to test LocationEnhancemetEngine when offline! -> skipping this test",e.getCause());
                return;
            } else if(e.getCause() instanceof SocketTimeoutException){
                log.warn("Seams like the geonames.org webservice is currently unavailable -> skipping this test",e.getCause());
                return;
            } else if (e.getMessage().contains("overloaded with requests")) {
                log.warn(
                        "Seams like the geonames.org webservice is currently unavailable -> skipping this test",
                        e.getCause());
                return;
            }
        }
        /*
         * Note:
         *  - Expected results depend on the geonames.org data. So if the test
         *    fails it may also mean that the data provided by geonames.org have
         *    changed
         */
        int entityAnnotationCount = checkAllEntityAnnotations(ci.getMetadata());
        //two suggestions for New Zealand and one hierarchy entry for the first
        //suggestion
        assertEquals(3, entityAnnotationCount);
    }

    /*
     * -----------------------------------------------------------------------
     * Helper Methods to check Text and EntityAnnotations
     * -----------------------------------------------------------------------
     */

    /**
     * @param g
     * @return
     */
    private int checkAllEntityAnnotations(MGraph g) {
        Iterator<Triple> entityAnnotationIterator = g.filter(null,
                Properties.RDF_TYPE, TechnicalClasses.FISE_ENTITYANNOTATION);
        int entityAnnotationCount = 0;
        while (entityAnnotationIterator.hasNext()) {
            UriRef entityAnnotation = (UriRef) entityAnnotationIterator.next().getSubject();
            // test if selected Text is added
            checkEntityAnnotation(g, entityAnnotation);
            entityAnnotationCount++;
        }
        return entityAnnotationCount;
    }

    /**
     * Checks if an entity annotation is valid
     *
     * @param g
     * @param textAnnotation
     */
    private void checkEntityAnnotation(MGraph g, UriRef entityAnnotation) {
        Iterator<Triple> relationIterator = g.filter(
                entityAnnotation, Properties.DC_RELATION, null);
        Iterator<Triple> requiresIterator = g.filter(
                entityAnnotation, Properties.DC_REQUIRES, null);
        // check if the relation or an requires annotation set
        assertTrue(relationIterator.hasNext() || requiresIterator.hasNext());
        while (relationIterator.hasNext()) {
            // test if the referred annotations are text annotations
            UriRef referredTextAnnotation = (UriRef) relationIterator.next().getObject();
            assertTrue(g.filter(referredTextAnnotation, Properties.RDF_TYPE,
                    TechnicalClasses.FISE_TEXTANNOTATION).hasNext());
        }

        // test if an entity is referred
        Iterator<Triple> entityReferenceIterator = g.filter(entityAnnotation,
                Properties.FISE_ENTITY_REFERENCE, null);
        assertTrue(entityReferenceIterator.hasNext());
        // test if the reference is an URI
        assertTrue(entityReferenceIterator.next().getObject() instanceof UriRef);
        // test if there is only one entity referred
        assertFalse(entityReferenceIterator.hasNext());

        // finally test if the entity label is set
        Iterator<Triple> entityLabelIterator = g.filter(entityAnnotation,
                Properties.FISE_ENTITY_LABEL, null);
        assertTrue(entityLabelIterator.hasNext());
    }

}
