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
package org.apache.stanbol.commons.testing.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

import org.apache.commons.io.LineIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.util.EntityUtils;

/** Executes a Request and provides convenience methods
 *  to validate the results.
 */
public class RequestExecutor {
    private final HttpClient httpClient;
    private HttpResponse response;
    private HttpEntity entity;
    private String content;
    
    public RequestExecutor(HttpClient client) {
        httpClient = client;
    }
    
    public RequestExecutor execute(Request r) throws ClientProtocolException, IOException {
        response = httpClient.execute(r.getRequest());
        entity = response.getEntity();
        if(entity != null) {
            // We fully read the content every time, not super efficient but
            // how can we read it on demand while avoiding a (boring) cleanup() 
            // method on this class?
            content = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        }
        return this;
    }

    /** Verify that response matches supplied status */
    public RequestExecutor assertStatus(int expected) {
        assertNotNull(response);
        assertEquals("Expecting status " + expected, expected, response.getStatusLine().getStatusCode());
        return this;
    }

    /** For each supplied regexp, fail unless content contains at 
     *  least one line that matches.
     *  Regexps are automatically prefixed/suffixed with .* so as
     *  to have match partial lines.
     */
    public RequestExecutor assertContentRegexp(String... regexp) {
        assertNotNull(response);
        nextPattern:
        for(String expr : regexp) {
            final Pattern p = Pattern.compile(".*" + expr + ".*");
            final LineIterator it = new LineIterator(new StringReader(content));
            while(it.hasNext()) {
                final String line = it.nextLine(); 
                if(p.matcher(line).matches()) {
                    continue nextPattern;
                }
            }
            fail("No match for regexp '" + expr + "', content=\n" + content);
        }
        return this;
    }

    /** For each supplied string, fail unless content contains it */
    public RequestExecutor assertContentContains(String... expected) throws ParseException, IOException {
        assertNotNull(response);
        for(String exp : expected) {
            if(!content.contains(exp)) {
                fail("Content does not contain '" + exp + "', content=\n" + content);
            }
        }
        return this;
    }
}
