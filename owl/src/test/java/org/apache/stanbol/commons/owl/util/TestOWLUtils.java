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

import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestOWLUtils {

    @BeforeClass
    public static void setupTests() throws Exception {
        TcManager.getInstance().addWeightedTcProvider(new SimpleTcProvider());
    }

    private ParsingProvider pp = new JenaParserProvider();

    private UriRef uri = new UriRef("ontonet:http://stanbol.apache.org/prova");

    @Test
    public void namedUriRef() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/owl/maincharacters.owl");
        MGraph mg = TcManager.getInstance().createMGraph(uri);
        pp.parse(mg, inputStream, "application/rdf+xml", uri);
        assertNotNull(OWLUtils.guessOntologyIdentifier(mg.getGraph()));
    }

    @Test
    public void namelessUriRef() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/owl/nameless_ontology.owl");
        MGraph mg = TcManager.getInstance().createMGraph(uri);
        pp.parse(mg, inputStream, "application/rdf+xml", uri);
//        No longer null!
        assertNotNull(OWLUtils.guessOntologyIdentifier(mg.getGraph()));
    }

    @After
    public void reset() throws Exception {
        TcManager.getInstance().deleteTripleCollection(uri);
    }

}
