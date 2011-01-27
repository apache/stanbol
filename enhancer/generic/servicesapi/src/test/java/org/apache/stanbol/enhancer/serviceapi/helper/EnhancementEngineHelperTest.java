package org.apache.stanbol.enhancer.serviceapi.helper;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_RELATED_CONTENT_ITEM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_EXTRACTION;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.junit.Test;


public class EnhancementEngineHelperTest {

    public static class MyEngine implements EnhancementEngine {

        public int canEnhance(ContentItem ci) throws EngineException {
            return 0;
        }

        public void computeEnhancements(ContentItem ci) throws EngineException {
            // do nothing
        }

    }

    @Test
    public void testEnhancementEngineHelper() throws Exception {
        ContentItem ci = new ContentItem() {
            MGraph mgraph = new SimpleMGraph();
            @Override
            public InputStream getStream() {
                return new ByteArrayInputStream("There is content".getBytes());
            }

            @Override
            public String getMimeType() { return "text/plain"; }

            @Override
            public MGraph getMetadata() { return mgraph; }

            @Override
            public String getId() { return "urn:test:contentItem"; }
        };
        EnhancementEngine engine = new MyEngine();

        UriRef extraction = EnhancementEngineHelper.createNewExtraction(ci, engine);
        MGraph metadata = ci.getMetadata();

        assertTrue(metadata.contains(new TripleImpl(extraction,
                ENHANCER_RELATED_CONTENT_ITEM, new UriRef(ci.getId()))));
        assertTrue(metadata.contains(new TripleImpl(extraction,
                RDF_TYPE, ENHANCER_EXTRACTION)));
        // and so on
    }
}
