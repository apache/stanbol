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
package org.apache.stanbol.enhancer.engines.uimatotriples.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.stanbol.commons.caslight.Feature;

/**
 * A Holder Class for FeatureStucture filters.
 * @author Mihály Héder
 */
public class FeatureStructureFilter {

    List<FeatureFilter> filters = new ArrayList<FeatureFilter>();

    /**
     * Adds a Feature Filter.
     * @param tnf 
     */
    public void addFeatureFilter(FeatureFilter tnf) {
        filters.add(tnf);
    }

    /**
     * Checks whether Feature Filters list is empty.
     * @return 
     */
    public boolean isEmpty() {
        return filters.isEmpty();
    }
    
    /**
     * Checks whether a FeatureStructure is allowed.
     * @param typeName the type name of the FeatureStructure
     * @param features the features of the FeatureStructure to inspect
     * @return true if allowed
     */
    public boolean checkFeatureStructureAllowed(String typeName, Set<Feature> features) {
        //no rule at all
        if (filters.isEmpty()) {
            return true;
        }
        for (FeatureFilter tnf : filters) {
            if (tnf.getTypeName().equals(typeName)) {
                for (Feature f : features) {
                    if (tnf.checkNameValueAllowed(f.getName(), f.getValueAsString())) {
                        //there is a supporting rule
                        return true;
                    }
                }
            }
        }
        //this type is not enumerated
        return false;
    }

    /**
     * Check whether a Feature of a FeatureStructure should be converted to RDF.
     * @param typeName the type name of the FeatureStructure
     * @param f the Feature to inspect
     * @return 
     */
    public boolean checkFeatureToConvert(String typeName, Feature f) {
        //no rule at all
        if (filters.isEmpty()) {
            return true;
        }
        for (FeatureFilter tnf : filters) {
            if (tnf.getTypeName().equals(typeName)) {
                if (tnf.checkNameToPass(f.getName())) {
                    return true;
                }
            }
        }
        //this type is not enumerated
        return false;
    }
}
