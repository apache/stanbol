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
/**
 * 
 */
package org.apache.stanbol.entityhub.indexing.core;

import java.util.Iterator;
import java.util.Map;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Dummy implementation of an {@link EntityIterator} that reads entity ids
 * directly form {@link IndexerTest#testData}
 * @author Rupert Westenthaler
 *
 */
public class DummyEntityIdSource implements EntityIterator {
    private Iterator<Representation> entiyIterator = IndexerTest.testData.values().iterator();
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
    }

    @Override
    public boolean hasNext() {
        return entiyIterator.hasNext();
    }

    @Override
    public EntityScore next() {
        Representation next = entiyIterator.next();
        Number score = next.getFirst(RdfResourceEnum.entityRank.getUri(), Number.class);
        return new EntityScore(next.getId(), score == null?0:score.floatValue());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}