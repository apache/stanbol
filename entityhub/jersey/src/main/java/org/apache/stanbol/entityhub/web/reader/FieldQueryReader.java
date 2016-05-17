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
package org.apache.stanbol.entityhub.web.reader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.query.FieldQueryImpl;
import org.apache.stanbol.entityhub.ldpath.query.LDPathFieldQueryImpl;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint.ConstraintType;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.RangeConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.SimilarityConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint.MODE;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Provider
public class FieldQueryReader implements MessageBodyReader<FieldQuery> {
    private static final Logger log = LoggerFactory.getLogger(FieldQueryReader.class);
    
    private static final ValueFactory valueFactory = InMemoryValueFactory.getInstance();
    private static final ValueConverterFactory converterFactory = ValueConverterFactory.getDefaultInstance();
    
    @Reference
    NamespacePrefixService namespacePrefixService;
    
    private NamespacePrefixService getNsPrefixService(){
        return namespacePrefixService;
    }
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        log.debug("isReadable type {}, mediaType {}",type,mediaType);
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
        log.debug("Parsed QueryString: \n{}",queryString);
        MediaType acceptedType = MediaType.valueOf(httpHeaders.getFirst("Accept"));
        if(acceptedType.isWildcardType()){
            acceptedType = MediaType.TEXT_PLAIN_TYPE;
        }
        try {
            return fromJSON(queryString,acceptedType, getNsPrefixService());
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
            // RDFTerm using this MessageBodyReader
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
    public static FieldQuery fromJSON(String jsonQueryString,MediaType acceptedMediaType, 
                                      NamespacePrefixService nsPrefixService) throws JSONException,WebApplicationException {
        if(jsonQueryString == null){
            throw new IllegalArgumentException("The parsed JSON object MUST NOT be NULL!");
        }
        JSONObject jQuery = new JSONObject(jsonQueryString);
        FieldQuery query;
        if(jQuery.has("ldpath")){ //STANBOL-417: support for using LDPath as select
            LDPathFieldQueryImpl ldPathQuery = new LDPathFieldQueryImpl();
            ldPathQuery.setLDPathSelect(jQuery.getString("ldpath"));
            query = ldPathQuery;
        } else {
            query = new FieldQueryImpl();
        }
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
                    parsingErrorMessages.append('\n');
                    parsingErrorMessages.append(
                        "Each Field Query Constraint MUST define a value for 'field'\n");
                    parsingErrorMessages.append("Parsed Constraint:\n");
                    parsingErrorMessages.append(jConstraint.toString(4));
                    parsingErrorMessages.append('\n');
                    parsingError = true;
                    continue;
                }
                String fieldUri = nsPrefixService.getFullName(field);
                if(fieldUri == null){
                    parsingErrorMessages.append('\n');
                    parsingErrorMessages.append(
                        "The 'field' '").append(field).append("uses an unknown namespace prefix '");
                    parsingErrorMessages.append(NamespaceMappingUtils.getPrefix(field)).append("'\n");
                    parsingErrorMessages.append("Parsed Constraint:\n");
                    parsingErrorMessages.append(jConstraint.toString(4));
                    parsingErrorMessages.append('\n');
                    parsingError = true;
                    continue;
                }else if(query.isConstrained(fieldUri)){
                    parsingErrorMessages.append('\n');
                    parsingErrorMessages.append("The parsed Query defines multiple constraints fr the field '").append(fieldUri).append("'!\n");
                    parsingErrorMessages.append("FieldQuery allows only a single Constraint for a field\n");
                    parsingErrorMessages.append("Parsed Constraints:\n");
                    parsingErrorMessages.append(constraints.toString(4));
                    parsingErrorMessages.append('\n');
                    parsingError = true;
                    continue;
                } else {
                    try {
                        query.setConstraint(fieldUri, parseConstraint(jConstraint,nsPrefixService));
                    } catch (IllegalArgumentException e) {
                        parsingErrorMessages.append('\n');
                        parsingErrorMessages.append(e.getMessage());
                        parsingErrorMessages.append('\n');
                        parsingError = true;
                        continue;
                    }
                }
            } else { //empty field
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
                selectedField = nsPrefixService.getFullName(selectedField);
                if(selectedField != null && !selectedField.isEmpty()){
                    query.addSelectedField(selectedField);
                }
            }
        } //else no selected fields -> funny but maybe someone do need only the ids
        //parse limit and offset
        if(jQuery.has("limit") && !jQuery.isNull("limit")){
            try {
                query.setLimit(jQuery.getInt("limit"));
            } catch (JSONException e) {
                parsingErrorMessages.append('\n');
                parsingErrorMessages.append("Property \"limit\" MUST BE a valid integer number!\n");
                parsingErrorMessages.append("Parsed Value:");
                parsingErrorMessages.append(jQuery.get("init"));
                parsingErrorMessages.append('\n');
                parsingError = true;
            }
        }
        if(jQuery.has("offset") && !jQuery.isNull("offset")){
            try {
                query.setOffset(jQuery.getInt("offset"));
            } catch (JSONException e) {
                parsingErrorMessages.append('\n');
                parsingErrorMessages.append("Property \"offset\" MUST BE a valid integer number!\n");
                parsingErrorMessages.append("Parsed Value:");
                parsingErrorMessages.append(jQuery.get("init"));
                parsingErrorMessages.append('\n');
                parsingError = true;
            }
        }
        return query;
    }

    private static Constraint parseConstraint(JSONObject jConstraint, NamespacePrefixService nsPrefixService) throws JSONException {
        final Constraint constraint;
        if(jConstraint.has("type") && !jConstraint.isNull("type")) {
            String type = jConstraint.getString("type");
            //Event that internally "reference" is not part of the
            //ConstraintType enum it is still present in the serialisation
            //ant the Java API (see ReferenceConstraint class)
            //Value constraints with the dataType Reference and AnyURI are
            //considered to represent reference constraints
            if(type.equals("reference")){
                constraint = parseReferenceConstraint(jConstraint,nsPrefixService);
            } else if (type.equals(ConstraintType.value.name())){
                constraint = parseValueConstraint(jConstraint, nsPrefixService);
            } else if (type.equals(ConstraintType.text.name())){
                constraint = parseTextConstraint(jConstraint);
            } else if (type.equals(ConstraintType.range.name())){
                constraint = parseRangeConstraint(jConstraint,nsPrefixService);
            } else if(type.equals(ConstraintType.similarity.name())){
                constraint = parseSimilarityConstraint(jConstraint, nsPrefixService);
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
        //finally parse the optional boost
        if(jConstraint.has("boost")){
            double boost = jConstraint.optDouble("boost");
            if(boost == Double.NaN || boost <= 0){
                StringBuilder message = new StringBuilder("The Boost of a Constraint " +
                		"MUST BE a double AND >= 0 (parsed: '");
                message.append(jConstraint.get("boost")).append("')!");
                log.warn(message.toString());
                throw new IllegalArgumentException(message.toString());
            } else {
                constraint.setBoost(boost);
            }
        } //else no boost defined
        return constraint;
    }

    private static Constraint parseSimilarityConstraint(JSONObject jConstraint, NamespacePrefixService nsPrefixService) throws JSONException {
        String context = jConstraint.optString("context");
        if(context == null){
            throw new IllegalArgumentException("SimilarityConstraints MUST define a \"context\": \n "+jConstraint.toString(4));
        }
        JSONArray addFields = jConstraint.optJSONArray("addFields");
        final List<String> fields;
        if(addFields != null && addFields.length() > 0){
            fields = new ArrayList<String>(addFields.length());
            for(int i=0;i<addFields.length();i++){
                String field = addFields.optString(i);
                field = field != null ? nsPrefixService.getFullName(field) : null;
                if(field != null && !field.isEmpty()){
                    fields.add(field);
                }
            }
        } else {
            fields = null;
        }
        return new SimilarityConstraint(context,fields);
    }

    /**
     * @param jConstraint
     * @return
     * @throws JSONException
     */
    private static Constraint parseRangeConstraint(JSONObject jConstraint, NamespacePrefixService nsPrefixService) throws JSONException {
        Constraint constraint;
        boolean inclusive;
        if(jConstraint.has("inclusive")){
            inclusive = jConstraint.getBoolean("inclusive");
        } else {
            log.debug("RangeConstraint does not define the field 'inclusive'. Use false as default!");
            inclusive = false;
        }
        Object upperBound = jConstraint.opt("upperBound");
        Object lowerBound = jConstraint.opt("lowerBound");
        Collection<String> datatypes = parseDatatypeProperty(jConstraint,nsPrefixService);
        if(datatypes != null && !datatypes.isEmpty()){
            Iterator<String> it = datatypes.iterator();
            String datatype = it.next();
            if(datatypes.size() > 1){ //write warning in case of multiple values
                log.warn("Multiple datatypes are not supported by RangeConstriants!");
                log.warn("  used: {}",datatype);
                while(it.hasNext()){
                    log.warn("  ignored: {}",it.next());
                }
            }
            StringBuilder convertingError = null;
            if(upperBound != null){
                Object convertedUpperBound = converterFactory.convert(upperBound, datatype, valueFactory);
                if(convertedUpperBound == null){
                    log.warn("Unable to convert upper bound {} to data type {}",
                        upperBound,datatype);
                    convertingError = new StringBuilder();
                    convertingError.append("Unable to convert the parsed upper bound value ")
                        .append(upperBound).append(" to data type ").append(datatype);
                } else { //set the converted upper bound
                    upperBound = convertedUpperBound;
                }
            }
            if(lowerBound != null){
                Object convertedLowerBound = converterFactory.convert(lowerBound, datatype, valueFactory);
                if(convertedLowerBound == null){
                    log.warn("Unable to convert lower bound {} to data type {}",
                        lowerBound,datatype);
                    if(convertingError == null){
                        convertingError = new StringBuilder();
                    } else {
                        convertingError.append('\n');
                    }
                    convertingError.append("Unable to convert the parsed value ")
                        .append(lowerBound).append(" to data type ").append(datatype);
                } else { //set the converted lower bound
                    lowerBound = convertedLowerBound;
                }
            }
            if(convertingError != null){ //if there was an error throw an exception
                convertingError.append("Parsed Constraint: \n");
                convertingError.append(jConstraint.toString(4));
                throw new IllegalArgumentException(convertingError.toString());
            }
        }
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
        final TextConstraint constraint;
        boolean caseSensitive = jConstraint.optBoolean("caseSensitive", false);
        //parse patternType
        PatternType patternType;
        String jPatternType = jConstraint.optString("patternType",null);
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
        String languageKey = null; //support both "languages" and "language"
        if(jConstraint.has("language")){
            languageKey = "language";
        } else if(jConstraint.has("languages")){
            log.warn("The key \"languages\" is deprecated. Use \"language\" instead.");
            languageKey = "languages";
        }
        if(languageKey != null){
            JSONArray jLanguages = jConstraint.optJSONArray(languageKey);
            if(jLanguages != null && jLanguages.length()>0){
                languages = new ArrayList<String>(jLanguages.length());
                for(int i=0;i<jLanguages.length();i++){
                    String lang = jLanguages.getString(i);
                    if(lang != null && !lang.isEmpty()){
                        languages.add(lang);
                    } else if(!languages.contains(null)){
                        languages.add(null);
                    }
                }
                if(languages.isEmpty()){
                    languages = null; //if no one was successfully added set the list back to null
                }
            } else {
                String language = jConstraint.getString(languageKey);
                if(language.isEmpty()){
                    languages = null;
                } else {  //add the single language
                    languages = Collections.singletonList(language);
                }
            }
        } else {
            languages = null;
        }
        //parse text and create constraint
        if(jConstraint.has("text") && !jConstraint.isNull("text")){
            List<String> textConstraints;
            JSONArray jTextConstraints = jConstraint.optJSONArray("text");
            if(jTextConstraints != null){
                textConstraints = new ArrayList<String>(jTextConstraints.length());
                for(int i=0;i<jTextConstraints.length();i++){
                    String text = jTextConstraints.getString(i);
                    if(text != null && !text.isEmpty()){
                        textConstraints.add(jTextConstraints.getString(i));
                    }
                }
            } else {
                String text = jConstraint.getString("text");
                if(text == null || text.isEmpty()){
                    textConstraints = Collections.emptyList();
                } else {
                    textConstraints = Collections.singletonList(text);
                }
            }
            if(textConstraints.isEmpty()){
                StringBuilder message = new StringBuilder();
                message.append("Parsed TextConstraint doese not define a valid (none empty) value for the 'text' property !\n");
                message.append("Parsed Constraint: \n");
                message.append(jConstraint.toString(4));
                throw new IllegalArgumentException(message.toString());
            }
            constraint = new TextConstraint(textConstraints,
                patternType,caseSensitive,
                languages == null?null:languages.toArray(new String[languages.size()]));
            //finally parse the optional termProximity
            if(jConstraint.has("proximityRanking")){
                constraint.setProximityRanking(jConstraint.optBoolean("proximityRanking", false));
            }
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
    private static Constraint parseValueConstraint(JSONObject jConstraint, NamespacePrefixService nsPrefixService) throws JSONException {
        Collection<String> dataTypes = parseDatatypeProperty(jConstraint, nsPrefixService);
        final List<Object> valueList;
        if(jConstraint.has("value") && !jConstraint.isNull("value")){
            Object value = jConstraint.get("value");
            if(value instanceof JSONArray){
                valueList = new ArrayList<Object>(((JSONArray)value).length());
                for(int i=0;i<((JSONArray)value).length();i++){
                    Object v = ((JSONArray)value).get(i);
                    if(v == null || v instanceof JSONArray || v instanceof JSONObject){
                        log.warn("Parsed ValueConstraint does define illegal values (values={})!",value);
                        StringBuilder message = new StringBuilder();
                        message.append("Parsed ValueConstraint does define illegal values for field 'value'" +
                        		"(value MUST NOT contain NULL, JSONObject nor JSONArray values)!\n");
                        message.append("Parsed Constraint: \n");
                        message.append(jConstraint.toString(4));
                        throw new IllegalArgumentException(message.toString());
                    }
                    valueList.add(v);
                }
            } else if(value instanceof JSONObject){
                log.warn("Parsed ValueConstraint does define illegal values (values={})!",value);
                StringBuilder message = new StringBuilder();
                message.append("Parsed ValueConstraint does define illegal value for field 'value'" +
                        "(value MUST NOT be an JSON object. Only values and JSONArray to parse" +
                        "multiple values are allowed)!\n");
                message.append("Parsed Constraint: \n");
                message.append(jConstraint.toString(4));
                throw new IllegalArgumentException(message.toString());
            } else {
                valueList = Collections.singletonList(jConstraint.get("value"));
            }
        } else {
            log.warn("Parsed ValueConstraint does not define the required field \"value\"!");
            StringBuilder message = new StringBuilder();
            message.append("Parsed ValueConstraint does not define the required field 'value'!\n");
            message.append("Parsed Constraint: \n");
            message.append(jConstraint.toString(4));
            throw new IllegalArgumentException(message.toString());
        }
        MODE mode = parseConstraintValueMode(jConstraint);
        return new ValueConstraint(valueList,dataTypes,mode);
    }

    /**
     * Parses the {@link MODE} for {@link ValueConstraint}s and 
     * {@link ReferenceConstraint}s, by evaluating the 'mode' attribute of
     * the parsed {@link JSONObject}
     * @param jConstraint the JSON formatted constraint
     * @return the parsed {@link MODE} or <code>null</code> if the 'mode'
     * attribute is not present
     * @throws JSONException if the value of the 'mode' is not an element of the
     * {@link MODE} enumeration.
     */
    private static MODE parseConstraintValueMode(JSONObject jConstraint) throws JSONException {
        MODE mode; 
        if(jConstraint.has("mode")){
            String jmode = jConstraint.getString("mode");
            try {
                mode = MODE.valueOf(jmode);
            } catch (IllegalArgumentException e) {
                String message = String.format("Parsed ValueConstraint defines an " +
                		"unknown MODE %s (supported: %s)!", jmode,
                		Arrays.asList(MODE.values()));
                log.warn(message,e);
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append(message).append('\n');
                errorMessage.append("Parsed Constraint: \n");
                errorMessage.append(jConstraint.toString(4));
                throw new IllegalArgumentException(message,e);
            }
        } else {
            mode = null;
        }
        return mode;
    }

    /**
     * @param jConstraint
     * @return
     * @throws JSONException
     */
    private static Collection<String> parseDatatypeProperty(JSONObject jConstraint,NamespacePrefixService nsPrefixService) throws JSONException {
        Collection<String> dataTypes;
        String dataTypeKey = null;
        if(jConstraint.has("datatype")){
            dataTypeKey = "datatype";
        } else if(jConstraint.has("dataTypes")){
            log.warn("The use of \"dataTypes\" is deprecated. Please use \"dataType\" instead");
            dataTypeKey = "dataTypes";
        }
        if(dataTypeKey != null){
            JSONArray jDataTypes = jConstraint.optJSONArray(dataTypeKey);
            if(jDataTypes != null && jDataTypes.length()>0){
                dataTypes = new ArrayList<String>(jDataTypes.length());
                for(int i=0;i<jDataTypes.length();i++){
                    String dataType = jDataTypes.getString(i);
                    //convert prefix:localName to fill URI
                    dataType = dataType != null ? nsPrefixService.getFullName(dataType) : null;
                    if(dataType != null && !dataType.isEmpty()){
                        dataTypes.add(dataType);
                    }
                }
                if(dataTypes.isEmpty()){
                    dataTypes = null; //if no one was successfully added set the list back to null
                }
            } else {
                String dataType = jConstraint.getString(dataTypeKey);
                //convert prefix:localName to fill URI
                dataType = dataType != null ? nsPrefixService.getFullName(dataType) : null;
                if(dataType != null && !dataType.isEmpty()){
                    dataTypes = Collections.singleton(dataType);
                } else {
                    dataTypes = null;
                }
            }
        } else {
            dataTypes = null;
        }
        return dataTypes;
    }

    /**
     * @param jConstraint
     * @return
     * @throws JSONException
     */
    private static Constraint parseReferenceConstraint(JSONObject jConstraint, NamespacePrefixService nsPrefixService) throws JSONException {
        final List<String> refList;
        if(jConstraint.has("value") && !jConstraint.isNull("value")){
            Object value = jConstraint.get("value");
            if(value instanceof JSONArray){
                refList = new ArrayList<String>(((JSONArray)value).length());
                for(int i=0;i<((JSONArray)value).length();i++){
                    String field = ((JSONArray)value).getString(i);
                    field = field != null ? nsPrefixService.getFullName(field) : null;
                    if(field != null && !field.isEmpty()){
                        refList.add(field);
                    }
                }
            } else if(value instanceof JSONObject){
                log.warn("Parsed ValueConstraint does define illegal values (values={})!",value);
                StringBuilder message = new StringBuilder();
                message.append("Parsed ValueConstraint does define illegal value for field 'value'" +
                        "(value MUST NOT be an JSON object. Only values and JSONArray to parse" +
                        "multiple values are allowed)!\n");
                message.append("Parsed Constraint: \n");
                message.append(jConstraint.toString(4));
                throw new IllegalArgumentException(message.toString());
            } else {
                String field = jConstraint.getString("value");
                field = field != null ? nsPrefixService.getFullName(field) : null;
                if(field != null){
                    refList = Collections.singletonList(field);
                } else {
                    refList = Collections.emptyList();
                }
            }
            if(refList.isEmpty()){
                log.warn("Parsed ReferenceConstraint does not define a single valid \"value\"!");
                StringBuilder message = new StringBuilder();
                message.append("Parsed ReferenceConstraint does not define a single valid 'value'!\n");
                message.append("This means values where only null, empty string or '{prefix}:{localname}' values with unknown {prefix}\n");
                message.append("Parsed Constraint: \n");
                message.append(jConstraint.toString(4));
                throw new IllegalArgumentException(message.toString());                
            }
            MODE mode = parseConstraintValueMode(jConstraint);
            return new ReferenceConstraint(refList,mode);
        } else {
            log.warn("Parsed ReferenceConstraint does not define the required field \"value\"!");
            StringBuilder message = new StringBuilder();
            message.append("Parsed ReferenceConstraint does not define the required field 'value'!\n");
            message.append("Parsed Constraint: \n");
            message.append(jConstraint.toString(4));
            throw new IllegalArgumentException(message.toString());
        }
    }
}
