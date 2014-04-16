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
package org.apache.stanbol.entityhub.jersey.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests added mainly in response to the bug reported by STANBOL-727
 * @author Rupert Westenthaler
 *
 */
public class JerseyUtilsTest {

    private static class TestMap extends HashMap<String,Collection<? extends Number>>{
        private static final long serialVersionUID = 1L;
    }
    /**
     * Tests some combination for Test
     */
    @Test
    public void testType(){
        Assert.assertTrue(JerseyUtils.testType(Map.class, HashMap.class));
        Assert.assertFalse(JerseyUtils.testType(Map.class, HashSet.class));
        Assert.assertTrue(JerseyUtils.testType(Map.class, HashMap.class));
        Map<String,Collection<? extends Number>> genericMapTest = new TestMap();
        Assert.assertTrue(JerseyUtils.testType(Map.class, genericMapTest.getClass()));
        Assert.assertFalse(JerseyUtils.testType(Set.class, genericMapTest.getClass()));
        //test a parsed Type
        Assert.assertTrue(JerseyUtils.testType(Map.class, TestMap.class.getGenericSuperclass()));
    }
    
    @Test
    public void testParameterisedType() {
        //NOTE: this can not check for Collection<String>!!
        Assert.assertTrue(JerseyUtils.testParameterizedType(Map.class, 
            new Class[]{String.class,Collection.class}, TestMap.class));
    }
    
}
