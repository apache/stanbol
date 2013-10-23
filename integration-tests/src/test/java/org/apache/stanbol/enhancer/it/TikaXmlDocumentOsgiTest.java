/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.enhancer.it;

import java.io.InputStream;

import org.apache.http.entity.InputStreamEntity;
import org.junit.Assert;
import org.junit.Test;
/**
 * This test sends an XML based document to the Tika engine to test if
 * XML processing works as expected when Tika is running in the
 * OSGI environment (see 
 * <a href="https://issues.apache.org/jira/browse/STANBOL-810">STANBOL-810</a>
 * for details).
 *
 */
public class TikaXmlDocumentOsgiTest extends EnhancerTestBase {
    
    public TikaXmlDocumentOsgiTest(){
        super();
    }

    /**
     * Tests docx format
     * engine 
     * @throws Exception
     */
    @Test
    public void testDocx() throws Exception {
        InputStream in = EngineEnhancementRequestTest.class.getClassLoader().getResourceAsStream("testWORD.docx");
        Assert.assertNotNull("Unable to find test resource 'testWORD.docx'",in);
        executor.execute(
            builder.buildPostRequest(getEndpoint()+"/engine/tika?omitMetadata=true")
            .withHeader("Accept","text/plain")
            .withHeader("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .withEntity(new InputStreamEntity(in, -1))
        )
        .assertStatus(200); //not interested in the results, just that it worked
    }
    /**
     * Tests docx format
     * engine 
     * @throws Exception
     */
    @Test
    public void testDocx2() throws Exception {
        InputStream in = EngineEnhancementRequestTest.class.getClassLoader().getResourceAsStream("Vorlage_Protokoll.docx");
        Assert.assertNotNull("Unable to find test resource 'Vorlage_Protokoll.docx'",in);
        executor.execute(
            builder.buildPostRequest(getEndpoint()+"/engine/tika?omitMetadata=true")
            .withHeader("Accept","text/plain")
            .withHeader("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .withEntity(new InputStreamEntity(in, -1))
        )
        .assertStatus(200); //not interested in the results, just that it worked
    }
}
