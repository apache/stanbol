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
package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.opensextant.solrtexttagger.TagClusterReducer;
import org.opensextant.solrtexttagger.TagLL;
import org.opensextant.solrtexttagger.Tagger;

/**
 * Allow to use multiple {@link TagClusterReducer} with a {@link Tagger}
 * instance.
 * @author Rupert Westenthaler
 *
 */
public class ChainedTagClusterReducer implements TagClusterReducer {
    
    private final TagClusterReducer[] reducers;

    public ChainedTagClusterReducer(TagClusterReducer... reducers){
        if(reducers == null || reducers == null || ArrayUtils.contains(reducers, null)){
            throw new IllegalArgumentException("The parsed TagClusterReducers MUST NOT"
                + "be NULL an emoty array or contain any NULL element!");
        }
        this.reducers = reducers;
    }

    @Override
    public void reduce(TagLL[] head) {
        for(TagClusterReducer reducer : reducers){
            if(head[0] == null){
                return; //no more tags left
            }
            reducer.reduce(head);
        }

    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append(Arrays.toString(reducers)).toString();
    }
}
