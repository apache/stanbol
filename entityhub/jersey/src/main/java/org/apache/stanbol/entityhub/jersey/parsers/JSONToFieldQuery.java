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
package org.apache.stanbol.entityhub.jersey.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.RangeConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint.ConstraintType;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a FieldQuery from its JSON representation.
 * @author Rupert Westenthaler
 *
 */
public class JSONToFieldQuery {
    private static final Logger log = LoggerFactory.getLogger(JSONToFieldQuery.class);
    
    public static FieldQuery fromJSON(String jsonQueryString) throws JSONException{
        if(jsonQueryString == null){
            throw new NullPointerException("The parsed JSON object MUST NOT be NULL!");
        }
        JSONObject jQuery = new JSONObject(jsonQueryString);
        FieldQuery query = DefaultQueryFactory.getInstance().createFieldQuery();
        if(!jQuery.has("constraints")){
            throw new IllegalArgumentException("The parsed JSON object MUST contain the required key \"constraints\"");
        }
        JSONArray constraints = jQuery.getJSONArray("constraints");
        for(int i=0;i<constraints.length();i++){
            JSONObject jConstraint = constraints.getJSONObject(i);
            if(jConstraint.has("field")){
                String field = jConstraint.getString("field");
                //check if there is already a constraint for that field
                if(field == null || field.isEmpty()){
                    log.warn("The value of the key \"field\" MUST NOT be NULL nor emtpy!");
                    log.warn(String.format("Constraint:\n %s",jConstraint.toString(4)));
                } else if(query.isConstraint(field)){
                    log.warn(String.format("Multiple constraints for field %s in parsed FieldQuery!",field));
                    log.warn(String.format(" - all Constraints:\n", constraints.toString(4)));
                    log.warn(String.format(" - ignore Constraint:\n %s",jConstraint.toString(4)));
                } else {
                    Constraint constraint = parseConstraint(jConstraint);
                    if(constraint != null){
                        query.setConstraint(field, parseConstraint(jConstraint));
                    } else { 
                        // log unparseable constraint (specific warning already
                        // given by the parseConstraint method
                        log.warn(String.format(" - ignore Constraint:\n %s",jConstraint.toString(4)));
                    }
                }
            } else { //no field defined -> ignroe and write warning!
                log.warn("Earch Constraint of a FieldQuery MUST define the key \"field\"!");
                log.warn(String.format("Constraint:\n %s",jConstraint.toString(4)));
            }
        }
        //parse selected fields
        JSONArray selected = jQuery.optJSONArray("selected");
        if(selected != null){
            for(int i=0;i<selected.length();i++){
                String selectedField = selected.getString(i);
                if(selectedField != null && !selectedField.isEmpty()){
                    query.addSelectedField(selectedField);
                }
            }
        } //else no selected fields -> funny but maybe someone do need only the ids
        //parse limit and offset
        if(jQuery.has("limit") && !jQuery.isNull("limit")){
            query.setLimit(jQuery.getInt("limit"));
        }
        if(jQuery.has("offset") && !jQuery.isNull("offset")){
            query.setOffset(jQuery.getInt("offset"));
        }
        return query;
    }

    private static Constraint parseConstraint(JSONObject jConstraint) throws JSONException {
        if(jConstraint.has("type") && !jConstraint.isNull("type")) {
            String type = jConstraint.getString("type");
            //Event that internally "reference" is not part of the
            //ConstraintType enum it is still present in the serialisation
            //ant the Java API (see ReferenceConstraint class)
            //Value constraints with the dataType Reference and AnyURI are
            //considered to represent reference constraints
            if(type.equals("reference")){
                return parseReferenceConstraint(jConstraint);
            } else if (type.equals(ConstraintType.value.name())){
                return parseValueConstraint(jConstraint);
            } else if (type.equals(ConstraintType.text.name())){
                return parseTextConstraint(jConstraint);
            } else if (type.equals(ConstraintType.range.name())){
                return parseRangeConstraint(jConstraint);
            } else {
                log.warn(String.format("Unknown Constraint Type %s. Supported values are %s",               
                    Arrays.asList("reference",ConstraintType.values())));
                return null;
            }
        } else {
            log.warn(String.format("Earch Constraint MUST HAVE the \"type\" key set to one of the values %s",
                Arrays.asList("reference",ConstraintType.values())));
            return null;
        }
    }

