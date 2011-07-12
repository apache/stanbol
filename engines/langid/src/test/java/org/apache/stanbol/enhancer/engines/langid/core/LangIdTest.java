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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.tika.language.LanguageIdentifier;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link LangIdTest} is a test class for {@link TextCategorizer}.
 *
 * @author Joerg Steffen, DFKI
 * @version $Id$
 */
public class LangIdTest {

    /**
     * This contains the text categorizer to test.
     */
  
    /**
     * This initializes the text categorizer.
     */
    @BeforeClass
    public static void oneTimeSetUp() throws IOException {
        LanguageIdentifier.initProfiles();
    }

    /**
     * Tests the language identification.
     *
     * @throws IOException if there is an error when reading the text
     */
    @Test
    public void testLangId() throws IOException {
        String testFileName = "en.txt";

        InputStream in = this.getClass().getClassLoader().getResourceAsStream(
                testFileName);
        assertNotNull("failed to load resource " + testFileName, in);

        String text = IOUtils.toString(in);
        LanguageIdentifier tc = new LanguageIdentifier(text);
        String language = tc.getLanguage();
        assertEquals("en", language);
    }

}
