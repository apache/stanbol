package org.apache.stanbol.entityhub.jersey.writers;

import java.util.Map.Entry;

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
                    jConstraint.put("vakue", valueConstraint.getValue());
                }
                if (valueConstraint.getDataTypes() != null && !valueConstraint.getDataTypes().isEmpty()) {
                    jConstraint.put("dataTypes", valueConstraint.getDataTypes());
                }
                break;
            case text:
                TextConstraint textConstraint = (TextConstraint) constraint;
                if (textConstraint.getLanguages() != null && !textConstraint.getLanguages().isEmpty()) {
                    jConstraint.put("languages", new JSONArray(textConstraint.getLanguages()));
                }
                jConstraint.put("patternType", textConstraint.getPatternType().name());
                if (textConstraint.getText() != null && !textConstraint.getText().isEmpty()) {
                    jConstraint.put("text", textConstraint.getText());
                }
                break;
            case range:
                RangeConstraint rangeConstraint = (RangeConstraint) constraint;
                if (rangeConstraint.getLowerBound() != null) {
                    jConstraint.put("lowerBound", rangeConstraint.getLowerBound().toString());
                }
                if (rangeConstraint.getUpperBound() != null) {
                    jConstraint.put("upperBound", rangeConstraint.getUpperBound().toString());
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
