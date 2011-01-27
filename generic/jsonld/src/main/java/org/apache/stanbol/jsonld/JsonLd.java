package org.apache.stanbol.jsonld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Fabian Christ
 *
 */
public class JsonLd {

    // Map Namespace -> Prefix
    private Map<String, String> namespacePrefixMap = new HashMap<String, String>();

    // Map Subject -> Resource
    private Map<String, JsonLdResource> resourceMap = new TreeMap<String, JsonLdResource>(new JsonComparator());

    /**
     * Flag to control whether the namespace prefix map should be used
     * to shorten IRIs to prefix notation during serialization. Default
     * value is <code>true</code>.<br />
     * <br />
     * If you already put values into this JSON-LD instance with prefix
     * notation, you should set this to <code>false</code> before starting
     * the serialization.
     */
    private boolean applyNamespaces = true;

    /**
     * Flag to control whether the serialized JSON-LD output will use
     * joint or disjoint graphs for subjects and namespaces.  Default
     * value is <code>true</code>.
     */
    private boolean useJointGraphs = true;

    /**
     * Add the given resource to this JsonLd object using the resourceId
     * as key.
     *
     * @param resourceId
     * @param resource
     */
    public void put(String resourceId, JsonLdResource resource) {
        this.resourceMap.put(resourceId, resource);
    }

    @Override
    public String toString() {
        if (useJointGraphs) {
            Map<String, Object> json = createJointGraph();

            return JsonSerializer.toString(json);
        }
        else {
            List<Object> json = createDisjointGraph();

            return JsonSerializer.toString(json);
        }
    }

    public String toString(int indent) {
        if (useJointGraphs) {
            Map<String, Object> json = createJointGraph();

            return JsonSerializer.toString(json, indent);
        }
        else {
            List<Object> json = createDisjointGraph();

            return JsonSerializer.toString(json, indent);
        }
    }

