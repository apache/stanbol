/**
 * 
 */
package org.apache.stanbol.demos.integritycheck.test;

import static org.junit.Assert.*;

import org.apache.stanbol.demos.integritycheck.IntegrityCheckFragment;
import org.junit.Before;
import org.junit.Test;

/**
 * @author enridaga
 *
 */
public class IntegrityCheckFragmentTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * TODO This is a stub test. Useful tests for WebFragments/Templates?
	 * Test method for {@link org.apache.stanbol.demos.integritycheck.IntegrityCheckFragment#getName()}.
	 */
	@Test
	public final void testGetName() {
		IntegrityCheckFragment fragment = new IntegrityCheckFragment();
		assertTrue(fragment.getName().equals("integritycheck"));
	}

}
