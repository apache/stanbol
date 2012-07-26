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
package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ensure that results have fields that is contextually similar. The implementation is typically based on a
 * cosine similarity score a normalized vector space of term frequencies - inverse document frequencies as
 * done by the MoreLikeThis feature of Solr for instance.
 * 
 * This type of constraint might not be supported by all the yard implementations. If it is not supported it
 * is just ignored.
 */
public class SimilarityConstraint extends Constraint {

    protected final String context;

    protected final List<String> additionalFields;

    public SimilarityConstraint(String context) {
        this(context, null);
    }

    public SimilarityConstraint(String context, List<String> additionalFields) {
        super(ConstraintType.similarity);
        this.context = context;
        if(additionalFields == null || additionalFields.isEmpty()){
            this.additionalFields = Collections.emptyList();
        } else {
            List<String> fields = new ArrayList<String>(additionalFields.size());
            for(String field : additionalFields){
                if(field != null && !field.isEmpty()){
                    fields.add(field);
                }
            }
            this.additionalFields = Collections.unmodifiableList(fields);
        }
    }
    /**
     * Additional fields used for similarity calculations
     * @return
     */
    public List<String> getAdditionalFields() {
        return additionalFields;
    }
    /**
     * The context used for checking the similarity
     * @return
     */
    public String getContext() {
        return context;
    }

}
