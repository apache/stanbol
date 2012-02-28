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
package org.apache.stanbol.enhancer.serviceapi.helper;

import java.util.Date;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryBlob;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemImpl;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentItemTest {
    
    private final Logger log = LoggerFactory.getLogger(ContentItemTest.class);
    private static final UriRef ciUri = new UriRef("http://example.org/");
    private static final Blob blob = new InMemoryBlob("hello", null);

    @Test(expected=IllegalArgumentException.class)
    public void missingUri(){
        new ContentItemImpl(null,blob,new SimpleMGraph()){};
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingBlob(){
        new ContentItemImpl(ciUri,null,new SimpleMGraph()){};
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingMetadata(){
        new ContentItemImpl(ciUri,blob,null){};
    }
    
	@Test
	public void addingAndRetrieving() {
		ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
		UriRef partUri = new UriRef("http://foo/");
		Date someObject = new Date();
		ci.addPart(partUri, someObject);
		ci.getMetadata().add(new TripleImpl(ciUri, new UriRef("http://example.org/ontology#hasPart"), partUri));
        ci.getMetadata().add(new TripleImpl(partUri, new UriRef("http://example.org/ontology#isPartOf"),ciUri));
		Assert.assertEquals(someObject, ci.getPart(partUri, Date.class));
		Assert.assertEquals(someObject, ci.getPart(1, Date.class));
		Assert.assertEquals(partUri, ci.getPartUri(1));
		Assert.assertEquals(new UriRef(ciUri.getUnicodeString()+"_main"), ci.getPartUri(0));
		try {
		    ci.getPart(2, Object.class);
		    Assert.assertTrue("Requesting non existance part MUST throw an NoSuchPartException", false);
		} catch (NoSuchPartException e) {/* expected*/}
        try {
            ci.getPart(new UriRef("http://foo/nonexisting"), Object.class);
            Assert.assertTrue("Requesting non existance part MUST throw an NoSuchPartException", false);
        } catch (NoSuchPartException e) {/* expected*/}
        try {
            ci.getPartUri(2);
            Assert.assertTrue("Requesting non existance part MUST throw an NoSuchPartException", false);
        } catch (NoSuchPartException e) {/* expected*/}
		//finally log the toString
		log.info("toString: {}",ci);
	}
	@Test(expected=IllegalArgumentException.class)
	public void addPartWithoutUri(){
	    ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
	    ci.addPart(null, new Date());
	}
    @Test(expected=IllegalArgumentException.class)
    public void addPartWithoutPartContent(){
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        ci.addPart(new UriRef("http://foo/"), null);
    }
    /**
     * The ContentItem MUST NOT allow to replace the main content part (the
     * Blob stored at index 0)
     */
    @Test(expected=IllegalArgumentException.class)
    public void replaceMainPart(){
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        UriRef mainPart = ci.getPartUri(0);
        ci.addPart(mainPart, new Date());
    }
    @Test(expected=IllegalArgumentException.class)
    public void removeNullPart() {
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        ci.removePart(null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void removeNegaitveIndexPart() {
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        ci.removePart(-1);
    }
    @Test(expected=IllegalStateException.class)
    public void removeMainContentPartByUri() {
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        ci.removePart(ci.getPartUri(0));
    }
    @Test(expected=IllegalStateException.class)
    public void removeMainContentPartByIndex() {
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        ci.removePart(0);
    }
    @Test(expected=NoSuchPartException.class)
    public void removeNonExistentPartByUri() {
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        ci.removePart(new UriRef("urn:does.not.exist:and.can.not.be.removed"));
    }
    @Test(expected=NoSuchPartException.class)
    public void removeNonExistentPartByIndex() {
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        ci.removePart(12345);
    }
    @Test
    public void removeRemoveByUri() {
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        UriRef uri = new UriRef("urn:content.part:remove.test");
        ci.addPart(uri, new Date());
        try {
            ci.getPart(uri, Date.class);
        }catch (NoSuchPartException e) {
            Assert.assertFalse("The part with the uri "+uri+" was not added correctly",
                true);
        }
        ci.removePart(uri);
        try {
            ci.getPart(uri, Date.class);
            Assert.assertFalse("The part with the uri "+uri+" was not removed correctly",
                true);
        }catch (NoSuchPartException e) {
            // expected
        }
    }
    @Test
    public void removeRemoveByIndex() {
        ContentItem ci = new ContentItemImpl(ciUri,blob,new SimpleMGraph()){};
        UriRef uri = new UriRef("urn:content.part:remove.test");
        ci.addPart(uri, new Date());
        int index = -1;
        try {
            for(int i=0; index < 0; i++){
                UriRef partUri = ci.getPartUri(i);
                if(partUri.equals(uri)){
                    index = i;
                }
            }
        }catch (NoSuchPartException e) {
            Assert.assertFalse("The part with the uri "+uri+" was not added correctly",
                true);
        }
        ci.removePart(index);
        try {
            ci.getPart(index, Date.class);
            Assert.assertTrue("The part with the uri "+uri+" was not removed correctly",
                false);
        }catch (NoSuchPartException e) {
            // expected
        }
    }
    
}
