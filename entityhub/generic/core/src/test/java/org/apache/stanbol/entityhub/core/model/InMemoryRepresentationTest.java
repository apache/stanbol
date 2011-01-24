package org.apache.stanbol.entityhub.core.model;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.test.model.RepresentationTest;
import org.junit.Before;


public class InMemoryRepresentationTest extends RepresentationTest {
    
    private ValueFactory valueFactory;
    
    @Before
    public void init(){
     valueFactory = InMemoryValueFactory.getInstance();   
    }
    
    @Override
    protected ValueFactory getValueFactory() {
        return valueFactory;
    }

    @Override
    protected Object getUnsupportedValueInstance() {
        return null; //indicates that all kinds of Objects are supported!
    }
 
}
