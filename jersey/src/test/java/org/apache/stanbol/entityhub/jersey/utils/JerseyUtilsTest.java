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
        Assert.assertTrue(JerseyUtils.testType(Map.class, new HashMap<String,String>().getClass()));
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
