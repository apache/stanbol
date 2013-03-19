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
package org.apache.stanbol.enhancer.engines.celi;

import static org.apache.stanbol.enhancer.engines.celi.langid.impl.CeliLanguageIdentifierEnhancementEngineTest.CELI_LANGID_SERVICE_URL;

import java.io.IOException;
import java.net.URL;

import javax.xml.soap.SOAPException;

import org.apache.stanbol.enhancer.engines.celi.langid.impl.LanguageIdentifierClientHTTP;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CeliHttpTest {
    
    public static final Logger log = LoggerFactory.getLogger(CeliHttpTest.class);

    /**
     * None Existing user account should throw an IOException with 
     * HTTP Error 401 Unauthorized.
     * 
     * @throws IOException
     * @throws SOAPException
     */
    @Test(expected=IOException.class)
    public void testNonExistentAccountAuthentification() throws IOException, SOAPException {
        LanguageIdentifierClientHTTP testClient = new LanguageIdentifierClientHTTP(
            new URL(CELI_LANGID_SERVICE_URL), "nonexistent:useraccount",5);
        testClient.guessQueryLanguage("This is a dummy request");
    }
    /**
     * Also illegal formatted user account are expected to throw a IOException
     * with a HTTP status code 4** (Bad Request)
     * 
     * @throws IOException
     * @throws SOAPException
     */
    @Test(expected=IOException.class)
    public void testIllegalFormattedAuthentification() throws IOException, SOAPException {
        LanguageIdentifierClientHTTP testClient = new LanguageIdentifierClientHTTP(
            new URL(CELI_LANGID_SERVICE_URL), "illeagalFormatted",5);
        testClient.guessQueryLanguage("This is a dummy request");
    }
    /**
     * Also illegal formatted user account are expected to throw a IOException
     * with a HTTP status code 4** (Bad Request)
     * 
     * @throws IOException
     * @throws SOAPException
     */
    @Test
    public void testTestAccount() throws IOException, SOAPException {
        LanguageIdentifierClientHTTP testClient = new LanguageIdentifierClientHTTP(
            new URL(CELI_LANGID_SERVICE_URL), null,5);
        try {
            Assert.assertNotNull(testClient.guessQueryLanguage("This is a dummy request"));
        } catch (IOException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
        }
    }
    
}
