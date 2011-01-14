package eu.iksproject.rick.core.model;

import org.junit.Before;

import eu.iksproject.rick.servicesapi.model.ValueFactory;
import eu.iksproject.rick.test.model.ValueFactoryTest;

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
