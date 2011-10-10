package org.apache.stanbol.explanation.impl;

import static org.junit.Assert.*;

import java.util.Hashtable;

import org.apache.stanbol.explanation.MockOsgiContext;
import org.apache.stanbol.explanation.api.ExplanationGenerator;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link ExplanationGenerator} implementations.
 */
public class TestGenerator {

    private static OfflineConfiguration config;

    @BeforeClass
    public static void setup() {
        config = new OfflineConfigurationImpl(new Hashtable<String,Object>());
    }

    @Test
    public void testInit() throws Exception {
        // ONManager onm = new ONManagerImpl(null, null, config, new Hashtable<String,Object>());
        ExplanationGenerator gen = new ExplanationGeneratorImpl(new ExplanationEnvironmentConfiguration(
                MockOsgiContext.onManager, new Hashtable<String,Object>()), null,
                new Hashtable<String,Object>());
        assertNotNull(gen);
    }
}
