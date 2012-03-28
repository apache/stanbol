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

package org.apache.stanbol.rules.manager.atoms;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.stanbol.rules.base.api.RuleAtom;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.vocabulary.XSD;

public class LessThanAtomTest extends AtomTest {

    private ExpressionAtom variable1;
    private ExpressionAtom variable2;

    private ExpressionAtom literal1;
    private ExpressionAtom literal2;

    private ExpressionAtom typedLiteral1;
    private ExpressionAtom typedLiteral2;

    @Before
    public void setup() {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        this.factory = manager.getOWLDataFactory();

        variable1 = new VariableAtom(URI.create("http://kres.iks-project.eu/ontology/meta/variables#x"),
                false);
        variable2 = new VariableAtom(URI.create("http://kres.iks-project.eu/ontology/meta/variables#y"),
                false);

        literal1 = new StringAtom("some text");
        literal2 = new StringAtom("some other text");

        try {
            typedLiteral1 = new TypedLiteralAtom(new NumberAtom("3.0"), new ResourceAtom(new URI(
                    XSD.xdouble.getURI())));
            typedLiteral2 = new TypedLiteralAtom(new NumberAtom("5.0"), new ResourceAtom(new URI(
                    XSD.xdouble.getURI())));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testValidAtomWithVariableArguments() {

        RuleAtom ruleAtom = new LessThanAtom(variable1, variable2);

        execTest(ruleAtom);

    }

    @Test
    public void testValidAtomWithLiteralArguments() {

        RuleAtom ruleAtom = new LessThanAtom(literal1, literal2);

        execTest(ruleAtom);
    }

    @Test
    public void testValidAtomWithTypedLiteralArguments() {

        RuleAtom ruleAtom = new LessThanAtom(typedLiteral1, typedLiteral2);

        execTest(ruleAtom);
    }

}
