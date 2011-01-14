package eu.iksproject.rick.core.model;

import org.junit.Before;

import eu.iksproject.rick.servicesapi.model.ValueFactory;
import eu.iksproject.rick.test.model.RepresentationTest;

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
