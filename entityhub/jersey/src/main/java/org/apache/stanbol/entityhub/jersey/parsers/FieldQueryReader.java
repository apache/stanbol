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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
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

@Provider
public class FieldQueryReader implements MessageBodyReader<FieldQuery> {
    private static final Logger log = LoggerFactory.getLogger(FieldQueryReader.class);
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        log.info("isReadable type {}, mediaType {}",type,mediaType);
        return FieldQuery.class.isAssignableFrom(type); //&& mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public FieldQuery readFrom(Class<FieldQuery> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType,
                               MultivaluedMap<String,String> httpHeaders,
                               InputStream entityStream) throws IOException, WebApplicationException {
        String queryString = IOUtils.toString(entityStream);
        log.info("Parsed QueryString: \n{}",queryString);
        MediaType acceptedType = MediaType.valueOf(httpHeaders.getFirst("Accept"));
        if(acceptedType.isWildcardType()){
            acceptedType = MediaType.TEXT_PLAIN_TYPE;
        }
        try {
            return fromJSON(
                DefaultQueryFactory.getInstance(), 
                queryString,acceptedType);
        } catch (JSONException e) {
            log.error("Unable to parse Request ",e);
            StringBuilder message = new StringBuilder();
            message.append("Parsed FieldQuery is not valid JSON\n");
            message.append("Parsed String:\n");
            message.append(queryString);
            log.warn(message.toString());
            //TODO: Jersey wraps Exceptions thrown by MessageBodyReader into
            // other ones. Because of that the Response created by the
            // WebApplicationException is "lost" and the user will get an
            // 500 with no comment and HTML content type :(
            // As a workaround one could use a wrapping object as generic type
            // that parses the error and than throw the Exception within the
            // Resource using this MessageBodyReader
            throw new WebApplicationException(
                Response.status(Status.BAD_REQUEST).
                entity(message.toString()).
                header(HttpHeaders.ACCEPT, acceptedType.toString()).build());
        }
    }
    /**
     * 
     * @param queryFactory
     * @param jsonQueryString
     * @param acceptedMediaType used to add the accept header to Error responses
     * @return
     * @throws JSONException
     * @throws WebApplicationException
     */
    public static FieldQuery fromJSON(FieldQueryFactory queryFactory, String jsonQueryString,MediaType acceptedMediaType) throws JSONException,WebApplicationException{
        if(jsonQueryString == null){
            throw new IllegalArgumentException("The parsed JSON object MUST NOT be NULL!");
        }
        JSONObject jQuery = new JSONObject(jsonQueryString);
        FieldQuery query = queryFactory.createFieldQuery();
        if(!jQuery.has("constraints")){
            StringBuilder message = new StringBuilder();
            message.append("The parsed Field Query MUST contain at least a single 'constraints'\n");
            message.append("Parsed Query:\n");
            message.append(jQuery.toString(4));
            log.warn(message.toString());
            throw new WebApplicationException(
                Response.status(Status.BAD_REQUEST).entity(
                    message.toString()).header(HttpHeaders.ACCEPT, acceptedMediaType.toString())
                    .build());
        }
        JSONArray constraints = jQuery.getJSONArray("constraints");
        //collect all parsing Errors to report a complete set of all errors
        boolean parsingError = false;
        StringBuilder parsingErrorMessages = new StringBuilder();
        parsingErrorMessages.append("Constraint parsing Errors:\n");
        for(int i=0;i<constraints.length();i++){
            JSONObject jConstraint = constraints.getJSONObject(i);
            if(jConstraint.has("field")){
                String field = jConstraint.getString("field");
                //check if there is already a constraint for that field
                if(field == null || field.isEmpty()){
//                    log.warn("The value of the key \"field\" MUST NOT be NULL nor emtpy!");
//                    log.warn("Constraint:\n {}",jConstraint.toString(4));
                    parsingErrorMessages.append('\n');
                    parsingErrorMessages.append(
                        "Each Field Query Constraint MUST define a value for 'field'\n");
                    parsingErrorMessages.append("Parsed Constraint:\n");
                    parsingErrorMessages.append(jConstraint.toString(4));
                    parsingErrorMessages.append('\n');
                    parsingError = true;
                    continue;
                } else if(query.isConstrained(field)){
 //                   log.warn("Multiple constraints for field {} in parsed FieldQuery!",field);
                    parsingErrorMessages.append('\n');
                    parsingErrorMessages.append(
                        "The parsed Query defines multiple constraints fr the field '"
                        +field+"'!\n");
                    parsingErrorMessages.append("FieldQuery allows only a single Constraint for a field\n");
                    parsingErrorMessages.append("Parsed Constraints:\n");
                    parsingErrorMessages.append(constraints.toString(4));
                    parsingErrorMessages.append('\n');
                    parsingError = true;
                    continue;
                } else {
                    try {
                        query.setConstraint(field, parseConstraint(jConstraint));
                    } catch (IllegalArgumentException e) {
                        parsingErrorMessages.append('\n');
                        parsingErrorMessages.append(e.getMessage());
                        parsingErrorMessages.append('\n');
                        parsingError = true;
                        continue;
                    }
                }
            } else { //no field defined -> ignroe and write warning!
                parsingErrorMessages.append('\n');
                parsingErrorMessages.append("Constraints MUST define a value for 'field'\n");
                parsingErrorMessages.append("Parsed Constraint:\n");
                parsingErrorMessages.append(jConstraint.toString(4));
                parsingErrorMessages.append('\n');
                parsingError = true;
                continue;
            }
        }
        if(parsingError){
            String message = parsingErrorMessages.toString();
            log.warn(message);
            throw new WebApplicationException(
                Response.status(Status.BAD_REQUEST).entity(
                    message).header(HttpHeaders.ACCEPT, acceptedMediaType.toString())
                    .build());
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
                StringBuilder message = new StringBuilder();
                message.append("Parsed Constraint uses an unknown value for 'type'!\n");
                message.append("Supported values: ");
                message.append(ConstraintType.values());
                message.append('\n');
                message.append("Parsed Constraint: \n");
                message.append(jConstraint.toString(4));
                throw new IllegalArgumentException(message.toString());
            }
        } else {
            log.warn(String.format("Earch Constraint MUST HAVE the \"type\" key set to one of the values %s",
                Arrays.asList("reference",ConstraintType.values())));
            StringBuilder message = new StringBuilder();
            message.append("Parsed Constraint does not define a value for the field 'type'!\n");
            message.append("Supported values: ");
            message.append(ConstraintType.values());
            message.append('\n');
            message.append("Parsed Constraint: \n");
            message.append(jConstraint.toString(4));
            throw new IllegalArgumentException(message.toString());
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
            log.info("RangeConstraint does not define the field 'inclusive'. Use false as default!");
            inclusive = false;
        }
        Object upperBound = jConstraint.opt("upperBound");
        Object lowerBound = jConstraint.opt("lowerBound");
        if(upperBound == null && lowerBound == null){
            log.warn("Range Constraint does not define an 'upperBound' nor an 'lowerBound'! " +
            		"At least one of the two MUST BE parsed for a valid RangeConstraint.");
            StringBuilder message = new StringBuilder();
            message.append("Range Constraint does not define an 'upperBound' nor an 'lowerBound'!");
            message.append(" At least one of the two MUST BE parsed for a valid RangeConstraint.\n");
            message.append("Parsed Constraint: \n");
            message.append(jConstraint.toString(4));
            throw new IllegalArgumentException(message.toString());
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
                log.warn("Encountered unknown patternType for TextConstraint!",e);
                patternType = PatternType.none;
                StringBuilder message = new StringBuilder();
                message.append("Illegal value for field 'patternType'.\n");
                message.append("Supported values are: ");
                message.append(Arrays.toString(PatternType.values()));
                message.append('\n');
                message.append("Parsed Constraint: \n");
                message.append(jConstraint.toString(4));
                throw new IllegalArgumentException(message.toString());
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
            StringBuilder message = new StringBuilder();
            message.append("Parsed TextConstraint doese not define the required field 'text'!\n");
            message.append("Parsed Constraint: \n");
            message.append(jConstraint.toString(4));
            throw new IllegalArgumentException(message.toString());
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
            log.warn("Parsed ValueConstraint does not define the required field \"value\"!");
            StringBuilder message = new StringBuilder();
            message.append("Parsed ValueConstraint does not define the required field 'value'!\n");
            message.append("Parsed Constraint: \n");
            message.append(jConstraint.toString(4));
            throw new IllegalArgumentException(message.toString());
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
            log.warn("Parsed ReferenceConstraint does not define the required field \"value\"!");
            StringBuilder message = new StringBuilder();
            message.append("Parsed ReferenceConstraint does not define the required field 'value'!\n");
            message.append("Parsed Constraint: \n");
            message.append(jConstraint.toString(4));
            throw new IllegalArgumentException(message.toString());
        }
        return constraint;
    }
}
