package org.apache.stanbol.commons.namespaceprefix.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.namespaceprefix.service.StanbolNamespacePrefixService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class StanbolNamespacePrefixServiceTest {

    private static NamespacePrefixService service;

    @BeforeClass
    public static void init() throws IOException {
        URL mappingURL = StanbolNamespacePrefixServiceTest.class.getClassLoader()
                .getResource("testnamespaceprefix.mappings");
        Assert.assertNotNull(mappingURL);
        File mappingFile;
        try {
          mappingFile = new File(mappingURL.toURI());
        } catch(URISyntaxException e) {
          mappingFile = new File(mappingURL.getPath());
        }
        Assert.assertTrue(mappingFile.isFile());
        service = new StanbolNamespacePrefixService(mappingFile);
    }

    @Test
    public void testInitialisation(){
        //this tests the namespaces defined in namespaceprefix.mappings file
        Assert.assertEquals("http://www.example.org/test#", service.getNamespace("test")); 
        Assert.assertEquals("http://www.example.org/test-1/",service.getNamespace("test-1"));
        Assert.assertEquals("urn:example.text:",service.getNamespace("urn_test"));
    }
    
    @Test 
    public void testMultiplePrefixesForNamespace(){
        List<String> prefixes = service.getPrefixes("http://www.example.org/test#");
        Assert.assertEquals(2, prefixes.size());
        Assert.assertEquals("test",prefixes.get(0));
        Assert.assertEquals("test2",prefixes.get(1));
        Assert.assertEquals("http://www.example.org/test#", service.getNamespace("test"));
        Assert.assertEquals("http://www.example.org/test#", service.getNamespace("test2"));
    }
    @Test
    public void testConversionMethods(){
        Assert.assertEquals("http://www.example.org/test#localname", 
            service.getFullName("test:localname"));
        Assert.assertEquals("http://www.example.org/test#localname:test", 
            service.getFullName("test:localname:test"));
        Assert.assertEquals("http://www.example.org/test#localname", 
            service.getFullName("http://www.example.org/test#localname"));

        Assert.assertEquals("urn:example.text:localname", 
            service.getFullName("urn_test:localname"));
        Assert.assertEquals("urn:example.text:localname/test", 
            service.getFullName("urn_test:localname/test"));
        Assert.assertEquals("urn:example.text:localname", 
            service.getFullName("urn:example.text:localname"));

        Assert.assertNull(service.getFullName("nonExistentPrefix:localname"));
    }
}
