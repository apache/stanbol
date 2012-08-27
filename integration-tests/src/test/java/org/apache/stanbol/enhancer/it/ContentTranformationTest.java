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
package org.apache.stanbol.enhancer.it;

import static org.apache.stanbol.enhancer.it.MultipartContentItemTestUtils.getHTMLContent;

import java.io.IOException;

import org.junit.Test;
/**
 * This tests RESTful API extensions to the Stanbol Enhancer as described by
 * STANBOL-481
 */
public class ContentTranformationTest extends EnhancerTestBase {

    public static final String[] TEXT_CONTENT  = new String[]{
       "Stanbol Content Transformation",
       "The multipart content API of Apache Stanbol allows to directly " +
       "request transformed content by adding the \"omitMetadata=true\" " +
       "query parameter and setting the \"Accept\" header to the target" +
       "content type.",
       "This feature can be used with any enhancement chain that " +
       "incudles an Engine that provides the required content transcoding" +
       "functionality. However because extracted metadata are omitted by" +
       "such requests it is best used with enhancement chains that only" +
       "contains such engines."
    };

    
    public ContentTranformationTest() {
        //for now use the language chain to test transforming
        super(getChainEndpoint("language"),"tika","langdetect");
    }
    
    @Test
    public void testHtml2PlainText() throws IOException {
        executor.execute(
            builder.buildPostRequest(getEndpoint()+"?omitMetadata=true")
            .withHeader("Accept","text/plain")
            .withContent(getHTMLContent(TEXT_CONTENT))
        )
        .assertStatus(200)
        .assertContentType("text/plain")
        .assertContentContains(TEXT_CONTENT);
        
    }
}
