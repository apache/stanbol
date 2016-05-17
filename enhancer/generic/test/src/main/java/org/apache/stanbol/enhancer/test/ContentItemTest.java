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
package org.apache.stanbol.enhancer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ContentItemTest {
    
    private final Logger log = LoggerFactory.getLogger(ContentItemTest.class);
    //private static final IRI ciUri = new IRI("http://example.org/");
    private static final ContentSource contentSource = new StringSource("This is a Test!"); 
    /**
     * Used to create ContentItems used by this Test. Each call MUST return a
     * newly initialised version.
     * @param source The ContentSource
     * @return the ContentItem used for a test
     */
    protected abstract ContentItem createContentItem(ContentSource source) throws IOException;
    /**
     * Used to create Blobs used by this Tests. Each call MUST return a new
     * blob instance.
     * @param source The {@link ContentSource} used to create the Blob
     * @return the Blob
     */
    protected abstract Blob createBlob(ContentSource source) throws IOException;
    
	@Test
	public void addingAndRetrieving()  throws IOException{
		ContentItem ci = createContentItem(contentSource);
		assertNotNull(ci);
		assertNotNull(ci.getUri());
		IRI partUri = new IRI("http://foo/");
		Date someObject = new Date();
		ci.addPart(partUri, someObject);
		ci.getMetadata().add(new TripleImpl(ci.getUri(), new IRI("http://example.org/ontology#hasPart"), partUri));
        ci.getMetadata().add(new TripleImpl(partUri, new IRI("http://example.org/ontology#isPartOf"),ci.getUri()));
		assertEquals(someObject, ci.getPart(partUri, Date.class));
		assertEquals(someObject, ci.getPart(1, Date.class));
		assertEquals(partUri, ci.getPartUri(1));
		assertEquals(new IRI(ci.getUri().getUnicodeString()+"_main"), ci.getPartUri(0));
		try {
		    ci.getPart(2, Object.class);
		    assertTrue("Requesting non existance part MUST throw an NoSuchPartException", false);
		} catch (NoSuchPartException e) {/* expected*/}
        try {
            ci.getPart(new IRI("http://foo/nonexisting"), Object.class);
            assertTrue("Requesting non existance part MUST throw an NoSuchPartException", false);
        } catch (NoSuchPartException e) {/* expected*/}
        try {
            ci.getPartUri(2);
            assertTrue("Requesting non existance part MUST throw an NoSuchPartException", false);
        } catch (NoSuchPartException e) {/* expected*/}
		//finally log the toString
		log.info("toString: {}",ci);
	}
	@Test(expected=IllegalArgumentException.class)
	public void addPartWithoutUri() throws IOException{
        ContentItem ci = createContentItem(contentSource);
	    ci.addPart(null, new Date());
	}
    @Test(expected=IllegalArgumentException.class)
    public void addPartWithoutPartContent() throws IOException{
        ContentItem ci = createContentItem(contentSource);
        ci.addPart(new IRI("http://foo/"), null);
    }
    /**
     * The ContentItem MUST NOT allow to replace the main content part (the
     * Blob stored at index 0)
     */
    @Test(expected=IllegalArgumentException.class)
    public void replaceMainPart() throws IOException{
        ContentItem ci = createContentItem(contentSource);
        IRI mainPart = ci.getPartUri(0);
        ci.addPart(mainPart, new Date());
    }
    @Test(expected=IllegalArgumentException.class)
    public void removeNullPart()  throws IOException{
        ContentItem ci = createContentItem(contentSource);
        ci.removePart(null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void removeNegaitveIndexPart() throws IOException {
        ContentItem ci = createContentItem(contentSource);
        ci.removePart(-1);
    }
    @Test(expected=IllegalStateException.class)
    public void removeMainContentPartByUri() throws IOException {
        ContentItem ci = createContentItem(contentSource);
        ci.removePart(ci.getPartUri(0));
    }
    @Test(expected=IllegalStateException.class)
    public void removeMainContentPartByIndex() throws IOException {
        ContentItem ci = createContentItem(contentSource);
        ci.removePart(0);
    }
    @Test(expected=NoSuchPartException.class)
    public void removeNonExistentPartByUri() throws IOException {
        ContentItem ci = createContentItem(contentSource);
        ci.removePart(new IRI("urn:does.not.exist:and.can.not.be.removed"));
    }
    @Test(expected=NoSuchPartException.class)
    public void removeNonExistentPartByIndex() throws IOException {
        ContentItem ci = createContentItem(contentSource);
        ci.removePart(12345);
    }
    @Test
    public void removeRemoveByUri() throws IOException {
        ContentItem ci = createContentItem(contentSource);
        IRI uri = new IRI("urn:content.part:remove.test");
        ci.addPart(uri, new Date());
        try {
            ci.getPart(uri, Date.class);
        }catch (NoSuchPartException e) {
            assertFalse("The part with the uri "+uri+" was not added correctly",
                true);
        }
        ci.removePart(uri);
        try {
            ci.getPart(uri, Date.class);
            assertFalse("The part with the uri "+uri+" was not removed correctly",
                true);
        }catch (NoSuchPartException e) {
            // expected
        }
    }
    @Test
    public void removeRemoveByIndex() throws IOException {
        ContentItem ci = createContentItem(contentSource);
        IRI uri = new IRI("urn:content.part:remove.test");
        ci.addPart(uri, new Date());
        int index = -1;
        try {
            for(int i=0; index < 0; i++){
                IRI partUri = ci.getPartUri(i);
                if(partUri.equals(uri)){
                    index = i;
                }
            }
        }catch (NoSuchPartException e) {
            assertFalse("The part with the uri "+uri+" was not added correctly",
                true);
        }
        ci.removePart(index);
        try {
            ci.getPart(index, Date.class);
            assertTrue("The part with the uri "+uri+" was not removed correctly",
                false);
        }catch (NoSuchPartException e) {
            // expected
        }
    }
    
}
