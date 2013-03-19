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
package org.apache.stanbol.enhancer.engines.celi.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {
    
    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    
    private Utils(){}
    
    /**
     * The maximum size of the preix/suffix for the selection context
     */
    private static final int DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE = 50;
    
    /**
     * Extracts the selection context based on the content, selection and
     * the start char offset of the selection
     * @param content the content
     * @param selection the selected text
     * @param selectionStartPos the start char position of the selection
     * @return the context
     */
    public static String getSelectionContext(String content, String selection,int selectionStartPos){
        //extract the selection context
        int beginPos;
        if(selectionStartPos <= DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE){
            beginPos = 0;
        } else {
            int start = selectionStartPos-DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            beginPos = content.indexOf(' ',start);
            if(beginPos < 0 || beginPos >= selectionStartPos){ //no words
                beginPos = start; //begin within a word
            }
        }
        int endPos;
        if(selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE >= content.length()){
            endPos = content.length();
        } else {
            int start = selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            endPos = content.lastIndexOf(' ', start);
            if(endPos <= selectionStartPos+selection.length()){
                endPos = start; //end within a word;
            }
        }
        return content.substring(beginPos, endPos);
    }
    /**
     * Creates a POST Request with the parsed URL and optional headers
     * @param serviceURL the service URL
     * @param headers optional header
     * @param conTimeout the connection timeout in seconds. If &lt;= 0 the default
     * (30sec) is used.
     * @return the HTTP connection
     * @throws IOException if the connection to the parsed service could not be established.
     * @throws IllegalArgumentException if <code>null</code> is parsed as service URL
     */
    public static HttpURLConnection createPostRequest(URL serviceURL, Map<String,String> headers, int conTimeout) throws IOException {
        if(serviceURL == null){
            throw new IllegalArgumentException("The parsed service URL MUST NOT be NULL!");
        }
        HttpURLConnection urlConn = (HttpURLConnection) serviceURL.openConnection();
        urlConn.setRequestMethod("POST");
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        if(conTimeout < 0){
            conTimeout = CeliConstants.DEFAULT_CONECTION_TIMEOUT;
        }
        urlConn.setConnectTimeout(conTimeout*1000);
        urlConn.setReadTimeout(conTimeout*1000);
        if(headers != null){
            for(Entry<String,String> entry : headers.entrySet()){
                urlConn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        return urlConn;
    }
    /**
     * Parses the {@link CeliConstants#CELI_LICENSE} form the configuration and
     * the Environment. Also checks for {@link CeliConstants#CELI_TEST_ACCOUNT}
     * if no license key is configured.
     * @param configuration the configuration of the CELI engine
     * @param ctx the {@link BundleContext} used to read the configuration of
     * the environment.
     * @return The license key or <code>null</code> if no license key is configured
     * but <code>{@link CeliConstants#CELI_TEST_ACCOUNT}=true</code>.
     * @throws ConfigurationException if no {@link CeliConstants#CELI_LICENSE} 
     * is configured and {@link CeliConstants#CELI_TEST_ACCOUNT} is not present
     * or not set to <code>true</code>.
     */
    public static String getLicenseKey(Dictionary<String,Object> configuration,BundleContext ctx) throws ConfigurationException {
        String licenseKey = (String) configuration.get(CeliConstants.CELI_LICENSE);
        if (licenseKey == null || licenseKey.isEmpty()) {
            licenseKey = ctx.getProperty(CeliConstants.CELI_LICENSE);
        }
        if (licenseKey == null || licenseKey.isEmpty()) {
            Object value = configuration.get(CeliConstants.CELI_TEST_ACCOUNT);
            if(value == null){
                value = ctx.getProperty(CeliConstants.CELI_TEST_ACCOUNT);
            }
            if(value == null || !Boolean.parseBoolean(value.toString())){
                throw new ConfigurationException(CeliConstants.CELI_LICENSE,
                    "The CELI License Key is a required configuration. To test the "
                    + "CELI engines you can also activate the test account by setting '"
                    + CeliConstants.CELI_TEST_ACCOUNT+"=true'. This account is limited "
                    +"to 100 requests pre day and IP address.");
            } else {
                log.warn("no CELI license key configured for this Engine, a guest account will be used (max 100 requests per day). Go on http://linguagrid.org for getting a proper license key.");
            }
        }
        return licenseKey;
    }
    /**
     * Retrieves the connection timeout from the enignes configuration
     * @param configuration the configuration of the CELI engine
     * @param ctx the {@link BundleContext} used to read the configuration of
     * the environment.
     * @return The connection timeout or <code>-1</code> if none is configured
     */
    public static int getConnectionTimeout(Dictionary<String,Object> configuration,BundleContext ctx) throws ConfigurationException {
        Object value = configuration.get(CeliConstants.CELI_CONNECTION_TIMEOUT);
        int timeout = -1;
        if (value instanceof Number){
            timeout = ((Number)value).intValue();
        } else if(value != null){
            try {
                timeout = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(CeliConstants.CELI_CONNECTION_TIMEOUT, 
                    "The configured value '"+value+"'is not a valid integer",e);
            }
        } else {
            value = ctx.getProperty(CeliConstants.CELI_TEST_ACCOUNT);
            if(value != null){
                try {
                    timeout = Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(CeliConstants.CELI_CONNECTION_TIMEOUT, 
                        "The configured value '"+value+"' taken from the system properties is not a valid integer",e);
                }
            }
        }
        return timeout;
    }
}
