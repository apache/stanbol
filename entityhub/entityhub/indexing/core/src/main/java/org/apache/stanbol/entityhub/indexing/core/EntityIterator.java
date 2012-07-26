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
package org.apache.stanbol.entityhub.indexing.core;

import java.util.Iterator;

/**
 * Interface used to iterate over Entities and optionally there score.
 * Typically Entities are by decreasing score values however this is no
 * requirement by this implementation. Scores &lt; 0 indicate that entities
 * should not be indexed. If no score is available the score MUST BE 
 * <code>null</code>
 * @author Rupert Westenthaler
 *
 */
public interface EntityIterator extends IndexingComponent, Iterator<EntityIterator.EntityScore> {

    /**
     * Struct like class providing access to the Id and optionally the score of 
     * an entity. A score of <code>0</code> indicated that no score is available.
     * Scores <code>&lt; 0</code> indicate that entities should be ignored
     * (unless the indexer is configured otherwise)
     * Allows direct access to the final id and score values
     * @author Rupert Westenthaler
     *
     */
    public class EntityScore{
        /**
         * The ID of the Entity (read only)
         */
        public final String id;
        /**
         * The score for the Entity. Entities with scores &lt; 0 should be
         * ignored for indexing. <code>null</code> indicates that no score is
         * available
         */
        public final Float score;
        public EntityScore(String id,Float score){
            this.id = id;
            this.score = score;
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof EntityScore && id.equals(((EntityScore)obj).id);
        }
        @Override
        public int hashCode() {
            return id.hashCode();
        }
        @Override
        public String toString() {
            return String.format("EntityScore[id=%s|score=%s]", id,score);
        }
    }
}
