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
package org.apache.stanbol.entityhub.indexing.core.normaliser;

import java.util.Map;


/**
 * This default implementation returns the parsed value. Intended to be used
 * in cases where parsing <code>null</code> as {@link ScoreNormaliser} is not
 * supported for some reason.
 * @author Rupert Westenthaler
 */
public class DefaultNormaliser implements ScoreNormaliser{

    
    private ScoreNormaliser normaliser;

    @Override
    public Float normalise(Float score) {
        if(normaliser != null){
            score = normaliser.normalise(score);
        }
        return score;
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        Object value = config.get(CHAINED_SCORE_NORMALISER);
        if(value != null){
            this.normaliser = (ScoreNormaliser) value;
        }
    }

    @Override
    public ScoreNormaliser getChained() {
        return normaliser;
    }

}
