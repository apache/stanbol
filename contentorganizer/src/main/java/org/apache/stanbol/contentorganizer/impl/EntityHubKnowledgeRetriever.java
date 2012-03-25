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
package org.apache.stanbol.contentorganizer.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contentorganizer.servicesapi.KnowledgeRetriever;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;

public class EntityHubKnowledgeRetriever implements KnowledgeRetriever {

    private final String REFERENCE_FIELD = "reference_t";

    private ReferencedSiteManager siteMgr;

    public EntityHubKnowledgeRetriever(ReferencedSiteManager siteMgr) {
        this.siteMgr = siteMgr;
    }

    @Override
    public TripleCollection aggregateKnowledge(ContentItem ci) {

        MGraph result = new IndexedMGraph();

        Set<String> references = new HashSet<String>();

        // TODO I'm falling back on this because I cannot seem to enrich the metadata graph.
        if (ci instanceof SolrContentItem) {
            SolrContentItem sci = (SolrContentItem) ci;
            Object obj = sci.getConstraints().get(REFERENCE_FIELD);
            if (obj != null && obj instanceof Collection<?>) for (Object s : (Collection<?>) obj)
                references.add(s.toString());
        }

        for (String uri : references) {
            // String uri = "http://revyu.com/things/eswc-2008-paper-exposing-large-sitemaps";
            // uri =
            // "http://esw.w3.org/topic/TaskForces/CommunityProjects/LinkingOpenData/SemanticWebSearchEngines";
            // uri = "http://revyu.com/people/tom";
            Entity signature = siteMgr.getEntity(uri);
//            System.out.println("Searching for signature of " + uri);
            if (signature != null) {
                RdfRepresentation rdfSignature = RdfValueFactory.getInstance().toRdfRepresentation(
                    signature.getRepresentation());
//                System.out.println("Signature of " + rdfSignature.getId() + " has "
//                                   + rdfSignature.getRdfGraph().size() + " triples ");
                // System.out.println(rdfSignature.getRdfGraph());
                result.addAll(rdfSignature.getRdfGraph());
            }  //else System.out.println("Not found.");
        }

        return result;

    }
}
