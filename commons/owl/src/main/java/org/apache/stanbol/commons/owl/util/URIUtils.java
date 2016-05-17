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
package org.apache.stanbol.commons.owl.util;

import java.net.URI;

import org.semanticweb.owlapi.model.IRI;

/**
 * A collection of utility methods for manipulating strings that can be converted to URIs or IRIs.
 * 
 * @author alexdma
 * 
 */
public final class URIUtils {

    /**
     * Restrict instantiation
     */
    private URIUtils() {}

    /**
     * Converts a IRI to an IRI.
     * 
     * @param uri
     *            the IRI to convert
     * @return the IRI form of the IRI
     */
    public static IRI createIRI(org.apache.clerezza.commons.rdf.IRI uri) {
        return IRI.create(uri.getUnicodeString());
    }

    /**
     * Converts an IRI to a IRI.
     * 
     * @param uri
     *            the IRI to convert
     * @return the IRI form of the IRI
     */
    public static org.apache.clerezza.commons.rdf.IRI createIRI(IRI uri) {
        return new org.apache.clerezza.commons.rdf.IRI(uri.toString());
    }

    /**
     * URL-decodes any terminating hash characters ("%23"). Any non-terminating URL-encoded hashes will be
     * left as they are (since there should be no intermediate hashes in a URL).
     * 
     * @param iri
     *            the IRI to desanitize
     * @return the desanitized IRI
     */
    public static IRI desanitize(IRI iri) {
        return IRI.create(desanitize(iri.toString()));
    }

    /**
     * URL-decodes any terminating hash characters ("%23"). Any non-terminating URL-encoded hashes will be
     * left as they are (since there should be no intermediate hashes in a URL).
     * 
     * @param iri
     *            the IRI in string form to desanitize
     * @return the desanitized IRI in string form
     */
    public static String desanitize(String iri) {
        if (iri == null || iri.isEmpty()) throw new IllegalArgumentException("Cannot desanitize null IRI.");
        while (iri.endsWith("%23"))
            iri = iri.substring(0, iri.length() - "%23".length()) + "#";
        return iri;
    }

    /**
     * Replaces terminating hash ('#') characters with their URL-encoded versions ("%23"). Any non-terminating
     * hashes will be left as they are (though they are not to be expected if the IRI denotes an URL).
     * 
     * @param iri
     *            the IRI to sanitize
     * @return the sanitized IRI
     */
    public static IRI sanitize(IRI iri) {
        return IRI.create(sanitize(iri.toString()));
    }

    /**
     * Replaces terminating hash ('#') characters with their URL-encoded versions ("%23"). Any non-terminating
     * hashes will be left as they are (though they are not to be expected if the string denotes an URL).
     * 
     * @param iri
     *            the IRI in string form to sanitize
     * @return the sanitized IRI in string form
     */
    public static String sanitize(String iri) {
        if (iri == null || iri.isEmpty()) throw new IllegalArgumentException("Cannot sanitize null IRI.");
        while (iri.endsWith("#"))
            iri = iri.substring(0, iri.length() - "#".length()) + "%23";
        return iri;
    }

    /**
     * Removes either the fragment, or query, or last path component from a URI, whatever it finds first.
     * 
     * @param iri
     * @return
     */
    public static IRI upOne(IRI iri) {
        return upOne(iri.toURI());
    }

    /**
     * Removes either the fragment, or query, or last path component from a URI, whatever it finds first.
     * 
     * @param uri
     * @return
     */
    public static IRI upOne(URI uri) {
        int index = -1;
        String tmpstr = uri.toString();
        // Strip the fragment
        String frag = uri.getFragment();
        if (frag != null && !frag.isEmpty()) index = tmpstr.length() - frag.length() - 1;
        else {// Strip the query
            frag = uri.getQuery();
            if (frag != null && !frag.isEmpty()) index = tmpstr.length() - frag.length() - 1;
            else { // Strip the slash part
                frag = uri.getPath();
                if (frag != null && !frag.isEmpty()) {
                    int i = frag.lastIndexOf('/');
                    boolean trimslash = false;
                    // If it ends with a slash, remove that too
                    if (i == frag.length() - 1) {
                        trimslash = true;
                        frag = frag.substring(0, i);
                    }
                    index = tmpstr.length() - frag.length() + frag.lastIndexOf('/') + (trimslash ? -1 : 0);
                }
            }
        }
        if (index >= 0) return IRI.create(tmpstr.substring(0, index));
        else return IRI.create(uri);
    }

}
