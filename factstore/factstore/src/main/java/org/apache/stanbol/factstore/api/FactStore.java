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
package org.apache.stanbol.factstore.api;

import java.util.Set;

import org.apache.stanbol.factstore.model.Fact;
import org.apache.stanbol.factstore.model.FactSchema;
import org.apache.stanbol.factstore.model.Query;
import org.apache.stanbol.factstore.model.FactResultSet;

public interface FactStore {

    public int getMaxFactSchemaURNLength();
    
    public boolean existsFactSchema(String factSchemaURN) throws Exception;

    public FactSchema getFactSchema(String factSchemaURN);

    public void createFactSchema(FactSchema factSchema) throws Exception;
    
    public Fact getFact(int factId, String factSchemaURN) throws Exception;
    
    public int addFact(Fact fact) throws Exception;
    
    public void addFacts(Set<Fact> factSet) throws Exception;

    public FactResultSet query(Query query) throws Exception;

}
