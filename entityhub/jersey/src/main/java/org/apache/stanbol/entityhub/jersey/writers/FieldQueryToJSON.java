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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.ldpath.query.LDPathSelect;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.RangeConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.SimilarityConstraint;
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
    static JSONObject toJSON(FieldQuery query,NamespacePrefixService nsPrefixService) throws JSONException {
        JSONObject jQuery = new JSONObject();
        jQuery.put("selected", new JSONArray(query.getSelectedFields()));
        JSONArray constraints = new JSONArray();
        jQuery.put("constraints", constraints);
        for (Entry<String, Constraint> fieldConstraint : query) {
            JSONObject jFieldConstraint = convertConstraintToJSON(fieldConstraint.getValue(),nsPrefixService);
            jFieldConstraint.put("field", fieldConstraint.getKey()); //add the field
            constraints.put(jFieldConstraint); //add fieldConstraint
        }
        if(query.getLimit() != null){
            jQuery.put("limit", query.getLimit());
        }
        //if(query.getOffset() != 0){
            jQuery.put("offset", query.getOffset());
        //}
        if(query instanceof LDPathSelect && 
                ((LDPathSelect)query).getLDPathSelect() != null &&
                !((LDPathSelect)query).getLDPathSelect().isEmpty()){
            jQuery.put("ldpath", ((LDPathSelect)query).getLDPathSelect());
        }
        return jQuery;
    }

    /**
     * Converts a {@link Constraint} to JSON
     *
     * @param constraint the {@link Constraint}
     * @param nsPrefixService Optionally the service that is used to convert data type
     * URIs to '{prefix}:{localname}'
     * @return the JSON representation
     * @throws JSONException
     */
    private static JSONObject convertConstraintToJSON(Constraint constraint, NamespacePrefixService nsPrefixService) throws JSONException {
        JSONObject jConstraint = new JSONObject();
        jConstraint.put("type", constraint.getType().name());
        switch (constraint.getType()) {
            case value: //both ValueConstraint and ReferenceConstraint
                ValueConstraint valueConstraint = ((ValueConstraint) constraint);
                if (valueConstraint.getValues() != null) {
                    if(valueConstraint.getValues().size() == 1){
                        jConstraint.put("value", valueConstraint.getValues().iterator().next());
                    } else {
                        jConstraint.put("value", new JSONArray(valueConstraint.getValues()));
                    }
                }
                if(constraint instanceof ReferenceConstraint){
                    //the type "reference" is not present in the ConstraintType
                    //enum, because internally ReferenceConstraints are just a
                    //ValueConstraint with a predefined data type, but "reference"
                    //is still a valid value of the type property in JSON
                    jConstraint.put("type", "reference");
                } else { // valueConstraint
                    jConstraint.put("type", constraint.getType().name());
                    //for valueConstraints we need to add also the dataType(s)
                    Collection<String> dataTypes = valueConstraint.getDataTypes();
                    if (dataTypes != null && !dataTypes.isEmpty()) {
                        if(dataTypes.size() == 1) {
                            String dataType = dataTypes.iterator().next();
                            jConstraint.put("datatype",
                                nsPrefixService != null ? nsPrefixService.getShortName(dataType) : dataType);
                        } else {
                            ArrayList<String> dataTypeValues = new ArrayList<String>(dataTypes.size());
                            for(String dataType : dataTypes){
                                dataTypeValues.add(nsPrefixService != null ?
                                        nsPrefixService.getShortName(dataType) : dataType);
                            }
                            jConstraint.put("datatype", dataTypeValues);
                        }
                    }
                }
                //finally write the MODE
                if(valueConstraint.getMode() != null){
                    jConstraint.put("mode", valueConstraint.getMode());
                }
                break;
            case text:
                TextConstraint textConstraint = (TextConstraint) constraint;
                Collection<String> languages = textConstraint.getLanguages();
                if (languages != null && !languages.isEmpty()) {
                    if(languages.size() == 1){
                        jConstraint.put("language", languages.iterator().next());
                    } else {
                        jConstraint.put("language", new JSONArray(languages));
                    }
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
                Set<DataTypeEnum> dataTypes = EnumSet.noneOf(DataTypeEnum.class);
                if (rangeConstraint.getLowerBound() != null) {
                    jConstraint.put("lowerBound", rangeConstraint.getLowerBound());
                    dataTypes.addAll(DataTypeEnum.getPrimaryDataTypes(
                        rangeConstraint.getLowerBound().getClass()));
                }
                if (rangeConstraint.getUpperBound() != null) {
                    jConstraint.put("upperBound", rangeConstraint.getUpperBound());
                    dataTypes.addAll(DataTypeEnum.getPrimaryDataTypes(
                        rangeConstraint.getUpperBound().getClass()));
                }
                jConstraint.put("inclusive", rangeConstraint.isInclusive());
                if(!dataTypes.isEmpty()){
                    jConstraint.put("datatype", dataTypes.iterator().next().getShortName());
                }
                break;
            case similarity:
                SimilarityConstraint simConstraint = (SimilarityConstraint) constraint;
                jConstraint.put("context", simConstraint.getContext());
                if(!simConstraint.getAdditionalFields().isEmpty()){
                    jConstraint.put("addFields", new JSONArray(
                        simConstraint.getAdditionalFields()));
                }
                break;
            default:
                //unknown constraint type
                log.warn("Unsupported Constriant Type " + constraint.getType() + " (implementing class=" + constraint.getClass() + "| toString=" + constraint + ") -> skiped");
                break;
        }
        return jConstraint;
    }
}
