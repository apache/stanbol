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
 * Implementation of an {@link EntityScoreProvider} that parsed the same value
 * for all entities. The default value is '1' but it can be configured by using
 * {@link #PARAM_SCORE}("{@value #PARAM_SCORE}").
 * 
 * @author Rupert Westenthaler
 *
 */
public class StaticEntityScoreProvider implements EntityScoreProvider {

    public static final String PARAM_SCORE = "score";
    public static final Float DEFAULT_SCORE = Float.valueOf(1f);
    
    private Float score = DEFAULT_SCORE;
    
    public StaticEntityScoreProvider() {
        this(null);
    }
    public StaticEntityScoreProvider(Float score){
        setScore(score);
    }
    /**
     * @param score
     */
    private void setScore(Float score) {
        if(score == null){
            this.score = DEFAULT_SCORE;
        } else if (score <= 0){
            throw new IllegalArgumentException("The parsed Score MUST NOT be <= 0!");
        } else {
           this.score = score; 
        }
    }
    
    @Override
    public boolean needsData() {
        return false;
    }

    @Override
    public Float process(String id) {
        return score;
    }

    @Override
    public Float process(Representation entity) {
        return score;
    }

    @Override
    public void close() {
    }

    @Override
    public void initialise() {
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        Object value = config.get(PARAM_SCORE);
        if(value != null && !value.toString().isEmpty()){
            try {
                setScore(Float.valueOf(value.toString())); 
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                    "Unable to parse score from value {}",value),e);
            }
        }
    }

}
