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
package org.apache.stanbol.ontologymanager.servicesapi.util;

import org.semanticweb.owlapi.model.IRI;

public final class StringUtils {

    /**
     * Restrict instantiation
     */
    private StringUtils() {}

    public static IRI stripIRITerminator(IRI iri) {
        if (iri == null) return null;
        return IRI.create(stripIRITerminator(iri.toString()));
    }

    public static String stripIRITerminator(String iri) {
        if (iri == null) return null;
        if (iri.endsWith("/") || iri.endsWith("#") || iri.endsWith(":"))
        // Shorten the string by one
        return stripIRITerminator(iri.substring(0, iri.length() - 1));
        else return iri;
    }

    public static String stripNamespace(String fullIri, String namespace) {
        if (fullIri.startsWith(namespace)) return fullIri.substring(namespace.length() - 1);
        else return fullIri;
    }

}
