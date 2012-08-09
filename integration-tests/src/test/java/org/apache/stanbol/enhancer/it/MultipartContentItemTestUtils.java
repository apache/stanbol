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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

public class MultipartContentItemTestUtils {

    public static String getHTMLContent(String...content){
        if(content == null || content.length<2){
            throw new IllegalArgumentException("The parsed content MUST have at lest two elements");
        }
        StringBuilder c = new StringBuilder();
        c.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        c.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" " +
                "lang=\"en\" dir=\"ltr\">\n");
        c.append("<head>\n");
        c.append("<meta http-equiv=\"Content-Type\" content=\"text/html; " +
                "charset=utf-8\" />\n");
        c.append("<title>").append(content[0]).append("</title>\n");
        c.append("  <meta http-equiv=\"Content-Type\" content=\"text/html; " +
                "charset=utf-8\" />\n");
        c.append("<style type=\"text/css\">");
        c.append("#headbox {\n");
        c.append("    background: none repeat scroll 0 0 white;\n");
        c.append("    border-bottom: 3px solid black;\n");
        c.append("    width: 100%;\n");
        c.append("}\n");
        c.append("</style>\n");
        c.append("</head>\n");
        c.append("<body>\n");
        c.append("<div class=\"content\">\n");
        c.append("<h2>").append(content[0]).append("</h2>\n");
        for(int i=1;i<content.length;i++){
        c.append("<p>").append(content[i]).append("</p>\n");
        }
        c.append("</div>\n");
        c.append("</body>\n");
        c.append("</html>\n");
        return c.toString();        
    }
    
    
    /**
     * Build an path from the supplied path and 
     * query parameters.
     *
     * @param queryParameters an even number of Strings, each pair
     * of values represents the key and value of a query parameter.
     * Keys and values are encoded by this method.
     */
    public static String buildPathWithParams(String path, String... queryParameters) {
        final StringBuilder sb = new StringBuilder();
        if (queryParameters == null || queryParameters.length == 0) {
            sb.append(path);
        } else if (queryParameters.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of queryParameters arguments ("
                    + queryParameters.length + "), must be even");
        } else {
            final List<NameValuePair> p = new ArrayList<NameValuePair>();
            for (int i = 0; i < queryParameters.length; i += 2) {
                p.add(new BasicNameValuePair(queryParameters[i], queryParameters[i + 1]));
            }
            sb.append(path);
            sb.append("?");
            sb.append(URLEncodedUtils.format(p, "UTF-8"));
        }

        return sb.toString();
    }
}
