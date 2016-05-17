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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Entity that Matches somewhere in the tagged text.
 * <p>
 * Matches are generated for {@link #id Lucene Document IDs} and
 * {@link #uri Solr Document ids} (the URI of the matching entity). On the
 * first access to the {@link #getLabels() labels}, {@link #getTypes() types} 
 * or {@link #getRedirects()} all those information are lazily retrieved by 
 * accessing the data stored in the index. The {@link FieldLoader} instance
 * parsed in the constructor is used to load those information.
 * Typically this is implemented by the {@link MatchPool} instance used to
 * instantiate Match instances.
 * 
 * 
 * @author Rupert Westenthaler
 *
 */
public class Match {
    
    private static final Logger log = LoggerFactory.getLogger(Match.class);

    /**
     * Lucene document id
     */
    public final int id;
    
    private FieldLoader fieldLoader;
    
    private Map<FieldType,Object> values;
    private boolean error = false;
    
    private Literal matchLabel;
    /**
     * The score of the Match
     */
    private double score;

    Match(int id, FieldLoader fieldLoader){
        this.id = id;
        this.fieldLoader = fieldLoader;
    }

    public String getUri() {
        return getValue(FieldType.id);
    }
    
    public Collection<Literal> getLabels(){
        return getValues(FieldType.label);
    }
    
    public Collection<IRI> getTypes(){
        return getValues(FieldType.type);
    }
    
    public Collection<IRI> getRedirects(){
        return getValues(FieldType.redirect);
    }
    public Double getRanking(){
        return getValue(FieldType.ranking);
    }
    private <T>  Collection<T> getValues(FieldType type){
        if(!type.isMultivalued()){
            throw new IllegalArgumentException("The parsed field Type '" + type
                + "' is not multi valued!");
        }
        Object value = getValue(type);
        return value == null ? Collections.EMPTY_SET : (Collection<T>)value;
    }
    private <T> T getValue(FieldType type){
        if(error){
            return null;
        } else if(values == null){
            try {
                values = fieldLoader.load(id);
            } catch (IOException e) {
                log.warn("Unable to load Entity for Lucene DocId '"+id+"'!",e);
                error = true;
                return null;
            } catch (RuntimeException e) {
                log.warn("Error while loading Entity for Lucene DocId '"+id+"'!",e);
                error = true;
                return null;
            }
        }
        return (T) values.get(type);
    }
    
    public void setMatch(double score, Literal matchLabel){
        this.score = score;
        this.matchLabel = matchLabel;
    }
    /**
     * Allows to update the {@link #getScore() score} without changing the
     * {@link #getMatchLabel() match}.
     * @param score the new score
     */
    public void updateScore(double score) {
        this.score = score;
    }
    /**
     * The score 
     * @return the score
     */
    public double getScore() {
        return score;
    }

    public Literal getMatchLabel() {
        return matchLabel;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof Match && id == ((Match)o).id;
    }
    
    @Override
    public String toString() {
        String uri = getUri();
        return uri != null ? uri : "Match[id: "+id+"|(uri unknown)]";
    }
    
    static enum FieldType {
        id(String.class),
        label(Literal.class, true), 
        type(IRI.class,true), 
        redirect(IRI.class,true), 
        ranking(Double.class);
        
        Class<?> valueType;
        boolean multivalued;
        
        FieldType(Class<?> type){
            this(type,false);
        }
        FieldType(Class<?> type, boolean multivalued){
            this.valueType = type;
            this.multivalued = multivalued;
        }
        public Class<?> getValueType() {
            return valueType;
        }
        public boolean isMultivalued() {
            return multivalued;
        }
    }
    
    static interface FieldLoader {
        Map<FieldType,Object> load(int id) throws IOException;
    }
    /**
     * Compares {@link Match} instances based on the {@link Match#getScore()}
     */
    public static final Comparator<Match> SCORE_COMPARATOR = new Comparator<Match>() {

        @Override
        public int compare(Match a, Match b) {
            return Double.compare(b.score,a.score); //higher first
        }
        
    };
    /**
     * Compares {@link Match} instances based on the {@link Match#getRanking()}.
     * <code>null</code> values are assumed to be the smallest.
     */
    public static final Comparator<Match> ENTITY_RANK_COMPARATOR = new Comparator<Match>(){
        @Override
        public int compare(Match arg0, Match arg1) {
            Double r1 = arg0.getRanking();
            Double r2 = arg1.getRanking();
            return r2 == null ? r1 == null ? 0 : -1 : r1 == null ? 1 : r2.compareTo(r1);
        }
    };

}
