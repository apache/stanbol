package org.apache.stanbol.ontologymanager.ontonet;

import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class Namespace {
	
	private static ONManager onm;
	
	@BeforeClass
	public static void setUp() {
		// An ONManagerImpl with no store and default settings
		onm = new ONManagerImpl(null,null, new Hashtable<String, Object>());
	}
	
	@Test
	public void getNamespace() {
		assertNotNull(onm.getKReSNamespace());
	}

}
