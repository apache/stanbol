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
package org.apache.stanbol.commons.testing.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.LineIterator;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a Request and provides convenience methods
 * to validate the results.
 */
public class RequestExecutor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final CloseableHttpClient httpClient;
    private HttpRequestBase request;
    private HttpResponse response;
    private HttpEntity entity;
    private String contentString;
    private byte[] content;
    private ContentType contentType;
    private Charset charset;

//    /**
//     * HttpRequestInterceptor for preemptive authentication, based on httpclient
//     * 4.0 example
//     */
//    private static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
//
//        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
//
//            AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
//            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);
//            HttpHost targetHost = (HttpHost) context.getAttribute(HttpClientContext.HTTP_TARGET_HOST);
//
//            // If not auth scheme has been initialized yet
//            if (authState.getAuthScheme() == null) {
//                AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
//
//                // Obtain credentials matching the target host
//                Credentials creds = credsProvider.getCredentials(authScope);
//
//                // If found, generate BasicScheme preemptively
//                if (creds != null) {
//                    authState.update(new BasicScheme(), creds);
//                }
//            }
//        }
//    }

    public RequestExecutor(CloseableHttpClient client) {
        httpClient = client;
    }
    public RequestExecutor(CloseableHttpClient client, String username, String password) {
        httpClient = client;
    }

    public String toString() {
        if (request == null) {
            return "Request";
        }
        return request.getMethod() + " request to " + request.getURI();
    }
    /**
     * Executes a {@link Request} using this executor. <p>
     * Note that this cleans up all data of the previous executed request.
     * @param r the request to execute
     * @return this
     * @throws ClientProtocolException
     * @throws IOException
     */
    public RequestExecutor execute(Request r) throws ClientProtocolException, IOException {
        clear();
        request = r.getRequest();

        RequestConfig rc = RequestConfig.custom()
                //.setConnectionRequestTimeout(TODO)
                //.setConnectTimeout(TODO)
                .setRedirectsEnabled(r.getRedirects())
                .setRelativeRedirectsAllowed(true).build();
        request.setConfig(rc);
        // Execute request
        response = httpClient.execute(request);
        entity = response.getEntity();
        if (entity != null) {
            // We fully read the content every time, not super efficient but
            // how can we read it on demand while avoiding a (boring) cleanup() 
            // method on this class?
            content = EntityUtils.toByteArray(entity);
            contentType = ContentType.getOrDefault(entity);
            charset = contentType.getCharset();
            contentString = new String(content, charset != null ? charset : HTTP.DEF_CONTENT_CHARSET);
            //and close the stream
            entity.getContent().close();
        }
        return this;
    }

    protected void clear() {
        request = null;
        entity = null;
        contentType = null;
        charset = null;
        content = null;
        contentString = null;
        response = null;
    }

    /**
     * Verify that response matches supplied status
     */
    public RequestExecutor assertStatus(int expected) {
        assertNotNull(this.toString(), response);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(this + ": expecting status " + expected 
            + " (content: "+contentString+")", expected, status);
        return this;
    }

    /**
     * Verify that response matches supplied content type
     */
    public RequestExecutor assertContentType(String expected) {
        assertNotNull(this.toString(), response);
        if (entity == null) {
            fail(this + ": no entity in response, cannot check content type");
        }

        // And check for match
        assertEquals(this + ": expecting content type " + expected, expected, contentType.getMimeType());
        return this;
    }
    /**
     * Verify that response matches supplied charset
     */
    public RequestExecutor assertCharset(String expected) {
        assertNotNull(this.toString(), response);
        if (entity == null) {
            fail(this + ": no entity in response, cannot check content type");
        }
        assertEquals(this + ": expecting charset " + expected, 
            expected, charset == null ? null : charset.name());
        return this;
    }
    
    public RequestExecutor assertHeader(String key, String...values){
        assertNotNull(this.toString(),response);
        Set<String> expectedValues;
        if(values == null || values.length<1){
            expectedValues = null;
        } else {
            expectedValues = new HashSet<String>(Arrays.asList(values));
        }
        Header[] headers = response.getHeaders(key);
        if(headers.length < 1){
            headers = null;
        }
        if(expectedValues == null){
            assertTrue("The header "+key+" MUST NOT have any values (values: "+headers+")", 
                headers == null);
        } else {
            assertNotNull("There are no values for header "+key+"!", headers);
            for(Header header : headers){
                assertTrue("Unexpected header value "+header.getValue(),
                    expectedValues.remove(header.getValue()));
            }
            assertTrue("Missing header values "+expectedValues+"!",
                expectedValues.isEmpty());
        }
        return this;
    }

    /**
     * For each supplied regexp, fail unless content contains at
     * least one line that matches.
     * Regexps are automatically prefixed/suffixed with .* so as
     * to have match partial lines.
     */
    public RequestExecutor assertContentRegexp(String... regexp) {
        return assertContentRegexp(true,regexp);
    }
    /**
     * For each supplied regexp, fail unless <ul>
     * <li><code>expected</code>: content contains at least one line that matches.
     * <li><code>!expected</code>: content contains any line that mathces.
     * <ul>
     * Regexps are automatically prefixed/suffixed with .* so as
     * to have match partial lines.
     */
    public RequestExecutor assertContentRegexp(boolean expected, String... regexp) {
        assertNotNull(this.toString(), response);
        nextPattern:
        for (String expr : regexp) {
            final Pattern p = Pattern.compile(".*" + expr + ".*");
            final LineIterator it = new LineIterator(new StringReader(contentString));
            while (it.hasNext()) {
                final String line = it.nextLine();
                if (expected & p.matcher(line).matches()) {
                    continue nextPattern;
                }
                if(!expected & p.matcher(line).matches()) {
                    fail(this + ": match found for regexp '" + expr + "', content=\n" + contentString);
                }
            }
            if(expected){
                fail(this + ": no match for regexp '" + expr + "', content=\n" + contentString);
            }
        }
        return this;
    }

    /**
     * For each supplied string, fail unless content contains it
     */
    public RequestExecutor assertContentContains(String... expected) throws ParseException {
        assertNotNull(this.toString(), response);
        for (String exp : expected) {
            if (!contentString.contains(exp)) {
                fail(this + ": content does not contain '" + exp + "', content=\n" + contentString);
            }
        }
        return this;
    }

    public void generateDocumentation(RequestDocumentor documentor, String... metadata) throws IOException {
        documentor.generateDocumentation(this, metadata);
    }

    public HttpUriRequest getRequest() {
        return request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public HttpEntity getEntity() {
        return entity;
    }
    /**
     * Getter for an {@link InputStream} over the byte array containing the
     * data of the {@link HttpEntity}. This means that this method can be
     * called multiple times and the streams need not to be closed as
     * {@link ByteArrayInputStream} does not consume any System resources.
     * @return
     */
    public InputStream getStream() {
        return new ByteArrayInputStream(content);
    }
    
    /**
     * Getter for the String content of the HttpEntity.
     * @return
     */
    public String getContent() {
        if(contentString == null){
            contentString = new String(content, charset);
        }
        return contentString;
    }

    /**
     * @return the contentType
     */
    public final ContentType getContentType() {
        return contentType;
    }

    /**
     * @return the charset
     */
    public final Charset getCharset() {
        return charset;
    }
    
    public void close() {
        clear();
        try {
            httpClient.close();
        } catch (IOException e) {
            log.error("Error while closing Http Client", e);
        }
    }
}
