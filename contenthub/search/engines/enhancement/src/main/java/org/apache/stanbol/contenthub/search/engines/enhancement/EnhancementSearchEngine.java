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

package org.apache.stanbol.contenthub.search.engines.enhancement;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.core.utils.indexing.EnhancementLARQ;
import org.apache.stanbol.contenthub.search.engines.enhancement.model.EnhancementPool;
import org.apache.stanbol.contenthub.search.engines.enhancement.model.EnhancementRepresentation;
import org.apache.stanbol.contenthub.search.engines.enhancement.model.EntityRepresentation;
import org.apache.stanbol.contenthub.servicesapi.search.engine.EngineProperties;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngineException;
import org.apache.stanbol.contenthub.servicesapi.search.execution.DocumentResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.ExternalResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.query.larq.LARQ;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * 
 * @author cihan
 * 
 */
@Component
@Service
public class EnhancementSearchEngine implements SearchEngine, EngineProperties {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancementSearchEngine.class);

    private static final Map<String,Object> properties;
    static {
        properties = new HashMap<String,Object>();
        properties.put(PROCESSING_ORDER, PROCESSING_DEFAULT);
    }

    Model enhancementGraph = null;

    @Override
    public void search(SearchContext searchContext) throws SearchEngineException {
        // FIXME: can be done in activator, but DOES ENHANCEMENTGRAPH CHANGES AFTER ACTIVATION?
        enhancementGraph = EnhancementLARQ.getInstance().getEnhancementModel();
        if (enhancementGraph != null) {
            for (QueryKeyword qk : searchContext.getQueryKeyWords()) {
                searchForKeyword(qk, searchContext);
                for (Keyword kw : qk.getRelatedKeywords()) {
                    searchForKeyword(kw, searchContext);
                }
            }
        } else {
            LOGGER.info("EnhancementGraph could not be obtained, skipped enhancement Search Engine");
        }

    }

    private void searchForKeyword(Keyword keyword, SearchContext context) {
        LOGGER.debug("Started searching for keyword {} ", keyword.getKeyword());
        String query = QueryGenerator.keywordBasedAnnotationQuery(keyword.getKeyword());
        IndexLARQ index = EnhancementLARQ.getInstance().getIndex();
        if (index != null) {
            QueryExecution qExec = QueryExecutionFactory.create(query, enhancementGraph);
            LARQ.setDefaultIndex(qExec.getContext(), index);
            ResultSet result = qExec.execSelect();
            processResults(result, keyword, context);
        } else {
            LOGGER.info("LARQ Index could not be obtained from Enhancement Listener Factory");
        }
    }

    private void processResults(ResultSet result, Keyword keyword, SearchContext context) {
        SearchContextFactory scf = context.getFactory();
        while (result.hasNext()) {
            QuerySolution resultBinding = result.nextSolution();
            String label = resultBinding.getLiteral("label").getString();
            double score = resultBinding.getLiteral("score").getDouble();
            String document = resultBinding.getResource("document").getURI();
            String type = resultBinding.getResource("type").getURI();
            String selectionText = resultBinding.getLiteral("text").getString();
            String ref = resultBinding.getResource("ref").getURI();
            double refScore = resultBinding.getLiteral("refscore").getDouble();

            EnhancementRepresentation er = EnhancementPool.getEnhancementRepresentation(label, document);
            EntityRepresentation etr = EnhancementPool.getEntityRepresentation(ref, label, document);
            er.setScore((score > 10 ? 1 : score / 10.0));
            er.setSelectionText(selectionText);
            etr.setScore((refScore > 10 ? 1 : refScore / 10.0));
            etr.getTypes().add(type);

        }

        for (EnhancementRepresentation er : EnhancementPool.getAll()) {
            DocumentResource dr = scf.createDocumentResource(er.getDocument(), 1.0, er.getScore(), keyword,
                er.getSelectionText());
            for (EntityRepresentation etr : er.getExternalResources()) {
                ExternalResource exr = scf.createExternalResource(etr.getRef(), 1.0, etr.getScore(), dr
                        .getRelatedKeywords().get(0));
                for (String type : etr.getTypes()) {
                    exr.addType(type);
                    exr.addRelatedDocument(dr);
                }
            }
        }

        EnhancementPool.clear();
    }

    @Override
    public Map<String,Object> getEngineProperties() {
        return properties;
    }

}
