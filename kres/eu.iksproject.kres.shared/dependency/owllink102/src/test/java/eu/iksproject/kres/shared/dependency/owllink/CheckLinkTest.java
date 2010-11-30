/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.shared.dependency.owllink;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author elvio
 */
public class CheckLinkTest {

    public CheckLinkTest() {
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

    /*@Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }*/

    /**
     * Test of getCkLink method, of class CheckLink.
     */
    @org.junit.Test
    public void testGetCkLink() {
        CheckLink check = new CheckLink();
        if(check.getCkLink()){
            assertEquals(check.getCkLink(),true);
        }else{
            fail("Problem with owl link");
        }
    }

}