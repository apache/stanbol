package eu.iksproject.kres.manager;

import static org.junit.Assert.*;

import java.util.Hashtable;

import org.junit.BeforeClass;
import org.junit.Test;

import eu.iksproject.kres.api.manager.KReSONManager;

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
