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
package org.apache.stanbol.entityhub.jersey.writers;

import java.util.Collection;
import java.util.Map.Entry;

import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.RangeConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FieldQueryToJSON {

    private FieldQueryToJSON() { /* do not create instances of utility classes */}

    private static Logger log = LoggerFactory.getLogger(FieldQueryToJSON.class);

    /**
     * Converts a {@link FieldQuery} to it's JSON representation
     *
     * @param query the Query
     * @return the {@link JSONObject}
     * @throws JSONException
     */
    static JSONObject toJSON(FieldQuery query) throws JSONException {
        JSONObject jQuery = new JSONObject();
        jQuery.put("selected", new JSONArray(query.getSelectedFields()));
        JSONArray constraints = new JSONArray();
        jQuery.put("constraints", constraints);
        for (Entry<String, Constraint> fieldConstraint : query) {
            JSONObject jFieldConstraint = convertConstraintToJSON(fieldConstraint.getValue());
            jFieldConstraint.put("field", fieldConstraint.getKey()); //add the field
            constraints.put(jFieldConstraint); //add fieldConstraint
        }
        if(query.getLimit() != null){
            jQuery.put("limit", query.getLimit());
        }
        if(query.getOffset() != 0){
            jQuery.put("offset", query.getOffset());
        }
        return jQuery;
    }

    /**
     * Converts a {@link Constraint} to JSON
     *
     * @param constraint the {@link Constraint}
     * @return the JSON representation
     * @throws JSONException
     */
    private static JSONObject convertConstraintToJSON(Constraint constraint) throws JSONException {
        JSONObject jConstraint = new JSONObject();
        jConstraint.put("type", constraint.getType().name());
        switch (constraint.getType()) {
            case value:
                ValueConstraint valueConstraint = ((ValueConstraint) constraint);
                if (valueConstraint.getValue() != null) {
                    jConstraint.put("value", valueConstraint.getValue());
                }
                Collection<String> dataTypes = valueConstraint.getDataTypes();
                if (dataTypes != null && !dataTypes.isEmpty()) {
                    //in case of type = reference we do not need to add any dataTypes!
                    jConstraint.put("dataTypes", valueConstraint.getDataTypes());
                    //Event that internally "reference" is not part of the
                    //ConstraintType enum it is still present in the serialisation
                    //ant the Java API (see ReferenceConstraint class)
                    //Value constraints with the dataType Reference and AnyURI are
                    //considered to represent reference constraints
                    if(dataTypes.size() == 1 && 
                            (dataTypes.contains(DataTypeEnum.Reference.getUri()) || 
                                    dataTypes.contains(DataTypeEnum.AnyUri.getUri()))){
                        jConstraint.remove("type");
                        jConstraint.put("type", "reference");
                    }
                }
                break;
            case text:
                TextConstraint textConstraint = (TextConstraint) constraint;
                if (textConstraint.getLanguages() != null && !textConstraint.getLanguages().isEmpty()) {
                    jConstraint.put("languages", new JSONArray(textConstraint.getLanguages()));
                }
                jConstraint.put("patternType", textConstraint.getPatternType().name());
                if (textConstraint.getTexts() != null && !textConstraint.getTexts().isEmpty()) {
                    if(textConstraint.getTexts().size() == 1){ //write a string
                        jConstraint.put("text", textConstraint.getTexts().get(0));
                    } else { //write an array
                        jConstraint.put("text", textConstraint.getTexts());
                    }
                }
                if(textConstraint.isCaseSensitive()){
                    jConstraint.put("caseSensitive", true);
                } //else default is false
                break;
            case range:
                RangeConstraint rangeConstraint = (RangeConstraint) constraint;
                if (rangeConstraint.getLowerBound() != null) {
                    jConstraint.put("lowerBound", rangeConstraint.getLowerBound());
                }
                if (rangeConstraint.getUpperBound() != null) {
                    jConstraint.put("upperBound", rangeConstraint.getUpperBound());
                }
                jConstraint.put("inclusive", rangeConstraint.isInclusive());
            default:
                //unknown constraint type
                log.warn("Unsupported Constriant Type " + constraint.getType() + " (implementing class=" + constraint.getClass() + "| toString=" + constraint + ") -> skiped");
                break;
        }
        return jConstraint;
    }
}
