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
package org.apache.stanbol.entityhub.indexing.core.source;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Implementation of the {@link EntityScoreProvider} interface based on a
 * {@link Map} 
 * @author Rupert Westenthaler
 */
public class MapEntityScoreProvider implements EntityScoreProvider {
    /**
     * The map with the rankings
     */
    private Map<String,Float> rankings;
    /**
     * Ranking based entity Evaluator.<p>
     * Note that Entities with rankings of <code>null</code> or 
     * <code>&lt; 0</code> will not be indexed.
     * @param rankings the map holding the rankings
     * @param normaliser the ScoreNormaliser used to normalise scores or <code>null</code>
     * to return the scores as present in the map.
     * @throws IllegalArgumentException if the ranking map is <code>null</code>
     * or empty and if the parsed minimum ranking is <code> &lt; 0</code>.
     */
    public MapEntityScoreProvider(Map<String,Float> rankings) throws IllegalArgumentException{
        if(rankings == null || rankings.isEmpty()){
            throw new IllegalArgumentException("The map with the rankings MUST NOT be NULL or empty");
        }
        this.rankings = rankings;
    }
    @Override
    public void setConfiguration(Map<String,Object> config) {
        throw new UnsupportedOperationException("Map based configuration is not supported by this implementation!");
    }
    @Override
    public boolean needsInitialisation() {
        return false;
    }
    @Override
    public void initialise() {
        // nothing to do
    }
    @Override
    public void close() {
        //do not remove the elements because the map might be also used by others
        this.rankings = null;
    }
    /**
     * Returns <code>false</code> because this implementation does not need the
     * data of the Entities
     * @see EntityScoreProvider#needsData()
     */
    @Override
    public boolean needsData() {
        return false;
    }

    @Override
    public Float process(String id) {
        return rankings.get(id);
    }

    @Override
    public Float process(Representation entity) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This Class uses process(String id) for evaluation");
    }

}