    /**
     * @param jConstraint
     * @return
     * @throws JSONException
     */
    private static Constraint parseRangeConstraint(JSONObject jConstraint) throws JSONException {
        Constraint constraint;
        boolean inclusive;
        if(jConstraint.has("inclusive")){
            inclusive = jConstraint.getBoolean("inclusive");
        } else {
            log.info("RangeConstraint does not define the field \"inclusive\". Use false as default!");
            inclusive = false;
        }
        Object upperBound = jConstraint.opt("upperBound");
        Object lowerBound = jConstraint.opt("lowerBound");
        if(upperBound == null && lowerBound == null){
            log.warn("Range Constraint does not define an \"upperBound\" nor an \"lowerBound\"! At least MUST BE parsed for a valid RangeConstraint.");
            constraint = null;
        } else {
            constraint = new RangeConstraint(lowerBound, upperBound, inclusive);
        }
        return constraint;
    }

    /**
     * @param jConstraint
     * @return
     * @throws JSONException
     */
    private static Constraint parseTextConstraint(JSONObject jConstraint) throws JSONException {
        Constraint constraint;
        boolean caseSensitive = jConstraint.optBoolean("caseSensitive", false);
        //parse patternType
        PatternType patternType;
        String jPatternType = jConstraint.optString("patternType");
        if(jPatternType == null){
            patternType = PatternType.none;
        } else {
            try {
                patternType = PatternType.valueOf(jPatternType);
            } catch (IllegalArgumentException e) {
                log.warn(String.format("Encountered unknown patternType for TextConstraint! Will use default value %s (allowed values are: %s)",
                    jPatternType,PatternType.none,Arrays.toString(PatternType.values())));
                patternType = PatternType.none;
            }
        }
        //parse languages
        Collection<String> languages;
        JSONArray jLanguages = jConstraint.optJSONArray("languages");
        if(jLanguages != null && jLanguages.length()>0){
            languages = new ArrayList<String>(jLanguages.length());
            for(int i=0;i<jLanguages.length();i++){
                String lang = jLanguages.getString(i);
                if(lang != null && !lang.isEmpty()){
                    languages.add(lang);
                }
            }
            if(languages.isEmpty()){
                languages = null; //if no one was successfully added set the list back to null
            }
        } else {
            languages = null;
        }
        //parse text and create constraint
        if(jConstraint.has("text") && !jConstraint.isNull("text")){
            constraint = new TextConstraint(jConstraint.getString("text"),
                patternType,caseSensitive,
                languages == null?null:languages.toArray(new String[languages.size()]));
        } else {
            log.warn("Parsed TextConstraint doese not define the required field \"text\"!");
            constraint = null;
        }
        
        TextConstraint textConstraint = (TextConstraint) constraint;
        if (textConstraint.getLanguages() != null && !textConstraint.getLanguages().isEmpty()) {
            jConstraint.put("languages", new JSONArray(textConstraint.getLanguages()));
        }
        jConstraint.put("patternType", textConstraint.getPatternType().name());
        if (textConstraint.getText() != null && !textConstraint.getText().isEmpty()) {
            jConstraint.put("text", textConstraint.getText());
        }
        return constraint;
    }

    /**
     * @param jConstraint
     * @return
     * @throws JSONException
     */
    private static Constraint parseValueConstraint(JSONObject jConstraint) throws JSONException {
        Constraint constraint;
        Collection<String> dataTypes;
        JSONArray jDataTypes = jConstraint.optJSONArray("dataTypes");
        if(jDataTypes != null && jDataTypes.length()>0){
            dataTypes = new ArrayList<String>(jDataTypes.length());
            for(int i=0;i<jDataTypes.length();i++){
                String dataType = jDataTypes.getString(i);
                if(dataType != null && !dataType.isEmpty()){
                    dataTypes.add(dataType);
                }
            }
            if(dataTypes.isEmpty()){
                dataTypes = null; //if no one was successfully added set the list back to null
            }
        } else {
            dataTypes = null;
        }
        if(jConstraint.has("value") && !jConstraint.isNull("value")){
            constraint = new ValueConstraint(jConstraint.get("value"), dataTypes);
        } else {
            log.warn("Parsed ValueConstraint doese not define the required field \"value\"!");
            constraint = null;
        }
        return constraint;
    }

    /**
     * @param jConstraint
     * @return
     * @throws JSONException
     */
    private static Constraint parseReferenceConstraint(JSONObject jConstraint) throws JSONException {
        Constraint constraint;
        if(jConstraint.has("value") && !jConstraint.isNull("value")){
            constraint = new ReferenceConstraint(jConstraint.getString("value"));
        } else {
            log.warn("Parsed ValueConstraint doese not define the required field \"value\"!");
            constraint = null;
        }
        return constraint;
    }
}
