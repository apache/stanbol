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
package org.apache.stanbol.enhancer.engines.htmlextractor.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * HtmlParser.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 *
 */
public class HtmlParser {

    /**
     * This contains the logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HtmlParser.class);

    private String baseURI = "";
    
    public HtmlParser() {
    }


    /**
     * @return the baseURI
     */
    public String getBaseURI() {
      return baseURI;
    }


    /**
     * @param baseURI the baseURI to set
     */
    public void setBaseURI(String baseURI) {
      this.baseURI = baseURI;
    }


    public Document getDOM(String html) {        
      if (html != null) {
        return getDOM(new ByteArrayInputStream(html.getBytes()), null);
      }
      return null;
    }

    public Document getDOM(InputStream html, String charset) {
        Document doc = null;
        try {
            doc = DOMBuilder.jsoup2DOM(Jsoup.parse(html, charset, baseURI));
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }
    
    public static void main(String[] args) throws Exception {
      int argv = 0;
      String encoding = null;
      while (argv < args.length && args[argv].startsWith("-")) {
        if (args[argv].equals("-enc")) {
          encoding = args[++argv];
        }
        ++argv;
      }
      HtmlParser parser = new HtmlParser();
      for (int i = argv; i < args.length; ++i) {
//        parser.setBaseURI(new File(args[i]).toURI().toString());
        InputStream is = new FileInputStream(args[i]);
        Document doc = parser.getDOM(is,encoding);
        OutputStream out = new FileOutputStream(new File(args[i]).getName()+".xml");
        DOMUtils.writeXml(doc,"UTF-8",null,out);
        out.close();
        is.close();
      }
    }
}
