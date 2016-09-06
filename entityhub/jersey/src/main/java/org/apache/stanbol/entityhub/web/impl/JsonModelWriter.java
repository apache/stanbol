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

package org.apache.stanbol.entityhub.web.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.utils.TimeUtils;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.web.ModelWriter;
import org.apache.stanbol.entityhub.web.fieldquery.FieldQueryToJsonUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * Component that supports serialising Entityhub Model classes as 
 * {@link MediaType#APPLICATION_JSON}.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true, metatype=true, policy=ConfigurationPolicy.OPTIONAL)
@Service
public class JsonModelWriter implements ModelWriter {

        
    /**
     * The list of supported media types as returned by {@link #supportedMediaTypes()}
     * containing only {@link MediaType#APPLICATION_JSON_TYPE}
     */
    public static final List<MediaType> SUPPORTED_MEDIA_TYPES = 
            Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);

    /**
     * Allows to enable pretty format and setting the indent. If %lt;= 0 
     * pretty format is deactivated.
     */
    @Property(intValue=JsonModelWriter.DEFAULT_INDENT)
    public static final String PROEPRTY_INDENT = "entityhub.web.writer.json.indent";
    /**
     * The default for {@link #PROEPRTY_INDENT} is <code>-1</code>.
     */
    public static final int DEFAULT_INDENT = -1;

    /**
     * The optional {@link NamespacePrefixService} is used for serialising
     * {@link QueryResultList}s that do use namespace prefixes
     */
    @org.apache.felix.scr.annotations.Reference(
        cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected NamespacePrefixService nsPrefixService;

    private int indent;
    
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        Object value =ctx.getProperties().get(PROEPRTY_INDENT);
        if(value instanceof Number){
            indent = ((Number)value).intValue();
        } else if(value != null){
            try {
                indent = Integer.parseInt(value.toString());
            } catch(NumberFormatException e){
                throw new ConfigurationException(PROEPRTY_INDENT, 
                    "The parsed indent MUST BE an Integer number (values <= 0 "
                    + "will deactivate pretty format)");
            }
        } else {
            indent = DEFAULT_INDENT;
        }
    }
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        indent = -1;
    }
        
    @Override
    public Class<? extends Representation> getNativeType() {
        return null; //no native type
    }

    @Override
    public List<MediaType> supportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    public MediaType getBestMediaType(MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType) ?
                MediaType.APPLICATION_JSON_TYPE : null;
    }

    @Override
    public void write(Representation rep, OutputStream out, MediaType mediaType) 
            throws WebApplicationException, IOException {
        try {
            writeJsonObject(toJSON(rep), out,getCharset(mediaType));
        } catch (JSONException e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void write(Entity entity, OutputStream out, MediaType mediaType) 
            throws WebApplicationException, IOException {
        try {
            writeJsonObject(toJSON(entity), out,getCharset(mediaType));
        } catch (JSONException e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void write(QueryResultList<?> result, OutputStream out, MediaType mediaType) 
            throws WebApplicationException, IOException {
        try {
            writeJsonObject(toJSON(result,nsPrefixService), out,getCharset(mediaType));
        } catch (JSONException e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }

    }
    private JSONObject toJSON(Entity entity) throws JSONException {
        return convertEntityToJSON(entity);
    }

    /**
     * Writes the {@link JSONObject} to the stream
     * @param jObject the object to write
     * @param out the output stream
     * @param charset the charset
     * @throws IOException
     * @throws JSONException
     */
    private void writeJsonObject(JSONObject jObject, OutputStream out, String charset) 
            throws IOException, JSONException {
        IOUtils.write(indent > 0 ? jObject.toString(indent) : jObject.toString(), 
                out, charset);
    }

    /**
     * @param mediaType
     * @return
     */
    private String getCharset(MediaType mediaType) {
        String charset = mediaType.getParameters().get("charset");
        if(charset == null){
            charset = ModelWriter.DEFAULT_CHARSET;
        }
        return charset;
    }

    /**
     * @param entity
     * @return
     * @throws JSONException
     */
    private JSONObject convertEntityToJSON(Entity entity) throws JSONException {
        JSONObject jSign;
        jSign = new JSONObject();
        jSign.put("id", entity.getId());
        jSign.put("site", entity.getSite());
//        Representation rep = sign.getRepresentation();
        jSign.put("representation", toJSON(entity.getRepresentation()));
        jSign.put("metadata", toJSON(entity.getMetadata()));
        return jSign;
    }

    /**
     * Converts the {@link Representation} to JSON
     *
     * @param jSign
     * @param rep
     * @throws JSONException
     */
    private JSONObject toJSON(Representation rep) throws JSONException {
        JSONObject jRep = new JSONObject();
        jRep.put("id", rep.getId());
        for (Iterator<String> fields = rep.getFieldNames(); fields.hasNext();) {
            String field = fields.next();
            Iterator<Object> values = rep.get(field);
            if (values.hasNext()) {
                jRep.put(field, convertFieldValuesToJSON(values));
            }
        }
        return jRep;
    }

    /**
     * @param values Iterator over all the values to add
     * @return The {@link JSONArray} with all the values as {@link JSONObject}
     * @throws JSONException
     */
    private JSONArray convertFieldValuesToJSON(Iterator<?> values) throws JSONException {
        JSONArray jValues = new JSONArray();
        while (values.hasNext()) {
            jValues.put(convertFieldValueToJSON(values.next()));
        }
        return jValues;
    }

    /**
     * The value to write. Special support for  {@link Reference} and {@link Text}.
     * The {@link #toString()} Method is used to write the "value" key.
     *
     * @param value the value
     * @return the {@link JSONObject} representing the value
     * @throws JSONException
     */
    private JSONObject convertFieldValueToJSON(Object value) throws JSONException {
        JSONObject jValue = new JSONObject();
        if (value instanceof Reference) {
            jValue.put("type", "reference");
            jValue.put("xsd:datatype", DataTypeEnum.AnyUri.getShortName());
            jValue.put("value", ((Reference)value).getReference());
        } else if (value instanceof Text) {
            jValue.put("type", "text");
            jValue.put("xml:lang", ((Text) value).getLanguage());
            jValue.put("value", ((Text)value).getText());
        } else if(value instanceof Date){
            jValue.put("type", "value");
            jValue.put("value", TimeUtils.toString(DataTypeEnum.DateTime, (Date)value));
            jValue.put("xsd:datatype", DataTypeEnum.DateTime.getShortName());
        } else {
            jValue.put("type", "value");
            Set<DataTypeEnum> dataTypes = DataTypeEnum.getPrimaryDataTypes(value.getClass());
            if(!dataTypes.isEmpty()){
                jValue.put("xsd:datatype", dataTypes.iterator().next().getShortName());
            } else {
                jValue.put("xsd:datatype", DataTypeEnum.String.getShortName());
            }
            jValue.put("value", value);
        }
        return jValue;
    }
    
    private <T> JSONObject toJSON(QueryResultList<?> resultList, NamespacePrefixService nsPrefixService) throws JSONException{
        JSONObject jResultList = new JSONObject();
        if(resultList.getQuery() != null){
            jResultList.put("query", FieldQueryToJsonUtils.toJSON(resultList.getQuery(),nsPrefixService));
        }
        jResultList.put("results", convertResultsToJSON(resultList,resultList.getType()));
        return jResultList;
    }

    private <T> JSONArray convertResultsToJSON(Iterable<?> results,Class<?> type) throws JSONException{
        JSONArray jResults = new JSONArray();
        if(String.class.isAssignableFrom(type)){
            for(Object result : results){
                jResults.put(result);
            }
        } else if(Representation.class.isAssignableFrom(type)){
            for(Object result : results){
                jResults.put(toJSON((Representation)result));
            }
        } else if(Entity.class.isAssignableFrom(type)){
            for(Object result : results){
                jResults.put(toJSON((Entity)result));
            }
        }
        return jResults;
    }

}
