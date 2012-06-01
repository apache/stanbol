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
import java.util.Map;
import java.util.Map.Entry;

public final class Utils {
    
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
     * @return the HTTP connection
     * @throws IOException if the connection to the parsed service could not be established.
     * @throws IllegalArgumentException if <code>null</code> is parsed as service URL
     */
    public static HttpURLConnection createPostRequest(URL serviceURL, Map<String,String> headers) throws IOException {
        if(serviceURL == null){
            throw new IllegalArgumentException("The parsed service URL MUST NOT be NULL!");
        }
        HttpURLConnection urlConn = (HttpURLConnection) serviceURL.openConnection();
        urlConn.setRequestMethod("POST");
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        if(headers != null){
            for(Entry<String,String> entry : headers.entrySet()){
                urlConn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        return urlConn;
    }

}
