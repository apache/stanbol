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

import static org.junit.Assert.*;

import org.apache.stanbol.commons.owl.util.URIUtils;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

public class TestUriUtils {

    private String _BASE = "http://www.ontologydesignpatterns.org/registry/explanation";

    private IRI iri_hash = IRI.create(_BASE + ".owl#ExplanationSchemaCatalog");

    private IRI iri_slash = IRI.create(_BASE + "/ExplanationSchemaCatalog");

    private IRI iri_slash_end = IRI.create(_BASE + "/ExplanationSchemaCatalog/");

    private IRI iri_query = IRI.create(_BASE + "?arg1=value1&arg2=value2");

    private IRI iri_slash_query = IRI.create(_BASE + "/?arg1=value1&arg2=value2");

    /**
     * Test that every IRI configuration is stripped as expected.
     * 
     * @throws Exception
     */
    @Test
    public void testUpOne() throws Exception {
        assertEquals(_BASE + ".owl", URIUtils.upOne(iri_hash).toString());
        assertEquals(_BASE, URIUtils.upOne(iri_slash).toString());
        assertEquals(_BASE, URIUtils.upOne(iri_slash_end).toString());
        assertEquals(_BASE, URIUtils.upOne(iri_query).toString());
        assertEquals(_BASE + "/", URIUtils.upOne(iri_slash_query).toString());
    }

}
