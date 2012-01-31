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

package org.apache.stanbol.contenthub.crawler.cnn;

import java.net.URI;
import java.util.Map;

/**
 * This is the interface to crawl CNN.
 * 
 * @see <a href=" http://topics.cnn.com/topics/">CNN News Topics</a>
 * 
 * 
 * @author cihan
 * 
 */
public interface CNNImporter {

    /**
     * 
     * @param topic
     *            The topic which will be crawled.
     * @param maxNumber
     *            Max number of news to be retrieved from CNN about the {@link topic}
     * @param fullNews
     *            If {@code true}, the topic will be crawled in detail to retrieve all information from CNN
     *            about the {@link topic}. If {@code false}, only summary of the news will be crawled and
     *            imported.
     * @return A map which includes the URI of the related topic and the news content. If {@link fullNews} is
     *         {@code true}, the news content is the full news; if not, it is the summary of the news.
     */
    Map<URI,String> importCNNNews(String topic, int maxNumber, boolean fullNews);

}
