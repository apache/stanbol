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
package org.apache.stanbol.commons.stanboltools.datafileprovider.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProviderEvent;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProviderLog;

/** (Felix) OSGi console plugin that displays DataFileProvider events */ 
@SuppressWarnings("serial")
@Component
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name="felix.webconsole.label", value="stanbol_datafileprovider", propertyPrivate=true),
    @Property(name="felix.webconsole.title", value="Stanbol Data File Provider", propertyPrivate=true)
})
public class WebConsolePlugin extends HttpServlet {

    @Reference
    private DataFileProviderLog dataFileProviderLog;
    
    @Reference
    private DataFileProvider dataFileProvider;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        final PrintWriter pw = response.getWriter();
        
        pw.println("<p class='statline ui-state-highlight'>");
        pw.println("Displaying the last " + dataFileProviderLog.size() + " DataFileProvider events");

        String dfPath = "<PATH NOT FOUND??>";
        if(dataFileProvider instanceof MainDataFileProvider) {
            dfPath = ((MainDataFileProvider)dataFileProvider).getDataFilesFolder().getAbsolutePath();
        }
            
        pw.println("<br/>");
        pw.println("Data files found in the " + dfPath + " folder have precedence");
        
        pw.println("<br/>");
        pw.println("The main DataFileProvider is " + dataFileProvider.getClass().getName());
        
        pw.println("</p>");
        
        pw.println("<table class='nicetable'>");
        
        final String [] labels = {
                "timestamp",
                "bundle/filename",
                "actual location/download info"
        };
        
        pw.println("<thead><tr>");
        for(String label : labels) {
            cell("th", pw, null, label);
        }
        pw.println("</tr></thead><tbody>");
        
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
        
        for(DataFileProviderEvent e : dataFileProviderLog) {
            pw.println("<tr>");
            cell(pw, null, fmt.format(e.getTimestamp()));
            cell(pw, null, e.getBundleSymbolicName(), "b", e.getFilename());

            final StringBuilder sb = new StringBuilder();
            for(Map.Entry<?, ?> comment : e.getComments().entrySet()) {
                if (sb.length() > 0) {
                    sb.append("<br/>");
                }
                sb.append(comment.getKey());
                sb.append(": ");
                sb.append(comment.getValue());
            }
            
            cell(pw, 
                    null, e.getActualFileLocation(), 
                    "i" , sb.toString());
            pw.println("</tr>");
        }
        pw.println("</tbody></table>");
    }
    
    private static void cell(PrintWriter pw, String...content) {
        cell("td", pw, content);
    }

    /** 
     * Content parameters: tags at even indexes, content at odd indexes. If
     * content is <code>null</code> than both tags and content are ignored.
     */
    private static void cell(String tag, PrintWriter pw, String...content) {
        pw.print("<");
        pw.print(tag);
        pw.print(">");
        
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(int i=0; i < content.length; i+= 2) {
            if(!first) {
                sb.append("<br/>\n");
            }
            String value = content[i+1];
            if(value != null){
                final String lineTag = content[i];
                if(lineTag != null) {
                    sb.append("<").append(lineTag).append(">");
                }
                sb.append(value);
                if(lineTag != null) {
                    sb.append("</").append(lineTag).append(">");
                }
                first = false;
            }
        } 
        
        pw.print(sb.toString());
        pw.print("<");
        pw.print(tag);
        pw.print(">");
    }
}