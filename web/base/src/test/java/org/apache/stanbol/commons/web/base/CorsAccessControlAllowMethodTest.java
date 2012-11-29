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
package org.apache.stanbol.commons.web.base;

import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;

import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.wink.common.internal.MultivaluedMapImpl;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests issue reported/fix for STANBOL-616
 * 
 * @author Rupert Westenthaler
 * 
 */
public class CorsAccessControlAllowMethodTest {

    @Test
    public void testAccessControlAllowMethodTest() {
        ServletContext context = new MockServletContext();
        context.setAttribute(CorsConstants.CORS_ORIGIN, Collections.singleton("*"));
        MultivaluedMap<String,String> header = new MultivaluedMapImpl();
        header.add("Origin", "https://issues.apache.org/jira/browse/STANBOL-616");
        header.put("Access-Control-Request-Headers", Arrays.asList("Origin", "Content-Type", "Accept"));
        header.add("Access-Control-Request-Method", "PUT");
        HttpHeaders requestHeaders = new MockHttpHeaders(header);

        ResponseBuilder builder = Response.ok("Test");
        CorsHelper.enableCORS(context, builder, requestHeaders, OPTIONS, GET, POST, PUT);
        Response response = builder.build();
        MultivaluedMap<String,Object> metadata = response.getMetadata();
        Assert.assertTrue("'Access-Control-Allow-Headers' expected",
            metadata.containsKey("Access-Control-Allow-Headers"));
        String value = (String) metadata.getFirst("Access-Control-Allow-Headers");
        Assert.assertTrue("'Access-Control-Allow-Headers' does not contain the expected values",
            value.contains("Origin") && value.contains("Content-Type") && value.contains("Accept"));
        Assert.assertTrue("'Access-Control-Allow-Origin' expected",
            metadata.containsKey("Access-Control-Allow-Origin"));
        Assert.assertEquals("'Access-Control-Allow-Origin' does not have the expected value '*'", "*",
            metadata.getFirst("Access-Control-Allow-Origin"));
        Assert.assertTrue("'Access-Control-Allow-Methods' expected",
            metadata.containsKey("Access-Control-Allow-Methods"));
        value = (String) metadata.getFirst("Access-Control-Allow-Methods");
        Assert.assertTrue("'Access-Control-Allow-Methods' does not contain the expected values",
            value.contains(OPTIONS) && value.contains(GET) && value.contains(POST) && value.contains(PUT));
        Assert.assertTrue("'Access-Control-Expose-Headers' expected",
            metadata.containsKey("Access-Control-Expose-Headers"));
        value = (String) metadata.getFirst("Access-Control-Expose-Headers");
        Assert.assertTrue("'Access-Control-Expose-Headers' does not contain the expected valur 'Location'",
            value.contains("Location"));
    }

}
