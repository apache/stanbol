package org.apache.stanbol.enhancer.serviceapi.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentReference;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.impl.UrlReference;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentReferenceTest {
    
    private static final String TEST_RESOURCE_NAME = "contentReferece_test.txt";
    private static final String TEST_RESOURCE_CONTENT = "Used to test ContentReference!";
    private static URL testURL;
    
    @BeforeClass
    public static void initURL(){
        testURL = ContentReferenceTest.class.getClassLoader().getResource(TEST_RESOURCE_NAME);
        assertNotNull("Unable to load test resource '"
            +TEST_RESOURCE_NAME+" via Classpath!",testURL);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void missingReferenceString(){
        new UrlReference((String)null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingReferenceURL(){
        new UrlReference((URL)null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void emptyReferenceString(){
        new UrlReference("");
    }
    @Test(expected=IllegalArgumentException.class)
    public void relativeReferenceString(){
        new UrlReference("relative/path/to/some.resource");
    }
    @Test(expected=IllegalArgumentException.class)
    public void unknownProtocolReferenceString(){
        new UrlReference("unknownProt://test.example.org/some.resource");
    }

    @Test
    public void testUrlReference() throws IOException{
        ContentReference ref = new UrlReference(testURL);
        assertNotNull(ref);
        assertEquals(ref.getReference(), testURL.toString());
        ContentSource source = ref.dereference();
        assertNotNull(source);
        String content = IOUtils.toString(source.getStream(), "UTF-8");
        assertNotNull(content);
        assertEquals(TEST_RESOURCE_CONTENT, content);

        //same as above, but by using ContentSource.getData() instead of
        //ContentSource.getStream()
        ref = new UrlReference(testURL);
        assertNotNull(ref);
        assertEquals(ref.getReference(), testURL.toString());
        source = ref.dereference();
        assertNotNull(source);
        content = new String(source.getData(),"UTF-8");
        assertNotNull(content);
        assertEquals(TEST_RESOURCE_CONTENT, content);

        //test the constructor that takes a String
        ref = new UrlReference(testURL.toString());
        assertNotNull(ref);
        assertEquals(ref.getReference(), testURL.toString());
        source = ref.dereference();
        assertNotNull(source);
        content = IOUtils.toString(source.getStream(), "UTF-8");
        assertNotNull(content);
        assertEquals(TEST_RESOURCE_CONTENT, content);
    }

}
