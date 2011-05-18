package org.apache.stanbol.entityhub.yard.solr.impl;

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
        if (bodies!= null && bodies.length > 0) {
            String body = StringUtils.join(bodies, " ");
            this.contentStream = new StringStream(body);
        }
    }

    @Override
    public Collection<ContentStream> getContentStreams() {
        return Arrays.asList(contentStream);
    }
}
