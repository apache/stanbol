package org.apache.stanbol.entityhub.core.model;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.test.model.ValueFactoryTest;
import org.junit.Before;


public class InMemoryValueFactoryTest extends ValueFactoryTest {
    
    private ValueFactory valueFactory;
    
    @Before
    public void init(){
     valueFactory = InMemoryValueFactory.getInstance();   
    }
    @Override
    protected Object getUnsupportedReferenceType() {
        return null; //indicates that all types are supported
    }
    
    @Override
    protected Object getUnsupportedTextType() {
        return null; //indicates that all types are supported
    }
    
    @Override
    protected ValueFactory getValueFactory() {
        return valueFactory;
    }
    
}
