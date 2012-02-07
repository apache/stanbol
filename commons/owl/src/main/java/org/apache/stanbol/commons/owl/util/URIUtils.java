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

import org.apache.clerezza.rdf.core.UriRef;
import org.semanticweb.owlapi.model.IRI;

public class URIUtils {

    /**
     * Removes either the fragment, or query, or last path component from a URI, whatever it finds first.
     * 
     * @param iri
     * @return
     */
    public static IRI upOne(IRI iri) {
        return upOne(iri.toURI());
    }

    public static IRI createIRI(UriRef uri) {
        return IRI.create(uri.getUnicodeString());
    }

    public static UriRef createUriRef(IRI iri) {
        return new UriRef(iri.toString());
    }

    /**
     * Strips terminating hashes.
     * 
     * @param iri
     * @return
     */
    public static IRI sanitizeID(IRI iri) {
        if (iri == null) throw new IllegalArgumentException("Cannot sanitize null IRI.");
        String s = iri.toString();
        while (s.endsWith("#"))
            s = s.substring(0, s.length() - 1);
        return IRI.create(s);
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
        else
        // Strip the query
        {
            frag = uri.getQuery();
            if (frag != null && !frag.isEmpty()) index = tmpstr.length() - frag.length() - 1;
            else
            // Strip the slash part
            {
                frag = uri.getPath();
                if (frag != null && !frag.isEmpty()) {
                    int i = frag.lastIndexOf("/");
                    boolean trimslash = false;
                    // If it ends with a slash, remove that too
                    if (i == frag.length() - 1) {
                        trimslash = true;
                        frag = frag.substring(0, i);
                    }
                    index = tmpstr.length() - frag.length() + frag.lastIndexOf("/") + (trimslash ? -1 : 0);
                }
            }
        }
        if (index >= 0) return IRI.create(tmpstr.substring(0, index));
        else return IRI.create(uri);
    }

}
