package org.apache.stanbol.entityhub.jersey.writers;

import java.util.Collection;
import java.util.Iterator;

import org.apache.stanbol.entityhub.core.utils.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


final class SignToJSON {

    private SignToJSON() { /* do not create instances of utility classes */}

    static JSONObject toJSON(Sign sign) throws JSONException {
        JSONObject jSign;
        if (sign instanceof Symbol) {
            jSign = writeSymbolAsJSON((Symbol) sign);
        } else if (sign instanceof EntityMapping) {
            jSign = writeEntityMappingAsJSON((EntityMapping) sign);
        } else {
            jSign = convertSignToJSON(sign);
        }
        return jSign;
    }

    private static JSONObject writeSymbolAsJSON(Symbol symbol) throws JSONException {
        JSONObject jSymbol = convertSignToJSON(symbol);
        jSymbol.put("label", symbol.getLabel());
        Iterator<Text> descriptions = symbol.getDescriptions();
        if (descriptions.hasNext()) {
            jSymbol.put("description", convertFieldValuesToJSON(descriptions));
        }
        Collection<String> value = ModelUtils.asCollection(symbol.getPredecessors());
        if (!value.isEmpty()) {
            jSymbol.put("predecessors", value);
        }
        value = ModelUtils.asCollection(symbol.getSuccessors());
        if (!value.isEmpty()) {
            jSymbol.put("successors", new JSONArray());
        }
        jSymbol.put("stateUri", symbol.getState().getUri());
        jSymbol.put("state", symbol.getState().name());
        return jSymbol;
    }

    private static JSONObject writeEntityMappingAsJSON(EntityMapping entityMapping) throws JSONException {
        JSONObject jEntityMapping = convertSignToJSON(entityMapping);
        jEntityMapping.put("symbol", entityMapping.getSymbolId());
        jEntityMapping.put("entity", entityMapping.getEntityId());
        jEntityMapping.put("stateUri", entityMapping.getState().getUri());
        jEntityMapping.put("state", entityMapping.getState().name());
        return jEntityMapping;
    }


    /**
     * @param sign
     * @return
     * @throws JSONException
     */
    private static JSONObject convertSignToJSON(Sign sign) throws JSONException {
        JSONObject jSign;
        jSign = new JSONObject();
        jSign.put("id", sign.getId());
        jSign.put("site", sign.getSignSite());
        Representation rep = sign.getRepresentation();
        jSign.put("representation", toJSON(rep));
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
        } else if (value instanceof Text) {
            jValue.put("type", "text");
            jValue.put("xml:lang", ((Text) value).getLanguage());
        } else {
            jValue.put("type", "value");//TODO: better name? ^^
        }
        jValue.put("value", value.toString());
        return jValue;
    }
}
