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
package org.apache.stanbol.enhancer.engines.entitycomention.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * EntitySearch implementation that does hold Entity information of mentioned
 * Entities in memory.
 * @author Rupert Westenthaler
 *
 */
public class InMemoryEntityIndex implements EntitySearcher {

    private final Logger log = LoggerFactory.getLogger(InMemoryEntityIndex.class);
    
    protected final LabelTokenizer tokenizer;
    //Holds Entity data
    private SortedMap<String,Collection<Entity>> index = new TreeMap<String,Collection<Entity>>(String.CASE_INSENSITIVE_ORDER);
    private Map<IRI,Entity> entities = new HashMap<IRI,Entity>();
    private Set<String> indexLanguages;
    protected String language;
    protected IRI nameField;

    
    public InMemoryEntityIndex(LabelTokenizer tokenizer, IRI nameField, String...languages) {
        this.indexLanguages = languages == null || languages.length < 1 ? 
                Collections.singleton((String)null) : 
                        new HashSet<String>(Arrays.asList(languages));
        this.language = languages == null || languages.length < 1 ? null :
            languages[0];
        this.tokenizer = tokenizer;
        this.nameField = nameField;
    }
    
    
    public void addEntity(Entity entity){
        if(log.isDebugEnabled()){
            log.debug(" > register {}",entity);
        }
        entities.put(entity.getUri(), entity);
        Iterator<Literal> labels = entity.getText(nameField);
        while(labels.hasNext()){
            Literal label = labels.next();
            String lang = label.getLanguage() == null ? null : label.getLanguage().toString();
            if(indexLanguages.contains(lang)){
                for(String token : tokenizer.tokenize(label.getLexicalForm(),null)){
                    token = token.toLowerCase(Locale.ROOT);
                    Collection<Entity> values = index.get(token);
                    if(values == null){
                        values = new ArrayList<Entity>();
                        index.put(token, values);
                    }
                    values.add(entity);
                }
            } //else ignore labels in other languages
        }
        
    }
    
    @Override
    public Entity get(IRI id, Set<IRI> includeFields, String...languages) throws IllegalStateException {
        return entities.get(id);
    }

    @Override
    public Collection<? extends Entity> lookup(IRI field,
                                           Set<IRI> includeFields,
                                           List<String> search, String[] languages,
                                           Integer numResults, Integer offset) throws IllegalStateException {
        //this assumes that 
        assert nameField.equals(field); //the nameField is the field
        assert Arrays.asList(languages).contains(language); //the parsed languages include the language
        //NOTES: 
        // We can ignore the following parameters
        // * includeFields: as we will return the Entities as added to the index
        
        //The Syntax requires to
        //  * AND over the tokenized elements of the search List
        //  * OR over the elements in the search
        //  * Elements that do match more search elements need to be ranked first
        Map<Entity, int[]> results = new HashMap<Entity,int[]>();
        for(String qe : search){
            Set<Entity> qeResult = join(tokenizer.tokenize(qe, language));
            for(Entity e : qeResult){
                int[] count = results.get(e);
                if(count != null){
                    count[0] = count[0]+qe.length();
                } else {
                    results.put(e, new int[]{qe.length()});
                }
            }
        }
        @SuppressWarnings("unchecked") //TODO how to create generic arrays
        Entry<Entity,int[]>[] resultArray = results.entrySet().toArray(new Entry[results.size()]);
        int index;
        if(offset != null && offset.intValue() > 0){
            index = offset.intValue();
        } else {
            index = 0;
        }
        if(index >= resultArray.length){ //no more results
            return Collections.emptyList();
        }
        //final ranking
        Arrays.sort(resultArray, RESULT_SCORE_COMPARATOR);
        List<Entity> resultList = new ArrayList<Entity>(Math.min(numResults+3, (resultArray.length-index)));
        int lastScore = -1;
        boolean done = false;
        //start at the parsed offset
        for(; index < resultArray.length && !done; index++){
            if(index < numResults){
                resultList.add(resultArray[index].getKey());
                if(index == (numResults - 1)){ //memorize the score of the last included
                    lastScore = resultArray[index].getValue()[0];
                }
            } else if (lastScore == resultArray[index].getValue()[0]){
                //include additional results with the same score
                resultList.add(resultArray[index].getKey());
            } else { //cut of
                done = true;
            }
       }
        return resultList;
    }

    private static final Comparator<Collection<?>> COLLECTION_SIZE_COMPARATOR = new Comparator<Collection<?>>() {

        @Override
        public int compare(Collection<?> c1, Collection<?> c2) {
            return c1 == null && c2 == null ? 0 : 
                c1 == null ? -1 : c2 == null ? 1 : //null values last
                    c2.size() - c1.size(); //lowest size first
        }
        
    };
    private static final Comparator<Entry<Entity,int[]>> RESULT_SCORE_COMPARATOR = new Comparator<Entry<Entity,int[]>>() {

        @Override
        public int compare(Entry<Entity,int[]> e1, Entry<Entity,int[]> e2) {
            return e1 == null && e2 == null ? 0 :
                e1 == null ? -1  : e2 == null ? 1 : //null values last
                    e1.getValue()[0] - e2.getValue()[0]; //highest score first!
        }};

    /**
     * Searches for Elements that do contain all the parsed Query Tokens
     * @param queryTokens the query tokens. MUST NOT be NULL, empty or contain
     * any NULL or empty string as element
     * @return matching entities or an empty Set if none.
     */
    private Set<Entity> join(String...queryTokens) {
        @SuppressWarnings("unchecked") //TODO: how to create a generic typed array
        Collection<Entity>[] tokenResults = new Collection[queryTokens.length];
        for(int i=0;i<queryTokens.length;i++){
            Collection<Entity> tokenResult = index.get(queryTokens[i].toLowerCase(Locale.ROOT));
            if(tokenResult == null || tokenResult.isEmpty()){
                return Collections.emptySet();
            }
            tokenResults[i] = tokenResult;
        }
        Set<Entity> join = new HashSet<Entity>(tokenResults[0]);
        if(tokenResults.length == 1){
            return join;
        } //else we need to join the single results
        
        //we want to join the shortest results first
        Arrays.sort(tokenResults,COLLECTION_SIZE_COMPARATOR);
        for(int i = 1; i < tokenResults.length && !join.isEmpty(); i++){
            Set<Entity> old = join;
            join = new HashSet<Entity>(); //new set to add all elements
            for(Iterator<Entity> it = tokenResults[i].iterator(); it.hasNext() && !old.isEmpty();){
                Entity e = it.next();
                if(old.remove(e)){
                    join.add(e);
                }
            }
        }
        return join;
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
        return Collections.emptyMap();
    }
}
