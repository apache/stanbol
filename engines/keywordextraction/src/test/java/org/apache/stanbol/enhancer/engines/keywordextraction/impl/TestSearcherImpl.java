package org.apache.stanbol.enhancer.engines.keywordextraction.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import opennlp.tools.tokenize.Tokenizer;

import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;

public class TestSearcherImpl implements EntitySearcher {

    private final String nameField;
    private final Tokenizer tokenizer;
    
    private SortedMap<String,Collection<Representation>> data = new TreeMap<String,Collection<Representation>>(String.CASE_INSENSITIVE_ORDER);
    private Map<String,Representation> entities = new HashMap<String,Representation>();

    
    public TestSearcherImpl(String nameField, Tokenizer tokenizer) {
        this.nameField = nameField;
        this.tokenizer = tokenizer;
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
                                           String... languages) throws IllegalStateException {
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

}
