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
package org.apache.stanbol.contentorganizer.impl.naive;

import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.stanbol.contentorganizer.servicesapi.KnowledgeRetriever;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;

public class NaiveKnowledgeRetriever implements KnowledgeRetriever {

    private final String URI_REVYU_PAPER = "http://revyu.com/things/eswc-2008-paper-exposing-large-sitemaps";

    private final String URI_REVYU_EXTERNAL = "http://esw.w3.org/topic/TaskForces/CommunityProjects/LinkingOpenData/SemanticWebSearchEngines";

    private ONManager onMgr;

    private SessionManager sesMgr;

    public NaiveKnowledgeRetriever() {}

    public NaiveKnowledgeRetriever(ONManager onMgr, SessionManager sesMgr) {
        this.onMgr = onMgr;
        this.sesMgr = sesMgr;
    }

    @Override
    public Graph aggregateKnowledge(ContentItem ci) {
        ScopeRegistry registry = onMgr.getScopeRegistry();
        Set<OntologyScope> active = registry.getActiveScopes();
        // TODO Auto-generated method stub
        return null;
    }

}
