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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
/**
 * Dummy implementation of an {@link IndexingDestination} that writes results
 * directly into {@link IndexerTest#indexedData}
 * @author Rupert Westenthaler
 *
 */
public class DummyIndexingDestination implements IndexingDestination {

    Yard yard = new Yard() {
        
        @Override
        public Iterable<Representation> update(Iterable<Representation> representations) throws YardException, IllegalArgumentException {
            Collection<Representation> updated = new ArrayList<Representation>();
            for(Representation rep : representations){
                try {
                    updated.add(update(rep));
                }catch(IllegalArgumentException e){
                    updated.add(null);
                }
            }
            return updated;
        }
        @Override
        public Representation update(Representation represnetation) throws YardException, IllegalArgumentException {
            if(represnetation == null){
                return represnetation;
            }
            if(IndexerTest.indexedData.containsKey(represnetation.getId())){
                IndexerTest.indexedData.put(represnetation.getId(), represnetation);
            } else {
                throw new IllegalArgumentException("Representation "+represnetation.getId()+" not present in store");
            }
            return represnetation;
        }
        
        @Override
        public Iterable<Representation> store(Iterable<Representation> representations) throws NullPointerException,
                                                                                       YardException {
            for(Representation rep : representations){
                store(rep);
            }
            return representations;
        }
        
        @Override
        public Representation store(Representation representation) throws NullPointerException, YardException {
            if(representation != null){
                IndexerTest.indexedData.put(representation.getId(), representation);
            }
            return representation;
        }
        
        @Override
        public void remove(Iterable<String> ids) throws IllegalArgumentException, YardException {
            for(String id :ids){
                remove(id);
            }
        }
        
        @Override
        public void remove(String id) throws IllegalArgumentException, YardException {
            IndexerTest.indexedData.remove(id);
        }
        
        @Override
        public boolean isRepresentation(String id) throws YardException, IllegalArgumentException {
            return IndexerTest.indexedData.containsKey(id);
        }
        
        @Override
        public ValueFactory getValueFactory() {
            return InMemoryValueFactory.getInstance();
        }
        
        @Override
        public Representation getRepresentation(String id) throws YardException, IllegalArgumentException {
            return IndexerTest.indexedData.get(id);
        }
        
        @Override
        public FieldQueryFactory getQueryFactory() {
            return DefaultQueryFactory.getInstance();
        }
        
        @Override
        public String getName() {
            return "dummyYard";
        }
        
        @Override
        public String getId() {
            return "dummyYard";
        }
        
        @Override
        public String getDescription() {
            return "Dummy Implementation of the Yard interface for unit testing";
        }
        
        @Override
        public QueryResultList<Representation> findRepresentation(FieldQuery query) throws YardException, IllegalArgumentException {
            throw new UnsupportedOperationException("I think this is not needed for testing");
        }
        
        @Override
        public QueryResultList<String> findReferences(FieldQuery query) throws YardException, IllegalArgumentException {
            throw new UnsupportedOperationException("I think this is not needed for testing");
        }
        
        @Override
        public QueryResultList<Representation> find(FieldQuery query) throws YardException, IllegalArgumentException {
            throw new UnsupportedOperationException("I think this is not needed for testing");
       }
        
        @Override
        public Representation create(String id) throws IllegalArgumentException, YardException {
            return InMemoryValueFactory.getInstance().createRepresentation(id);
        }
        
        @Override
        public Representation create() throws YardException {
            return InMemoryValueFactory.getInstance().createRepresentation("urn:"+System.currentTimeMillis()+"-"+Math.random());
        }
        @Override
        public void removeAll() throws YardException {
            throw new UnsupportedOperationException("I think this is not needed for testing");
        }
    };
    @Override
    public void finalise() {
    }

    @Override
    public Yard getYard() {
        return yard;
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
    }
    
}