package org.apache.stanbol.contenthub.test;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Map;

import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.contenthub.crawler.cnn.CNNImporter;
import org.junit.Test;

public class CNNTest {
	
	@TestReference
	private CNNImporter cNNImporter;
	
	@Test
	public void testImportCNNNews(){
//		Map<URI, String> res = cNNImporter.importCNNNews("paris", 1, false);
//		assertNotNull("abc"+res.values(),res);
	}
}
