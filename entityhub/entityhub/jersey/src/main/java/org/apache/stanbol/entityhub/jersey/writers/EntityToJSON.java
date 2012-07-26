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

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.apache.stanbol.entityhub.core.utils.TimeUtils;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


final class EntityToJSON {

    private EntityToJSON() { /* do not create instances of utility classes */}

    static JSONObject toJSON(Entity entity) throws JSONException {
        JSONObject jSign;
//        if (entity instanceof Symbol) {
//            jSign = writeSymbolAsJSON((Symbol) entity);
//        } else if (entity instanceof EntityMapping) {
//            jSign = writeEntityMappingAsJSON((EntityMapping) entity);
//        } else {
            jSign = convertEntityToJSON(entity);
//        }
        return jSign;
    }

//    private static JSONObject writeSymbolAsJSON(Symbol symbol) throws JSONException {
//        JSONObject jSymbol = convertSignToJSON(symbol);
//        jSymbol.put("label", symbol.getLabel());
//        Iterator<Text> descriptions = symbol.getDescriptions();
//        if (descriptions.hasNext()) {
//            jSymbol.put("description", convertFieldValuesToJSON(descriptions));
//        }
//        Collection<String> value = ModelUtils.asCollection(symbol.getPredecessors());
//        if (!value.isEmpty()) {
//            jSymbol.put("predecessors", value);
//        }
//        value = ModelUtils.asCollection(symbol.getSuccessors());
//        if (!value.isEmpty()) {
//            jSymbol.put("successors", new JSONArray());
//        }
//        jSymbol.put("stateUri", symbol.getState().getUri());
//        jSymbol.put("state", symbol.getState().name());
//        return jSymbol;
//    }

//    private static JSONObject writeEntityMappingAsJSON(EntityMapping entityMapping) throws JSONException {
//        JSONObject jEntityMapping = convertSignToJSON(entityMapping);
//        jEntityMapping.put("symbol", entityMapping.getTargetId());
//        jEntityMapping.put("entity", entityMapping.getSourceId());
//        jEntityMapping.put("stateUri", entityMapping.getState().getUri());
//        jEntityMapping.put("state", entityMapping.getState().name());
//        return jEntityMapping;
//    }


    /**
     * @param entity
     * @return
     * @throws JSONException
     */
    private static JSONObject convertEntityToJSON(Entity entity) throws JSONException {
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
    static JSONObject toJSON(Representation rep) throws JSONException {
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
    private static JSONArray convertFieldValuesToJSON(Iterator<?> values) throws JSONException {
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
    private static JSONObject convertFieldValueToJSON(Object value) throws JSONException {
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
}
