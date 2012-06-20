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
package org.apache.stanbol.ontologymanager.ontonet;

import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.parser;
import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.ClerezzaOWLUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;

public class TestUtilities {

    private OWLOntologyID expectedOntologyID = new OWLOntologyID(
            IRI.create("http://stanbol.apache.org/ontologies/test1.owl"));

    @BeforeClass
    public static void cleanup() throws Exception {
        reset();
    }

    @Test
    public void testLookahead() throws Exception {
        InputStream content = getClass().getResourceAsStream("/ontologies/test1.owl");
        OWLOntologyID id = ClerezzaOWLUtils.guessOntologyID(content, parser, SupportedFormat.RDF_XML);
        assertNotNull(id);
        assertEquals(expectedOntologyID, id);
    }

}
