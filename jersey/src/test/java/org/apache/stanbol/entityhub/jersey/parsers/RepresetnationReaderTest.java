package org.apache.stanbol.entityhub.jersey.parsers;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.representation.Form;
import com.sun.jersey.server.impl.provider.RuntimeDelegateImpl;
import javax.ws.rs.ext.RuntimeDelegate;

public class RepresetnationReaderTest {

    Logger log = LoggerFactory.getLogger(RepresetnationReaderTest.class);
    
    RepresentationReader reader = new RepresentationReader();
    
    /**
     * Tests the bug reported by STANBOL-727
     */
    @Test
    public void testIsReadable(){
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
        //NOTE the use of com.sun.* API for unit testing
        Class<Form> formClass = com.sun.jersey.api.representation.Form.class;
        boolean state = reader.isReadable(formClass, formClass, null, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Assert.assertFalse(state);
    }
}
