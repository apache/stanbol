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
package org.apache.stanbol.commons.jsonld;

import java.util.HashMap;
import java.util.Map;

public abstract class JsonLdCommon {

    public static final String CONTEXT = "@context";
    public static final String COERCE = "@coerce";
    
    public static final String LITERAL = "@literal";
    public static final String DATATYPE = "@datatype";
    
    public static final String IRI = "@iri";
    
    public static final String SUBJECT = "@subject";
    public static final String TYPE = "@type";
    
    public static final String PROFILE = "@profile";
    public static final String TYPES = "@types";

    /**
     * Maps URIs to namespace prefixes.
     */
    protected Map<String,String> namespacePrefixMap = new HashMap<String,String>();
    
    /**
     * Internal map to hold the namespaces and prefixes that were actually used.
     */
    protected Map<String, String> usedNamespaces = new HashMap<String, String>();
    
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
        String curie = uri;
        if (uri != null) {
            for (String namespace : namespacePrefixMap.keySet()) {
                String prefix = namespacePrefixMap.get(namespace);
                String prefixEx = prefix + ":";
                
                if (!uri.startsWith(prefix)) {
                    curie = curie.replace(namespace, prefixEx);
                    
                    if (!uri.equals(curie)) {
                        // we mark this namespace as being used
                        this.usedNamespaces.put(namespace, prefix);
                        break;
                    }
                }
                else {
                    // we mark this namespace as being used
                    this.usedNamespaces.put(namespace, prefix);
                    break;
                }
            }
        }
        return curie;
    }

    public String unCURIE(String uri) {
        if (uri != null) {
            for (String namespace : namespacePrefixMap.keySet()) {
                String prefix = namespacePrefixMap.get(namespace);
                String prefixEx = prefix + ":";                
                
                if (uri.startsWith(prefixEx)) {
                    uri = uri.replace(prefixEx, namespace);
                }
            }
        }
        return uri;
    }
}
