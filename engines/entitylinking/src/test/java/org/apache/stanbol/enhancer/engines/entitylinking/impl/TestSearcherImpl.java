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
package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opennlp.tools.tokenize.Tokenizer;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

public class TestSearcherImpl implements EntitySearcher {

    private final String nameField;
    private final Tokenizer tokenizer;
    
    private SortedMap<String,Collection<Representation>> data = new TreeMap<String,Collection<Representation>>(String.CASE_INSENSITIVE_ORDER);
    private Map<String,Representation> entities = new HashMap<String,Representation>();
    private Map<UriRef,Collection<Resource>> originInfo;

    
    public TestSearcherImpl(String siteId,String nameField, Tokenizer tokenizer) {
        this.nameField = nameField;
        this.tokenizer = tokenizer;
        this.originInfo = Collections.singletonMap(
            new UriRef(RdfResourceEnum.site.getUri()), 
            (Collection<Resource>)Collections.singleton(
                (Resource)new PlainLiteralImpl(siteId)));
    }
    
    
    public void addEntity(Representation rep){
        entities.put(rep.getId(), rep);
        Iterator<Text> labels = rep.getText(nameField);
        while(labels.hasNext()){
            Text label = labels.next();
            for(String token : tokenizer.tokenize(label.getText())){
                Collection<Representation> values = data.get(token);
                if(values == null){
                    values = new ArrayList<Representation>();
                    data.put(label.getText(), values);
                }
                values.add(rep);
            }
        }
        
    }
    
    @Override
    public Representation get(String id, Set<String> includeFields) throws IllegalStateException {
        return entities.get(id);
    }

    @Override
    public Collection<? extends Representation> lookup(String field,
                                           Set<String> includeFields,
                                           List<String> search,
                                           String[] languages,Integer numResults) throws IllegalStateException {
        if(field.equals(nameField)){
            //we do not need sorting
            //Representation needs to implement equals, therefore results filters multiple matches
            Set<Representation> results = new HashSet<Representation>();
            for(String term : search){
                //TODO: adding 'zzz' to the parsed term is no good solution for
                //      searching ...
                for(Collection<Representation> termResults : data.subMap(term, term+"zzz").values()){
                    results.addAll(termResults);
                }
            }
            return results;
        } else {
            throw new IllegalStateException("Lookup is only supported for the nameField '"+
                nameField+"' parsed to the constructor");
        }
    }

    @Override
    public boolean supportsOfflineMode() {
        return true;
    }


    @Override
    public Integer getLimit() {
        return null;
    }

    @Override
    public Map<UriRef,Collection<Resource>> getOriginInformation() {
        return originInfo;
    }
}
