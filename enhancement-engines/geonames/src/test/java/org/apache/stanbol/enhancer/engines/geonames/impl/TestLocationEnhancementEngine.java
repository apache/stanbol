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
package org.apache.stanbol.enhancer.engines.geonames.impl;

import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.PROPERTY_NAME;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllEntityAnnotations;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.rdfentities.RdfEntityFactory;
import org.apache.stanbol.enhancer.rdfentities.fise.TextAnnotation;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestLocationEnhancementEngine {

    private static final Logger log = LoggerFactory.getLogger(TestLocationEnhancementEngine.class);

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
    public static final String ORGANISATION = "University of Otago";

    /**
     * The place for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String PLACE = "New Zealand";

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();

    static LocationEnhancementEngine locationEnhancementEngine = new LocationEnhancementEngine();

    @BeforeClass
    public static void setUpServices() throws IOException, ConfigurationException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(PROPERTY_NAME, "geonames");
        // use the anonymous service for the unit tests
        properties.put(LocationEnhancementEngine.GEONAMES_USERNAME, 
            "\u0073\u0074\u0061\u006E\u0062\u006F\u006C");
        properties.put(LocationEnhancementEngine.GEONAMES_TOKEN, 
            "\u0073\u0074\u006E\u0062\u006C\u002E\u0075\u0074");
        MockComponentContext context = new MockComponentContext(properties);
        locationEnhancementEngine.activate(context);
    }

    @AfterClass
    public static void shutdownServices() {
        locationEnhancementEngine.deactivate(null);
    }

    public static ContentItem getContentItem(final String id,
            final String text) throws IOException {
    	return ciFactory.createContentItem(new IRI(id), new StringSource(text));
    }

    public static void getTextAnnotation(ContentItem ci, String name, String context, IRI type) {
        String content;
        try {
            content = IOUtils.toString(ci.getStream(),"UTF-8");
        } catch (IOException e) {
            //should never happen anyway!
            content = "";
        }
        RdfEntityFactory factory = RdfEntityFactory.createInstance(ci.getMetadata());
        TextAnnotation testAnnotation = factory.getProxy(new IRI("urn:org.apache:stanbol.enhancer:test:text-annotation:person"), TextAnnotation.class);
        testAnnotation.setCreator(new IRI("urn:org.apache:stanbol.enhancer:test:dummyEngine"));
        testAnnotation.setCreated(new Date());
        testAnnotation.setSelectedText(name);
        testAnnotation.setSelectionContext(context);
        testAnnotation.getDcType().add(type);
        Integer start = content.indexOf(name);
        if (start < 0) { //if not found in the content
            //set some random numbers for start/end
            start = (int) (Math.random() * 100);
        }
        testAnnotation.setStart(start);
        testAnnotation.setEnd(start + name.length());
    }

    @Test
    public void testLocationEnhancementEngine() throws IOException, EngineException {
        //create a content item
        ContentItem ci = getContentItem("urn:org.apache:stanbol.enhancer:text:content-item:person", CONTEXT);
        //add three text annotations to be consumed by this test
        getTextAnnotation(ci, PERSON, CONTEXT, DBPEDIA_PERSON);
        getTextAnnotation(ci, ORGANISATION, CONTEXT, DBPEDIA_ORGANISATION);
        getTextAnnotation(ci, PLACE, CONTEXT, DBPEDIA_PLACE);
        //perform the computation of the enhancements
        try {
            locationEnhancementEngine.computeEnhancements(ci);
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e, "overloaded with requests");
            return;
        }
        Map<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
            locationEnhancementEngine.getClass().getName()));
        //adding null as expected for confidence makes it a required property
        expectedValues.put(Properties.ENHANCER_CONFIDENCE, null);

        /*
         * Note:
         *  - Expected results depend on the geonames.org data. So if the test
         *    fails it may also mean that the data provided by geonames.org have
         *    changed
         */
        int entityAnnotationCount = validateAllEntityAnnotations(ci.getMetadata(),expectedValues);
        //two suggestions for New Zealand and one hierarchy entry for the first
        //suggestion
        //NOTE 2012-10-10: changed expected value back to "3" as geonames.org
        //   again returns "Oceania" as parent for "New Zealand"
        //NOTE: 2012-11-12: deactivated this check, because this the fact that
        //   "Oceania" is returned as parent for "New Zealand" changes every
        //   every view weeks
        //assertEquals(3, entityAnnotationCount);
    }


}
