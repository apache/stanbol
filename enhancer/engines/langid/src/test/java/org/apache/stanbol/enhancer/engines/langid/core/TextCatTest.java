package org.apache.stanbol.enhancer.engines.langid.core;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knallgrau.utils.textcat.TextCategorizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * {@link TextCatTest} is a test class for {@link TextCategorizer}.
 *
 * @author Joerg Steffen, DFKI
 * @version $Id$
 */
public class TextCatTest {

    /**
     * This contains the text categorizer to test.
     */
    private static TextCategorizer tc;

    /**
     * This initializes the text categorizer.
     */
    @BeforeClass
    public static void oneTimeSetUp() {
        tc = new TextCategorizer();
    }

    /**
     * This test the language identification.
     *
     * @throws IOException
     *             if there is an error when reading the text
     */
    @Test
    public void testTextCat() throws IOException {
        String testFileName = "en.txt";

        InputStream in =
            this.getClass().getClassLoader().getResourceAsStream(
                testFileName);
        assertNotNull("failed to load resource " + testFileName, in);
        String text = IOUtils.toString(in);
        assertEquals("en", tc.categorize(text));
    }

}
