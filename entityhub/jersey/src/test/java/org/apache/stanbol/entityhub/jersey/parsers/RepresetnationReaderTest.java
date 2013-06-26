/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.entityhub.jersey.parsers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.representation.Form;
import com.sun.jersey.server.impl.provider.RuntimeDelegateImpl;

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
