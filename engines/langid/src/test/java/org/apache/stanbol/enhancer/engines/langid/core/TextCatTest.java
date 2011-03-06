/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.enhancer.engines.langid.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
    private static Properties langMap = new Properties();

    /**
     * This initializes the text categorizer.
     */
    @BeforeClass
    public static void oneTimeSetUp() throws IOException {
        tc = new TextCategorizer();
        InputStream in = tc.getClass().getClassLoader().getResourceAsStream("languageLabelsMap.txt");
        langMap.load(in);
    }

    /**
     * Tests the language identification.
     *
     * @throws IOException if there is an error when reading the text
     */
    @Test
    public void testTextCat() throws IOException {
        String testFileName = "en.txt";

        InputStream in = this.getClass().getClassLoader().getResourceAsStream(
                testFileName);
        assertNotNull("failed to load resource " + testFileName, in);

        String text = IOUtils.toString(in);
        String language = tc.categorize(text);
        assertEquals("en", langMap.getProperty(language, language));
    }

}
