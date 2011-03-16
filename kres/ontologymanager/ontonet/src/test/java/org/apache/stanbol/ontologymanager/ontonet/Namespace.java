package org.apache.stanbol.ontologymanager.ontonet;

import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManager;
import org.junit.BeforeClass;
import org.junit.Test;

public class Namespace {
	
	private static KReSONManager onm;
	
	@BeforeClass
	public static void setUp() {
		// An ONManager with no store and default settings
		onm = new ONManager(null, new Hashtable<String, Object>());
	}
	
	@Test
	public void getNamespace() {
		assertNotNull(onm.getKReSNamespace());
	}

}
