package org.apache.stanbol.reasoners.jena;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.stanbol.reasoners.jena.filters.PropertyFilter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Statement;
/**
 * This class tests the PropertyFilter
 */
public class PropertyFilterTest {

	private static final Logger log = LoggerFactory
			.getLogger(PropertyFilterTest.class);

	@Test
	public void test() {
		PropertyFilter filter = new PropertyFilter(
				TestData.foaf_firstname);
		log.info("Testing the {} class", filter.getClass());
		Set<Statement> output = TestData.alexdma.getModel().listStatements()
				.filterKeep(filter).toSet();
		for(Statement statement : output){
			assertTrue(statement.getPredicate().equals(TestData.foaf_firstname));
		}
	}
}
