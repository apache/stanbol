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

package org.apache.stanbol.contenthub.core.search.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.stanbol.contenthub.servicesapi.search.execution.DocumentResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContextFactory;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.query.BindingQueryPlan;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.impl.OntModelImpl;
import com.hp.hpl.jena.query.larq.IndexBuilderString;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * 
 * @author anil.sinaci
 * 
 */
public class SearchContextImpl extends OntModelImpl implements SearchContext {
    private static final Logger log = LoggerFactory.getLogger(SearchContextImpl.class);

    private Map<String,List<Object>> constraints;

    private final SearchContextFactoryImpl factory;
    private final List<String> allowedEngines;

    private OntModel searchModel;
    private IndexBuilderString userGraphIndexBuilder = null;

    public SearchContextImpl(OntModel ontModel,
                             String[] queryKeywords,
                             List<String> allowedEngines,
                             Map<String,List<Object>> constraints) {
        super(OntModelSpec.OWL_DL_MEM_RDFS_INF);

        this.searchModel = ontModel;
        this.constraints = constraints;
        this.allowedEngines = allowedEngines;

        if (this.searchModel != null) {
            // Index the user graph
            this.userGraphIndexBuilder = new IndexBuilderString(SearchVocabulary.HAS_LOCAL_NAME);
            this.userGraphIndexBuilder.indexStatements(this.searchModel.listStatements());
            // Do not forget to set the default index just before the query execution according to its context
            // LARQ.setDefaultIndex(index);
        }

        factory = new SearchContextFactoryImpl();
        factory.setSearchContext(this);
        for (String queryKeyword : queryKeywords) {
            factory.createQueryKeyword(queryKeyword.toLowerCase());
        }
    }

    @Override
    public List<QueryKeyword> getQueryKeyWords() {
        List<QueryKeyword> qkws = new ArrayList<QueryKeyword>();
        for (Resource res : this.listResourcesWithProperty(RDF.type, SearchVocabulary.QUERY_KEYWORD).toList()) {
            qkws.add(factory.getQueryKeyword(res.getURI()));
        }
        return Collections.unmodifiableList(qkws);
    }

    @Override
    public SearchContextFactory getFactory() {
        return factory;
    }

    @Override
    public OntModel getSearchModel() {
        return this.searchModel;
    }

    @Override
    public <T extends RDFNode> ExtendedIterator<T> queryFor(BindingQueryPlan query,
                                                            List<BindingQueryPlan> altQueries,
                                                            Class<T> asKey) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAllowedEngines() {
        return Collections.unmodifiableList(allowedEngines);
    }

    @Override
    public Map<String,List<Object>> getConstraints() {
        return constraints;
    }

    @Override
    public IndexLARQ getIndex() {
        if (userGraphIndexBuilder == null) {
            log.warn("There is no index on user model available for this search.");
            return null;
        }
        return userGraphIndexBuilder.getIndex();
    }

    @Override
    public List<String> getDocumentIDs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DocumentResource> getDocuments() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String,List<Object>> getFacets() {
        return getConstraints();
    }

}
