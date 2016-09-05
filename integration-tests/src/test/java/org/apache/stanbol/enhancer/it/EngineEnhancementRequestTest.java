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
 * Tests sending EnhancementRequests to single Engines
 *
 */
public class EngineEnhancementRequestTest extends EnhancerTestBase {
    
    public EngineEnhancementRequestTest(){
        super();
    }

    /**
     * Tests an normal enhancement request directed to the tika engine 
     * @throws Exception
     */
    @Test
    public void testTikaMetadata() throws Exception {
        InputStream in = EngineEnhancementRequestTest.class.getClassLoader().getResourceAsStream("testJPEG_EXIF.jpg");
        Assert.assertNotNull("Unable to find test resource 'testJPEG_EXIF.jpg'",in);
        executor.execute(
            builder.buildPostRequest(getEndpoint()+"/engine/tika")
            .withHeader("Accept","text/rdf+nt")
            .withEntity(new InputStreamEntity(in, -1))
        )
        .assertStatus(200)
        .assertContentRegexp( //we need not test the extraction results here
            //only that the Enhancer REST API works also with engines!
            "<http://purl.org/dc/terms/format> \"image/jpeg\"",
            "<http://www.w3.org/ns/ma-ont#hasKeyword> \"serbor\"",
            "<http://www.semanticdesktop.org/ontologies/2007/05/10/nexif#isoSpeedRatings> \"400\""
        );
    }
    /**
     * Tests plain text extraction for an request directly sent to the tika 
     * engine 
     * @throws Exception
     */
    @Test
    public void testPlainTextExtraction() throws Exception {
        InputStream in = EngineEnhancementRequestTest.class.getClassLoader().getResourceAsStream("test.pdf");
        Assert.assertNotNull("Unable to find test resource 'test.pdf'",in);
        executor.execute(
            builder.buildPostRequest(getEndpoint()+"/engine/tika?omitMetadata=true")
            .withHeader("Accept","text/plain")
            .withEntity(new InputStreamEntity(in, -1))
        )
        .assertStatus(200)
        .assertContentRegexp( //we need not test the extraction results here
            //only that the Enhancer REST API works also with engines!
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris"
        );
    }

}
