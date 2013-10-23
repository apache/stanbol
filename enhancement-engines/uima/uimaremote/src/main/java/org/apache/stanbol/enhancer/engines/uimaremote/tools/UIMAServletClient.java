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
package org.apache.stanbol.enhancer.engines.uimaremote.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import org.apache.stanbol.commons.caslight.FeatureStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This class is an HTTP client for processing an UIMA Simple Servlet Result
 *
 * @author Mihály Héder
 */
public class UIMAServletClient {
    
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Queries the UIMA Simple Servlet and returns the FeatureSet list.
     * @param servletURI The uri of the servlet
     * @param sourceName The source name of this processor
     * @param types The TypeConfigMap for the generated FeatureSets
     * @param input The Sofa String
     * @return The generated FeatureSet list
     */
   public List<FeatureStructure> getFSList(String servletURI,String sourceName,String input) {
        try {
            if (input == null) {
               logger.error("input (sofaString) is null!");
               return null;
            }
            // Construct data
            String data = URLEncoder.encode("text", "UTF-8") + "=" + URLEncoder.encode(input, "UTF-8");
            data += "&" + URLEncoder.encode("mode", "UTF-8") + "=" + URLEncoder.encode("xml", "UTF-8");
            // Send data
            URL url = new URL(servletURI);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            XMLReader xr = XMLReaderFactory.createXMLReader();
            SaxUIMAServletResult2Offsets handler = new SaxUIMAServletResult2Offsets();
            handler.setSourceName(sourceName);
          
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.parse(new InputSource(rd));
            wr.close();
            rd.close();
            
            List<FeatureStructure> fsList = handler.getFsList();
            
            for (FeatureStructure fs:fsList) {
                fs.setCoveredText(fs.getSofaChunk(input));
            }
            
            return fsList;

        } catch (SAXException ex) {
            logger.error("Error in UIMAClient", ex);
        } catch (IOException ex) {
            logger.error("Error in UIMAClient", ex);
        }
        return null;

   }
}
