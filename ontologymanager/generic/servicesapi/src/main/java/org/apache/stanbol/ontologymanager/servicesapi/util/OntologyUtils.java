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

import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.stanbol.commons.owl.util.URIUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;

public final class OntologyUtils {

    /**
     * Restrict instantiation
     */
    private OntologyUtils() {}

    private static String[] preferredFormats = {RDF_XML, TURTLE, X_TURTLE, RDF_JSON, N3, N_TRIPLE,
                                                MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML};

    /**
     * Extracts an OWL Ontology ID from its standard string form. The string must be of type
     * <tt>ontologyIRI[:::versionIRI]</tt>. Any substring <tt>"%3A%3A%3A"</tt> present in <tt>ontologyIRI</tt>
     * or <tt>versionIRI</tt> will be URL-decoded (i.e. converted to <tt>":::"</tt>).<br/>
     * <br/>
     * Also note that both <tt>ontologyIRI</tt> and <tt>versionIRI</tt> are desanitized in the process.
     * 
     * @param stringForm
     *            the string to decode
     * @return the string form of this ID.
     * @see URIUtils#desanitize(IRI)
     */
    public static OWLOntologyID decode(String stringForm) {
        if (stringForm == null || stringForm.isEmpty()) throw new IllegalArgumentException(
                "Supplied string form must be non-null and non-empty.");
        IRI oiri, viri;
        String[] split = stringForm.split(":::");
        if (split.length >= 1) {
            oiri = URIUtils.desanitize(IRI.create(split[0].replace("%3A%3A%3A", ":::")));
            viri = (split.length > 1) ? URIUtils.desanitize(IRI.create(split[1].replace("%3A%3A%3A", ":::")))
                    : null;
            return (viri != null) ? new OWLOntologyID(oiri, viri) : new OWLOntologyID(oiri);
        } else return null; // Anonymous but versioned ontologies are not acceptable.
    }

    /**
     * Provides a standardized string format for an OWL Ontology ID. The string returned is of type
     * <tt>ontologyIRI[:::versionIRI]</tt>. Any substring <tt>":::"</tt> present in <tt>ontologyIRI</tt> or
     * <tt>versionIRI</tt> will be URL-encoded (i.e. converted to <tt>"%3A%3A%3A"</tt>).<br/>
     * <br/>
     * Also note that both <tt>ontologyIRI</tt> and <tt>versionIRI</tt> are sanitized in the process. No other
     * URL encoding occurs.
     * 
     * @param id
     *            the OWL ontology ID to encode
     * @return the string form of this ID.
     * @see URIUtils#sanitize(IRI)
     */
    public static String encode(OWLOntologyID id) {
        if (id == null) throw new IllegalArgumentException("Cannot encode a null OWLOntologyID.");
        if (id.getOntologyIRI() == null) throw new IllegalArgumentException(
                "Cannot encode an OWLOntologyID that is missing an ontologyIRI.");
        String s = "";
        s += URIUtils.sanitize(id.getOntologyIRI()).toString().replace(":::", "%3A%3A%3A");
        if (id.getVersionIRI() != null) s += (":::")
                                             + URIUtils.sanitize(id.getVersionIRI()).toString()
                                                     .replace(":::", "%3A%3A%3A");
        return s;
    }

    public static List<String> getPreferredFormats() {
        List<String> result = new ArrayList<String>();
        Collections.addAll(result, preferredFormats);
        return result;
    }

    public static List<String> getPreferredSupportedFormats(Collection<String> supported) {
        List<String> result = new ArrayList<String>();
        for (String f : preferredFormats)
            if (supported.contains(f)) result.add(f);
        /*
        // The non-preferred supported formats on the tail in any order
        for (String f : supported)
            if (!result.contains(f)) result.add(f);
        */
        return result;
    }

}
