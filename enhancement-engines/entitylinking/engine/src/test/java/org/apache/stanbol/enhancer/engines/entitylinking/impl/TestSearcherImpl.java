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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;

public class TestSearcherImpl implements EntitySearcher {

    private final IRI nameField;
    private final LabelTokenizer tokenizer;
    
    private SortedMap<String,Collection<Entity>> data = new TreeMap<String,Collection<Entity>>(String.CASE_INSENSITIVE_ORDER);
    private Map<IRI,Entity> entities = new HashMap<IRI,Entity>();
    private Map<IRI,Collection<RDFTerm>> originInfo;

    
    public TestSearcherImpl(String siteId,IRI nameField, LabelTokenizer tokenizer) {
        this.nameField = nameField;
        this.tokenizer = tokenizer;
        this.originInfo = Collections.singletonMap(
            new IRI(NamespaceEnum.entityhub+"site"), 
            (Collection<RDFTerm>)Collections.singleton(
                (RDFTerm)new PlainLiteralImpl(siteId)));
    }
    
    
    public void addEntity(Entity rep){
        entities.put(rep.getUri(), rep);
        Iterator<Literal> labels = rep.getText(nameField);
        while(labels.hasNext()){
            Literal label = labels.next();
            for(String token : tokenizer.tokenize(label.getLexicalForm(),null)){
                Collection<Entity> values = data.get(token);
                if(values == null){
                    values = new ArrayList<Entity>();
                    data.put(label.getLexicalForm(), values);
                }
                values.add(rep);
            }
        }
        
    }
    
    @Override
    public Entity get(IRI id, Set<IRI> includeFields, String...lanuages) throws IllegalStateException {
        return entities.get(id);
    }

    @Override
    public Collection<? extends Entity> lookup(IRI field,
                                           Set<IRI> includeFields,
                                           List<String> search,
                                           String[] languages,Integer numResults, Integer offset) throws IllegalStateException {
        if(field.equals(nameField)){
            //we do not need sorting
            //Representation needs to implement equals, therefore results filters multiple matches
            Set<Entity> results = new LinkedHashSet<Entity>();
            for(String term : search){
                //TODO: adding 'zzz' to the parsed term is no good solution for
                //      searching ...
                for(Collection<Entity> termResults : data.subMap(term, term+"zzz").values()){
                    results.addAll(termResults);
                }
            }
            List<Entity> resultList = new ArrayList<Entity>(results);
            if(offset != null && offset.intValue() > 0){
                if(offset.intValue() > results.size()){
                    return Collections.emptyList();
                } else {
                    return resultList.subList(offset, results.size());
                }
            } else {
                return results;
            }
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
    public Map<IRI,Collection<RDFTerm>> getOriginInformation() {
        return originInfo;
    }
}
