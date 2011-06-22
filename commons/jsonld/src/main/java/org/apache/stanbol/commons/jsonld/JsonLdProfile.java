package org.apache.stanbol.commons.jsonld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONArray;

public class JsonLdProfile extends JsonLdCommon {

    private Map<String,List<String>> typesMap = new HashMap<String,List<String>>();

    public void addType(String property, String type) {
        if (this.typesMap.get(property) == null) {
            this.typesMap.put(property, new ArrayList<String>());
        }
        List<String> types = this.typesMap.get(property);
        types.add(type);
    }
    
    public void addTypes(String property, List<String> types) {
        if (this.typesMap.get(property) == null) {
            this.typesMap.put(property, new ArrayList<String>());
        }
        this.typesMap.get(property).addAll(types);
    }

    public List<String> getTypes(String property) {
        return this.typesMap.get(property);
    }
    
    public Set<String> getTypes() {
        return typesMap.keySet();
    }

    public String toString() {
        Map<String,Object> json = createJson();

        return JsonSerializer.toString(json);
    }

    public String toString(int indent) {
        Map<String,Object> json = createJson();

        return JsonSerializer.toString(json, indent);
    }

    private Map<String,Object> createJson() {
        Map<String,Object> json = new TreeMap<String,Object>(new JsonComparator());

        // put the namespaces
        Map<String,Object> contextObject = new TreeMap<String,Object>(new JsonComparator());
        for (String ns : namespacePrefixMap.keySet()) {
            contextObject.put(namespacePrefixMap.get(ns), ns);
        }

        // put types
        Map<String,Object> typesObject = new TreeMap<String,Object>(new JsonComparator());
        for (String property : this.typesMap.keySet()) {
            List<String> types = this.typesMap.get(property);
            if (types.size() == 1) {
                typesObject.put(handleCURIEs(property), handleCURIEs(types.get(0)));
            } else {
                JSONArray typesArray = new JSONArray();
                for (String type : types) {
                    typesArray.put(handleCURIEs(type));
                }
                typesObject.put(handleCURIEs(property), typesArray);
            }
        }
        contextObject.put(JsonLdCommon.TYPES, typesObject);

        json.put(JsonLdCommon.CONTEXT, contextObject);

        return json;
    }
}
