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
package org.apache.stanbol.entityhub.indexing.core.processor;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.YardBackend;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * Uses the {@link IndexingDestination#getYard()} as LDPath {@link RDFBackend}
 * for the execution of configured LDPath statements.<p>
 * <b>NOTE</b> in contrast to the {@link LdpathProcessor} this implementation
 * is not limited to a subset of ldpath programs.<p>
 * Typical use cases of this processor include:<ul>
 * <li> indexing transitive closures (e.g. "
 *   <code>skos:broaderTransitive = (skos:broader)*</code>")
 * <li> collecting labels of referenced entities to be used for disambiguation
 *   (e.g. use lables of linked concepts in a SKOS concept scheme : 
 *   "<code> <urn:disambiguate.label> = *[rdf:type is skos:Concept]/(skos:prefLabel | skos:altLabel)<code>")
 * <li> advanced indexing rules that need paths longer than one (e.g. adding
 *   labels of redirects pointing to an entity 
 *   "<code> rdfs:label = rdfs:label | (^rdfs:seeAlso/rdfs:label)</code>")
 * </ul>
 * <p>
 * The focus on post-processing allows an easy configuration as the
 * data source needs not to be configured, but is directly retrieved from
 * the {@link IndexingDestination}. Note that this also means that if this 
 * processor is not used in the post-processing state results are unpredictable
 * as they will depend on the indexing order of the entities!
 * 
 * @author Rupert Westenthaler
 *
 */
public class LdpathPostProcessor extends LdpathProcessor implements EntityProcessor {

    @Override
    public void setConfiguration(Map<String,Object> config) {
        super.setConfiguration(config);
    }

    @Override
    public boolean needsInitialisation() {
        return true;
    }

    @Override
    public void initialise() {
        //override the ldpath instance used for the initialisation with
        //the one using the IndexingDestination
        //this is OK, because parsing ldpath programs anyway does only need
        //the "value factory" role of the RDFBackend and does not actually
        //access any data.
        Yard yard = indexingConfig.getIndexingDestination().getYard();
        YardBackend backend = new YardBackend(yard);
        this.ldPath = new EntityhubLDPath(backend,yard.getValueFactory());
    }


}
