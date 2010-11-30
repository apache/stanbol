/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.shared.dependency.owlapi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author elvio
 */
public class CheckOwlApiTest {

    public CheckOwlApiTest() {
    }

    @org.junit.BeforeClass
    public static void setUpClass() throws Exception {
    }

    @org.junit.AfterClass
    public static void tearDownClass() throws Exception {
    }

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

   /* @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }*/

    /**
     * Test of getCkOwl method, of class CheckOwlApi.
     */
    @org.junit.Test
    public void testOwl() {
        // TODO review the generated test code and remove the default call to fail.
        CheckOwlApi check = new CheckOwlApi();
        if(check.getCkOwl()){
            assertEquals(check.getCkOwl(),true);
        }else{
            fail("Problem with OwlApi");
        }
    }

}