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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.scope;

import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.onManager;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.reset;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.sources.clerezza.GraphContentInputSource;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAxiomInterpretation {

    @BeforeClass
    public static void setup() throws Exception {
        reset();
    }

    @Test
    public void testCustomAboxCoreTbox() throws Exception {
        String path = "/ontologies/imports-disconnected";

        InputStream content = getClass().getResourceAsStream(path + "/abox.owl");
        OntologyInputSource<?> coreSrc = new GraphContentInputSource(content, SupportedFormat.TURTLE);
        Scope scope = onManager.createOntologyScope("imports-disconnected", coreSrc);
        assertNotNull(scope);

        content = getClass().getResourceAsStream(path + "/tbox.owl");
        OntologyInputSource<?> custSrc = new GraphContentInputSource(content, SupportedFormat.TURTLE);
        scope.getCustomSpace().addOntology(custSrc);

        ImmutableGraph g = scope.export(ImmutableGraph.class, true);

        // for (Triple t : g)
        // System.out.println(t);
        //
        // OWLOntology o = scope.export(OWLOntology.class, true);
        // for (OWLAxiom ax : o.getAxioms())
        // System.out.println(ax);

    }

    @After
    public void cleanup() throws Exception {
        reset();
    }

}
