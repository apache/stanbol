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
package org.apache.stanbol.commons.solr.utils;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase.StringStream;

/**
 * Extend the default Solr QueryRequest to make it possible to pass MoreLikeThis the payload as a content
 * stream.
 * 
 * This is temporary solution waiting for a proper support of MoreLikeThisHandler in SolrJ.
 * https://issues.apache.org/jira/browse/SOLR-1085
 */
public class StreamQueryRequest extends QueryRequest {

    private static final long serialVersionUID = 1L;

    protected ContentStream contentStream;

    public StreamQueryRequest(SolrQuery q) {
        super(q, METHOD.POST);
        String[] bodies = q.remove(CommonParams.STREAM_BODY);
        if (bodies != null && bodies.length > 0) {
            String body = StringUtils.join(bodies, " ");
            this.contentStream = new StringStream(body);
        }
    }

    @Override
    public Collection<ContentStream> getContentStreams() {
        return Arrays.asList(contentStream);
    }
}
