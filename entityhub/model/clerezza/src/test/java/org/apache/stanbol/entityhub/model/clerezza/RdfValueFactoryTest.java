package org.apache.stanbol.entityhub.model.clerezza;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.test.model.ValueFactoryTest;
import org.junit.Before;
import org.junit.Test;


public class RdfValueFactoryTest extends ValueFactoryTest {
    
    protected RdfValueFactory valueFactory;
    
    @Before
    public void init(){
        this.valueFactory = RdfValueFactory.getInstance();
    }
    
    @Override
    protected Object getUnsupportedReferenceType() {
        return null; //all references are supported (no test for valid IRIs are done by Clerezza)
    }
    
    @Override
    protected Object getUnsupportedTextType() {
        return null; //all Types are supported
    }
    
    @Override
    protected ValueFactory getValueFactory() {
        return valueFactory;
    }
    @Test(expected=NullPointerException.class)
    public void testNullNodeRepresentation() {
        SimpleMGraph graph = new SimpleMGraph();
        valueFactory.createRdfRepresentation(null, graph);
    }
    @Test(expected=NullPointerException.class)
    public void testNullGraphRepresentation() {
        UriRef rootNode = new UriRef("urn:test.rootNode");
        valueFactory.createRdfRepresentation(rootNode, null);
    }
    
}
