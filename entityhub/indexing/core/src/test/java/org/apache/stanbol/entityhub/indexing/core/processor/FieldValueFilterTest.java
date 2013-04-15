package org.apache.stanbol.entityhub.indexing.core.processor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.impl.NamespacePrefixProviderImpl;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldValueFilterTest {
    private static final String FB = "http://rdf.freebase.com/ns/";

    private static final String TEST_CONFIG = "prefix.config";
    
    private static ValueFactory vf = InMemoryValueFactory.getInstance();
    
    private static NamespacePrefixProvider nsPrefixProvider;

    private static final Map<String,String> nsMappings = new HashMap<String,String>();
    static {
        nsMappings.put("fb", FB);
        nsMappings.put("rdf", NamespaceEnum.rdf.getNamespace());
        nsMappings.put("rdfs", NamespaceEnum.rdfs.getNamespace());
        nsMappings.put("skos", NamespaceEnum.skos.getNamespace());
        nsMappings.put("foaf", NamespaceEnum.foaf.getNamespace());
    }
    
    
    
    @BeforeClass
    public static void init() throws IOException{
        nsPrefixProvider = new NamespacePrefixProviderImpl(nsMappings);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testIncludeExcludeConfig1(){
        new FieldValueFilter(nsPrefixProvider,"rdf:type","foaf:Person;skos:Concept;!skos:Concept");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testIncludeExcludeConfig2(){
        new FieldValueFilter(nsPrefixProvider,"rdf:type","foaf:Person;!skos:Concept;skos:Concept");
    }

    
    
    @Test
    public void testIncludeConfig(){
        EntityProcessor filter = new FieldValueFilter(nsPrefixProvider,"rdf:type","foaf:Person");
        
        Representation r = getRepresentation(NamespaceEnum.foaf+"Person");
        Assert.assertNotNull(filter.process(r));
        
        r = getRepresentation(NamespaceEnum.skos+"Concept");
        Assert.assertNull(filter.process(r));
        
        r = getRepresentation(NamespaceEnum.skos+"Concept", NamespaceEnum.foaf+"Person");
        Assert.assertNotNull(filter.process(r));
        
        //test empty value
        filter = new FieldValueFilter(nsPrefixProvider,"skos:releated","");
        Assert.assertNotNull(filter.process(r));
        
        filter = new FieldValueFilter(nsPrefixProvider,"skos:releated","null");
        Assert.assertNotNull(filter.process(r));

        filter = new FieldValueFilter(nsPrefixProvider,"skos:releated", null);
        Assert.assertNotNull(filter.process(r));
    }
    
    @Test
    public void testExcludeConfig(){
        EntityProcessor filter = new FieldValueFilter(nsPrefixProvider,"rdf:type","*;!foaf:Person");
        
        Representation r = getRepresentation(NamespaceEnum.foaf+"Person");
        Assert.assertNull(filter.process(r));
        
        r = getRepresentation(NamespaceEnum.skos+"Concept");
        Assert.assertNotNull(filter.process(r));
        
        r = getRepresentation(NamespaceEnum.skos+"Concept", NamespaceEnum.foaf+"Person");
        Assert.assertNotNull(filter.process(r));
        
        //test empty value
        filter = new FieldValueFilter(nsPrefixProvider,"skos:releated","*;!null");
        Assert.assertNull(filter.process(r));
        
        filter = new FieldValueFilter(nsPrefixProvider,"skos:releated","*;!");
        Assert.assertNull(filter.process(r));

        filter = new FieldValueFilter(nsPrefixProvider,"skos:releated", "*;!;!foaf:Person");
        Assert.assertNull(filter.process(r));
    }

    private Representation getRepresentation(String...types){
        Representation r = vf.createRepresentation("urn:test");
        for(String type : types){
            r.add(NamespaceEnum.rdf+"type", vf.createReference(type));
        }
        return r;
    }


}

