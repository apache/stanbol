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
package org.apache.stanbol.enhancer.engines.entityhublinking;

import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

public class EntityhubEntity extends Entity {
    
    private static RdfValueFactory vf = RdfValueFactory.getInstance();
    private static IRI entityRanking = new IRI(RdfResourceEnum.entityRank.getUri());
    
    public EntityhubEntity(Representation rep, Set<IRI> fields, Set<String> languages) {
        super(new IRI(rep.getId()), 
            toGraph(rep, fields, languages));
    }
    @Override
    public Float getEntityRanking() {
        return EnhancementEngineHelper.get(data, uri, entityRanking, Float.class, lf);
    }
    /**
     * Converts {@link Representation}s to RDF ({@link Graph}) and
     * also filter literals with languages other than the parsed one
     * @param rep
     * @param languages
     * @return
     */
    private static Graph toGraph(Representation rep, Set<IRI> includeFields, Set<String> languages){
        if (rep instanceof RdfRepresentation) {
            return ((RdfRepresentation) rep).getRdfGraph();
        } else {
            //create the Clerezza Represenation
            RdfRepresentation clerezzaRep = vf.createRepresentation(rep.getId());
            //Copy all values field by field
            for (Iterator<String> fields = rep.getFieldNames(); fields.hasNext();) {
                String field = fields.next();
                if(includeFields == null || includeFields.contains(field)){
                    for (Iterator<Object> fieldValues = rep.get(field); fieldValues.hasNext();) {
                        Object value = fieldValues.next();
                        if(languages == null || //we need not to filter languages
                                !(value instanceof Text) || //filter only Text values
                                languages.contains(((Text)value).getLanguage())){
                            clerezzaRep.add(field, value);
                        }
                    }
                }
            }
            return clerezzaRep.getRdfGraph();
        }
        
    }
}