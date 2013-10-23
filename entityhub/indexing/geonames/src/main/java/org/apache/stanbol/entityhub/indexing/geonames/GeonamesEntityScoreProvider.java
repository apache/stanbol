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
package org.apache.stanbol.entityhub.indexing.geonames;

import static org.apache.stanbol.entityhub.indexing.geonames.GeonamesConstants.GEONAMES_ONTOLOGY_NS;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

public class GeonamesEntityScoreProvider implements EntityScoreProvider {

    private static final String FCLASS_A = GEONAMES_ONTOLOGY_NS +"A";
    private static final String FCLASS_P = GEONAMES_ONTOLOGY_NS +"P";
    private static final int MAX_POPULATION = 10000000;
    private static final int MIN_POPULATION = 1000;
    // used to change the scale of the the natural log 
    private static final double POPULATION_SCALE = 10000; //10k is one 
    private static final double FACT = Math.log1p(MAX_POPULATION/POPULATION_SCALE);
    private static final Float DEFAULT_SCORE = Float.valueOf(0.1f);
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean needsData() {
        return true;
    }

    @Override
    public Float process(String id) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This implementation requries data to process the score");
    }

    @Override
    public Float process(Representation entity) throws UnsupportedOperationException {
        Reference ref = entity.getFirstReference(GeonamesPropertyEnum.gn_featureClass.toString());
        String fclass = ref == null ? null : ref.getReference();
        //ref = entity.getFirstReference(GeonamesPropertyEnum.gn_featureCode.toString());
        //String fCode = ref == null ? null : ref.getReference();
        
        if(FCLASS_A.equals(fclass)){
            ref = entity.getFirstReference(GeonamesPropertyEnum.gn_featureCode.toString());
            String fcode = ref == null ? null : ref.getReference();
            if(fcode == null){
                return DEFAULT_SCORE;
            } else {
                fcode = fcode.substring(GEONAMES_ONTOLOGY_NS.length()+2);
                if(fcode.length() > 2 && fcode.startsWith("PC")){
                    return Float.valueOf(1.0f);
                } else if(fcode.length() > 3 && fcode.charAt(3) == '1'){
                    return Float.valueOf(0.5f);
                } else if(fcode.length() > 3 && fcode.charAt(3) == '2'){
                    return Float.valueOf(0.25f);
                } else if(fcode.length() > 3 && fcode.charAt(3) == '3'){
                    return Float.valueOf(0.125f);
                } else if(fcode.length() > 3 && (fcode.charAt(3) == '4' ||
                        fcode.charAt(3) == 'D')){
                    return Float.valueOf(0.062f);
                } else if(fcode.length() > 3 && fcode.charAt(3) == '5'){
                    return Float.valueOf(0.031f);
                } else {
                    return Float.valueOf(0.062f);
                }
            }
        } else if(FCLASS_P.equals(fclass)){
            Long population = entity.getFirst(GeonamesPropertyEnum.gn_population.toString(), Long.class);
            if(population == null){
                population = Long.valueOf(1); //use 1 to avoid creating a new instance
            }
            //normalise the population
            double p = Math.max(Math.min(MAX_POPULATION, population.longValue()),MIN_POPULATION);
            //population factor
            double fact = Math.log1p(p/POPULATION_SCALE);
            //Normalised based on the maximum popuoation
            Float score = Float.valueOf((float)(fact/FACT));
            return score;
        } else {
            return DEFAULT_SCORE;
        }
    }

}
