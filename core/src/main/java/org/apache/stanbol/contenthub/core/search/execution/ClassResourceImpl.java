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

import org.apache.stanbol.contenthub.servicesapi.search.execution.ClassResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Implementation of {@link ClassResource}.
 * 
 * @author cihan
 * 
 */
public class ClassResourceImpl extends AbstractKeywordRelatedResource implements ClassResource {

    ClassResourceImpl(Node n,
                      EnhGraph g,
                      Double weight,
                      Double score,
                      Keyword relatedKeyword,
                      String classURI,
                      SearchContextFactoryImpl factory) {
        super(n, g, weight, score, relatedKeyword, factory);
        this.addProperty(RDF.type, SearchVocabulary.CLASS_RESOURCE);
        this.addProperty(SearchVocabulary.CLASS_URI, ResourceFactory.createResource(classURI));
    }

    @Override
    public String getClassURI() {
        return this.getPropertyValue(SearchVocabulary.CLASS_URI).asResource().getURI();
    }

}
