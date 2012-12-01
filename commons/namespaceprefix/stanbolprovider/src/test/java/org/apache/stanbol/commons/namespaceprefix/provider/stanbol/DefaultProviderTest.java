package org.apache.stanbol.commons.namespaceprefix.provider.stanbol;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.namespaceprefix.mappings.DefaultNamespaceMappingsEnum;
import org.apache.stanbol.commons.namespaceprefix.service.StanbolNamespacePrefixService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultProviderTest {

    private static NamespacePrefixService service;

    @BeforeClass
    public static void init() throws IOException {
        //this test for now does not use predefined mappings
        URL mappingURL = DefaultProviderTest.class.getClassLoader()
                .getResource("testnamespaceprefix.mappings");
        //Assert.assertNotNull(mappingURL);
        File mappingFile;
        if(mappingURL != null){
            try {
              mappingFile = new File(mappingURL.toURI());
            } catch(URISyntaxException e) {
              mappingFile = new File(mappingURL.getPath());
            }
            //Assert.assertTrue(mappingFile.isFile());
        } else {
            mappingFile = new File("testnamespaceprefix.mappings");
        }
        service = new StanbolNamespacePrefixService(mappingFile);
    }

    @Test
    public void testDefaultMappings(){
        //this tests both the implementation of the getNamespace and getPrefix mappings
        //and that the defaultNamespaceMappingProvider is correctly loaded by the ServiceLoader 
        for(DefaultNamespaceMappingsEnum defaultMapping : DefaultNamespaceMappingsEnum.values()){
            Assert.assertEquals(defaultMapping.getNamespace(), 
                service.getNamespace(defaultMapping.getPrefix()));
            Assert.assertTrue(service.getPrefixes(defaultMapping.getNamespace()).contains(defaultMapping.getPrefix()));
        }
    }
}