    private List<Object> createDisjointGraph() {
        List<Object> json = new ArrayList<Object>();
        if (!resourceMap.isEmpty()) {

            for (String subject : resourceMap.keySet()) {
                Map<String, Object> subjectObject = new TreeMap<String, Object>(new JsonComparator());

                // put the namespaces
                if (!namespacePrefixMap.isEmpty()) {
                    Map<String, Object> nsObject = new TreeMap<String, Object>(new JsonComparator());
                    for (String ns : namespacePrefixMap.keySet()) {
                        nsObject.put(namespacePrefixMap.get(ns), ns);
                    }
                    subjectObject.put("#", nsObject);
                }

                JsonLdResource resource = resourceMap.get(subject);

                // put subject
                if (resource.getSubject() != null) {
                    subjectObject.put("@", resource.getSubject());
                }

                // put types
                putTypes(subjectObject, resource);

                // put properties = objects
                putProperties(subjectObject, resource);

                // add to list of subjects
                json.add(subjectObject);
            }

        }

        return json;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createJointGraph() {
        Map<String, Object> json = new TreeMap<String, Object>(new JsonComparator());
        if (!resourceMap.isEmpty()) {
            List<Object> subjects = new ArrayList<Object>();

            for (String subject : resourceMap.keySet()) {
                // put subject
                Map<String, Object> subjectObject = new TreeMap<String, Object>(new JsonComparator());

                JsonLdResource resource = resourceMap.get(subject);

                // put subject
                if (resource.getSubject() != null) {
                    subjectObject.put("@", resource.getSubject());
                }

                // put types
                putTypes(subjectObject, resource);

                // put properties = objects
                putProperties(subjectObject, resource);

                // add to list of subjects
                subjects.add(subjectObject);
            }

            // put subjects
            if (!subjects.isEmpty()) {
                if (subjects.size() == 1) {
                    json = (Map<String, Object>) subjects.get(0);
                } else {
                    json.put("@", subjects);
                }
            }
        }

        // put the namespaces
        if (!namespacePrefixMap.isEmpty()) {
            Map<String, Object> nsObject = new TreeMap<String, Object>(new JsonComparator());
            for (String ns : namespacePrefixMap.keySet()) {
                nsObject.put(namespacePrefixMap.get(ns), ns);
            }
            json.put("#", nsObject);
        }

        return json;
    }

    private void putTypes(Map<String, Object> subjectObject, JsonLdResource resource) {
        if (!resource.getTypes().isEmpty()) {
            List<String> types = new ArrayList<String>();
            for (String type : resource.getTypes()) {
                types.add(applyNamespace(type));
            }
            if (types.size() == 1) {
                subjectObject.put("a", types.get(0));
            }
            else {
                Collections.sort(types, new Comparator<String>() {

                    @Override
                    public int compare(String arg0, String arg1) {
                        return arg0.compareTo(arg1);
                    }

                });
                subjectObject.put("a", types);
            }
        }
    }

    private void putProperties(Map<String, Object> jsonObject, JsonLdResource resource) {
        for (String property : resource.getPropertyMap().keySet()) {
            Object value = resource.getPropertyMap().get(property);
            if (value instanceof String) {
                value = applyNamespace((String) value);
                jsonObject.put(applyNamespace(property), value);
            }
            else if (value instanceof String[]) {
                String[] stringArray = (String[]) value;
                List<String> valueList = new ArrayList<String>();
                for (String uri : stringArray) {
                    valueList.add(applyNamespace(uri));
                }
                List<Object> jsonArray = new ArrayList<Object>(valueList);
                jsonObject.put(applyNamespace(property), jsonArray);
            }
            else if (value instanceof Object[]) {
                Object[] objectArray = (Object[]) value;
                List<Object> jsonArray = new ArrayList<Object>();
                for (Object object : objectArray) {
                    jsonArray.add(object);
                }
                jsonObject.put(applyNamespace(property), jsonArray);
            }
            else {
                jsonObject.put(applyNamespace(property), value);
            }
        }
    }

    private String applyNamespace(String uri) {
        if (applyNamespaces) {
            for (String namespace : namespacePrefixMap.keySet()) {
                String prefix = namespacePrefixMap.get(namespace) + ":";
                uri = uri.replaceAll(namespace, prefix);
            }
        }
        return uri;
    }

    /**
     * Return the JSON-LD Resource for the given subject.
     */
    public JsonLdResource getResource(String subject) {
        return resourceMap.get(subject);
    }

    /**
     * Get the known namespace to prefix mapping.
     *
     * @return A {@link Map} from namespace String to prefix String.
     */
    public Map<String, String> getNamespacePrefixMap() {
        return namespacePrefixMap;
    }

    /**
     * Sets the known namespaces for the serializer.
     *
     * @param namespacePrefixMap
     *            A {@link Map} from namespace String to prefix
     *            String.
     */
    public void setNamespacePrefixMap(Map<String, String> namespacePrefixMap) {
        this.namespacePrefixMap = namespacePrefixMap;
    }

    /**
     * Adds a new namespace and its prefix to the list of used namespaces for this
     * JSON-LD instance.
     *
     * @param namespace A namespace IRI.
     * @param prefix A prefix to use and identify this namespace in serialized JSON-LD.
     */
    public void addNamespacePrefix(String namespace, String prefix) {
        namespacePrefixMap.put(namespace, prefix);
    }

    /**
     * Determine whether currently joint or disjoint graphs are serialized with this JSON-LD instance.
     *
     * @return <code>True</code> if joint graphs are used, <code>False</code>otherwise.
     */
    public boolean isUseJointGraphs() {
        return useJointGraphs;
    }

    /**
     * Set to <code>true</code> if you want to use joint graphs (default) or <code>false</code> otherwise.
     *
     * @param useJointGraphs
     */
    public void setUseJointGraphs(boolean useJointGraphs) {
        this.useJointGraphs = useJointGraphs;
    }

    /**
     * Flag to control whether the namespace prefix map should be used
     * to shorten IRIs to prefix notation during serialization. Default
     * value is <code>true</code>.
     * <p>
     * If you already put values into this JSON-LD instance with prefix
     * notation, you should set this to <code>false</code> before starting
     * the serialization.
     *
     * @return <code>True</code> if namespaces are applied during serialization, <code>false</code> otherwise.
     */
    public boolean isApplyNamespaces() {
        return applyNamespaces;
    }

    /**
     * Control whether namespaces from the namespace prefix map are
     * applied to URLs during serialization.
     * <p>
     * Set this to <code>false</code> if you already have shortened IRIs
     * with prefixes.
     *
     * @param applyNamespaces
     */
    public void setApplyNamespaces(boolean applyNamespaces) {
        this.applyNamespaces = applyNamespaces;
    }

}
