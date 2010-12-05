package eu.iksproject.fise.engines.entitytagging.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.iksproject.fise.engines.entitytagging.impl.ReferencedSiteEntityTaggingEnhancementEngine;
import eu.iksproject.fise.servicesapi.ContentItem;
import eu.iksproject.fise.servicesapi.TextAnnotation;
import eu.iksproject.fise.servicesapi.helper.RdfEntityFactory;
import eu.iksproject.fise.servicesapi.rdf.Properties;
import eu.iksproject.fise.servicesapi.rdf.TechnicalClasses;

public class TestEntityLinkingEnhancementEngine {

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
	
	static ReferencedSiteEntityTaggingEnhancementEngine entityLinkingEngine = new ReferencedSiteEntityTaggingEnhancementEngine();

    @BeforeClass
    public static void setUpServices() throws IOException {
    }
	  @Before
	  public void bindServices() throws IOException {
	  }
	
	  @After
	  public void unbindServices() {
	  }

    @AfterClass
    public static void shutdownServices() {
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
    public void testEntityLinkingEnhancementEngine() throws Exception{
    	//TODO: adapt this test to work with this engine
    	// -> here the problem is mainly to fake the needed infrastructure
    	return;
//    	//create a content item
//    	ContentItem ci = getContentItem("urn:iks-project:fise:text:content-item:person", CONTEXT);
//    	//add three text annotations to be consumed by this test
//    	getTextAnnotation(ci, PERSON, CONTEXT, OntologicalClasses.DBPEDIA_PERSON);
//    	getTextAnnotation(ci, ORGANISATION, CONTEXT, OntologicalClasses.DBPEDIA_ORGANISATION);
//    	getTextAnnotation(ci, PLACE, CONTEXT, OntologicalClasses.DBPEDIA_PLACE);
//    	//perform the computation of the enhancements
//    	entityLinkingEngine.computeEnhancements(ci);
//    	int entityAnnotationCount = checkAllEntityAnnotations(ci.getMetadata());
//    	assertEquals(2, entityAnnotationCount);
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
        Iterator<Triple> relationToTextAnnotationIterator = g.filter(
                entityAnnotation, Properties.DC_RELATION, null);
        // check if the relation to the text annotation is set
        assertTrue(relationToTextAnnotationIterator.hasNext());
        while (relationToTextAnnotationIterator.hasNext()) {
            // test if the referred annotations are text annotations
            UriRef referredTextAnnotation = (UriRef) relationToTextAnnotationIterator.next().getObject();
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
