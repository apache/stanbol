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

package org.apache.stanbol.contenthub.crawler.cnn.impl;

import java.net.URI;

/**
 * 
 * @author cihan
 * 
 */
public class NewsSummary {

    private URI newsURI;
    private String title;
    private String content;

    public void setNewsURI(URI newsURI) {
        this.newsURI = newsURI;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public URI getNewsURI() {
        return newsURI;
    }

    public String getContent() {
        return content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    /*public Map<String, List<Object>> getTitleConstraint() {
        if(title == null || title.isEmpty()) return null;
        Map<String, List<Object>> titleConstraint = new HashMap<String,List<Object>>();
        List<Object> titleList = new ArrayList<Object>(1);
        titleList.add(title);
        titleConstraint.put(SolrVocabulary.SolrFieldName.TITLE.toString(), titleList);
        return titleConstraint;
    }*/

}
