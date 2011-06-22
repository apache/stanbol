package org.apache.stanbol.commons.jsonld;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JsonLdParserCommon {

    private static Logger logger = LoggerFactory.getLogger(JsonLdParserCommon.class);
    
    /**
     * Uses the underlying Jettison to parse a JSON object.
     * 
     * @param jsonString
     *            JSON String representation.
     * @return
     */
    protected static JSONObject parseJson(String jsonString) throws Exception {
        JSONObject jo = null;
        try {
            jo = new JSONObject(jsonString);
        } catch (JSONException e) {
            logger.info("Could not parse JSON string: {}", jsonString, e);
            throw new Exception("Could not parse JSON string: " + jsonString, e);
        }

        return jo;
    }
}
