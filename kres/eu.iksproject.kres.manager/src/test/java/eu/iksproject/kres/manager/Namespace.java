package eu.iksproject.kres.manager;

import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;


public class Namespace {
	
	
	static ONManager onManager;
	
	@BeforeClass
	public static void setUp() {
		
		onManager = ONManager.get();
		
	}
	
	@Test
	public static void getNamespace() {
		
	}

}
