/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.shared.dependency.hermit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author elvio
 */
public class CheckHermiTTest {

    public CheckHermiTTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getCkHermiT method, of class CheckHermiT.
     */
    @Test
    public void testGetCkHermiT() {
        CheckHermiT check = new CheckHermiT();
        if(check.getCkHermiT()){
            assertEquals(check.getCkHermiT(),true);
        }else{
            fail("Problem with Hermit");
        }
    }

}