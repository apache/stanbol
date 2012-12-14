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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class filters caslight Feature by rules.
 * @author Mihály Héder
 */
public class FeatureFilter {
    String typeName;
    List<Entry<String,String>> features = new ArrayList<Entry<String, String>>();
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    /**
     * Returns the list of Features recognized by this filter. 
     * @return 
     */
    public List<Entry<String, String>> getFeatures() {
        return features;
    }

    /**
     * Sets the list of Features recognized by this filter. 
     * @param features 
     */
    public void setFeatures(List<Entry<String, String>> features) {
        this.features = features;
    }

    /**
     * Returns the FeatureStructure's type name for which this filter was created.
     * @return 
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Sets the FeatureStructure's type name for which this filter was created.
     * @param typeName 
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    /**
     * Adds a Feature filtering rule.
     * @param featureName the name of the Feature in question.
     * @param featureValue the value of the rule: a regular expression against the value should match.
     */
    public void addFeatureFilter(String featureName, String featureValue) {
        Entry<String,String> entry = new AbstractMap.SimpleEntry<String, String>(featureName, featureValue);
        features.add(entry);
    }
    
    /**
     * Checks whether a Feature name and Value is allowed by the rules.
     * @param name the name of the Feature
     * @param value the Value of the Feature
     * @return 
     */
    public boolean checkNameValueAllowed(String name, String value) {
        //no rules at all for features -> accept
        logger.debug("checking name:"+name+" value:"+value);
        if (features.isEmpty()) {
            return true;
        }
        for (Entry<String,String> e:features) {
            logger.debug("Checking against name:"+e.getKey() + " value:"+ e.getValue());
            if (e.getKey().equals(name)) {
                if (e.getValue() == null || e.getValue().isEmpty()) {
                    //feature name enumerated, no value -> accept
                    return true;
                } else {
                    if (value.matches(e.getValue())) {
                        //value matches on regex
                        return true;
                    }
                    else {
                        //value does not match
                        return false;
                    }
                }
            }            
        }
        //no rule for this feature -> deny
        return false;
    }

    /**
     * Check wheter this Feature is allowed to pass the filtering.
     * @param name
     * @return 
     */
    boolean checkNameToPass(String name) {
        //no rules at all for features -> print
        if (features.isEmpty()) {
            return true;
        }
        for (Entry<String,String> e:features) {
            //enumerated
            if (e.getKey().equals(name)) {
                //name there
                return true;
            }
        }
        //not enumerated
        return false;
    }
    
}
