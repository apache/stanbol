package org.apache.stanbol.commons.jsonld;

import java.util.HashMap;
import java.util.Map;

public abstract class JsonLdCommon {

    public static final String CONTEXT = "@context";
    public static final String COERCION = "@coercion";
    public static final String IRI = "@iri";
    public static final String PROFILE = "@profile";
    public static final String SUBJECT = "@";
    public static final String TYPES = "#types";
    
    protected Map<String,String> namespacePrefixMap = new HashMap<String,String>();
    /**
     * Flag to control whether the namespace prefix map should be used to shorten URIs to CURIEs during
     * serialization. Default value is <code>true</code>.
     */
    protected boolean applyNamespaces = true;

    /**
     * Get the known namespace to prefix mapping.
     * 
     * @return A {@link Map} from namespace String to prefix String.
     */
    public Map<String,String> getNamespacePrefixMap() {
        return this.namespacePrefixMap;
    }

    /**
     * Sets the known namespaces for the serializer.
     * 
     * @param namespacePrefixMap
     *            A {@link Map} from namespace String to prefix String.
     */
    public void setNamespacePrefixMap(Map<String,String> namespacePrefixMap) {
        this.namespacePrefixMap = namespacePrefixMap;
    }

    /**
     * Adds a new namespace and its prefix to the list of used namespaces for this JSON-LD instance.
     * 
     * @param namespace
     *            A namespace IRI.
     * @param prefix
     *            A prefix to use and identify this namespace in serialized JSON-LD.
     */
    public void addNamespacePrefix(String namespace, String prefix) {
        namespacePrefixMap.put(namespace, prefix);
    }
    
    /**
     * Flag to control whether the namespace prefix map should be used to shorten IRIs to prefix notation
     * during serialization. Default value is <code>true</code>.
     * <p>
     * If you already put values into this JSON-LD instance with prefix notation, you should set this to
     * <code>false</code> before starting the serialization.
     * 
     * @return <code>True</code> if namespaces are applied during serialization, <code>false</code> otherwise.
     */
    public boolean isApplyNamespaces() {
        return applyNamespaces;
    }

    /**
     * Control whether namespaces from the namespace prefix map are applied to URLs during serialization.
     * <p>
     * Set this to <code>false</code> if you already have shortened IRIs with prefixes.
     * 
     * @param applyNamespaces
     */
    public void setApplyNamespaces(boolean applyNamespaces) {
        this.applyNamespaces = applyNamespaces;
    }
    
    /**
     * Convert URI to CURIE if namespaces should be applied and CURIEs to URIs if namespaces should not be
     * applied.
     * 
     * @param uri
     *            That may be in CURIE form.
     * @return
     */
    protected String handleCURIEs(String uri) {
        if (this.applyNamespaces) {
            uri = doCURIE(uri);
        } else {
            uri = unCURIE(uri);
        }

        return uri;
    }

    public String doCURIE(String uri) {
        for (String namespace : namespacePrefixMap.keySet()) {
            String prefix = namespacePrefixMap.get(namespace) + ":";
            if (!uri.startsWith(prefix)) {
                uri = uri.replace(namespace, prefix);
            }
        }
        return uri;
    }

    public String unCURIE(String uri) {
        for (String namespace : namespacePrefixMap.keySet()) {
            String prefix = namespacePrefixMap.get(namespace) + ":";
            if (uri.startsWith(prefix)) {
                uri = uri.replace(prefix, namespace);
            }
        }
        return uri;
    }
}
