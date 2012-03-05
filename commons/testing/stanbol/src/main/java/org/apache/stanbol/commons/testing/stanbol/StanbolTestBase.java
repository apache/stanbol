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
package org.apache.stanbol.commons.testing.stanbol;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.stanbol.commons.testing.http.RequestBuilder;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.commons.testing.jarexec.JarExecutor;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Stanbol integration tests - starts the runnable jar
 * to test if needed, and waits until server is ready before executing
 * the tests.
 */
public class StanbolTestBase {

    public static final String TEST_SERVER_URL_PROP = "test.server.url";
    public static final String SERVER_READY_TIMEOUT_PROP = "server.ready.timeout.seconds";
    public static final String SERVER_READY_PROP_PREFIX = "server.ready.path";
    public static final String KEEP_JAR_RUNNING_PROP = "keepJarRunning";

    protected static String serverBaseUrl;

    private static final Logger log = LoggerFactory.getLogger(StanbolTestBase.class);

    protected boolean serverReady = false;
    protected RequestBuilder builder;
    protected DefaultHttpClient httpClient = new DefaultHttpClient();
    protected RequestExecutor executor = new RequestExecutor(httpClient);

    @BeforeClass
    public static synchronized void startRunnableJar() throws Exception {
        if (serverBaseUrl != null) {
            // concurrent initialization by loading subclasses
            return;
        }
        final String configuredUrl = System.getProperty(TEST_SERVER_URL_PROP);
        if (configuredUrl != null) {
            serverBaseUrl = configuredUrl;
            log.info(TEST_SERVER_URL_PROP + " is set: not starting server jar (" + serverBaseUrl + ")");
        } else {
            final JarExecutor j = JarExecutor.getInstance(System.getProperties());
            j.start();
            serverBaseUrl = "http://localhost:" + j.getServerPort();
            log.info("Forked subprocess server listening to: " + serverBaseUrl);

            // Optionally block here so that the runnable jar stays up - we can
            // then run tests against it from another VM
            if ("true".equals(System.getProperty(KEEP_JAR_RUNNING_PROP))) {
                log.info(KEEP_JAR_RUNNING_PROP + " set to true - entering infinite loop"
                        + " so that runnable jar stays up. Kill this process to exit.");
                while (true) {
                    Thread.sleep(1000L);
                }
            }
        }
    }

    @Before
    public void waitForServerReady() throws Exception {
        // initialize instance request builder and HTTP client
        builder = new RequestBuilder(serverBaseUrl);
        httpClient = new DefaultHttpClient();
        executor = new RequestExecutor(httpClient);

        if (serverReady) {
            return;
        }

        // Timeout for readiness test
        final String sec = System.getProperty(SERVER_READY_TIMEOUT_PROP);
        final int timeoutSec = sec == null ? 60 : Integer.valueOf(sec);
        log.info("Will wait up to " + timeoutSec + " seconds for server to become ready");
        final long endTime = System.currentTimeMillis() + timeoutSec * 1000L;

        // Get the list of paths to test and expected content regexps
        final List<String> testPaths = new ArrayList<String>();
        final TreeSet<Object> propertyNames = new TreeSet<Object>();
        propertyNames.addAll(System.getProperties().keySet());
        for (Object o : propertyNames) {
            final String key = (String) o;
            if (key.startsWith(SERVER_READY_PROP_PREFIX)) {
                testPaths.add(System.getProperty(key));
            }
        }

        // Consider the server ready if it responds to a GET on each of 
        // our configured request paths with a 200 result and content
        // that matches the regexp supplied with the path
        long sleepTime = 100;
        readyLoop:
        while (!serverReady && System.currentTimeMillis() < endTime) {
            // Wait a bit between checks, to let the server come up
            Thread.sleep(sleepTime);
            sleepTime = Math.min(5000L, sleepTime * 2);

            // A test path is in the form path:substring or just path, in which case
            // we don't check that the content contains the substring 
            for (String p : testPaths) {
                final String[] s = p.split(":");
                final String path = s[0];
                final String substring = (s.length > 0 ? s[1] : null);
                final String url = serverBaseUrl + path;
                final HttpGet get = new HttpGet(url);
                for(int i = 2; i+1<s.length;i=i+2){
                    if(s[i] != null && !s[i].isEmpty() &&
                            s[i+1] != null && !s[i+1].isEmpty()){
                        get.setHeader(s[i], s[i+1]);
                    }
                }
                HttpEntity entity = null;
                try {
                    HttpResponse response = httpClient.execute(get);
                    entity = response.getEntity();
                    final int status = response.getStatusLine().getStatusCode();
                    if (status != 200) {
                        log.info("Got " + status + " at " + url + " - will retry");
                        continue readyLoop;
                    }

                    if (substring != null) {
                        if (entity == null) {
                            log.info("No entity returned for " + url + " - will retry");
                            continue readyLoop;
                        }
                        final String content = EntityUtils.toString(entity);
                        if (!content.contains(substring)) {
                            log.info("Returned content for " + url
                                    + " does not contain " + substring + " - will retry");
                            continue readyLoop;
                        }
                    }
                } catch (HttpHostConnectException e) {
                    log.info("Got HttpHostConnectException at " + url + " - will retry");
                    continue readyLoop;
                } finally {
                    if (entity != null) {
                        entity.consumeContent();
                    }
                }
            }
            serverReady = true;
            log.info("Got expected content for all configured requests, server is ready");
        }

        if (!serverReady) {
            throw new Exception("Server not ready after " + timeoutSec + " seconds");
        }
    }

}
