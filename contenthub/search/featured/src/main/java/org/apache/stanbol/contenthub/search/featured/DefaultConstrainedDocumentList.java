/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.contenthub.search.featured;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.ConstrainedDocumentList;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Constraint;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Facet;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;

public class DefaultConstrainedDocumentList implements ConstrainedDocumentList {

    private String queryTerm;
    private FeaturedSearch featuredSearch;
    private List<UriRef> documentURIs;
    private Set<Constraint> constraints;
    private Set<Facet> facets;

    public DefaultConstrainedDocumentList(String queryTerm,
                                          QueryResponse queryResponse,
                                          Set<Constraint> constraints,
                                          FeaturedSearch featuredSearch) throws SearchException {
        this.queryTerm = queryTerm;
        this.featuredSearch = featuredSearch;
        parseQueryResponse(queryResponse);
        this.constraints = constraints;
    }

    @Override
    public List<UriRef> getDocuments() throws SearchException {
        return documentURIs;
    }

    private void parseQueryResponse(QueryResponse queryResponse) {
        // parse ids
        List<UriRef> ids = new ArrayList<UriRef>();
        SolrDocumentList documentList = queryResponse.getResults();
        for (SolrDocument solrDocument : documentList) {
            ids.add(new UriRef((String) solrDocument.getFieldValue(SolrFieldName.ID.toString())));
        }
        documentURIs = ids;

        // parse facets
        Set<Facet> facets = new HashSet<Facet>();
        for (FacetField facetField : queryResponse.getFacetFields()) {
            List<PlainLiteral> labels = new ArrayList<PlainLiteral>();
            labels.add(new PlainLiteralImpl(facetField.getName()));
            Facet facet = new FacetImpl(labels);
            facets.add(facet);
            List<Count> values = facetField.getValues();
            if (values != null) {
                for (Count count : facetField.getValues()) {
                    new ConstraintImpl(count.getName(), facet);
                }
            }
        }
        this.facets = facets;
    }

    @Override
    public Set<Constraint> getConstraints() {
        return this.constraints != null ? this.constraints : new HashSet<Constraint>();
    }

    @Override
    public Set<Facet> getFacets() {
        return this.facets;
    }

    public ConstrainedDocumentList subSet(int offset, int limit) throws SearchException {
        return featuredSearch.search(queryTerm, getConstraints(), offset, limit);
    }

    @Override
    public ConstrainedDocumentList narrow(Constraint constraint) throws SearchException {
        Set<Constraint> newConstraints = new HashSet<Constraint>(getConstraints());
        newConstraints.add(constraint);
        return featuredSearch.search(queryTerm, newConstraints);
    }

    @Override
    public ConstrainedDocumentList broaden(Constraint constraint) throws SearchException {
        Set<Constraint> newConstraints = new HashSet<Constraint>(getConstraints());
        newConstraints.remove(constraint);
        return featuredSearch.search(queryTerm, newConstraints);
    }
}
