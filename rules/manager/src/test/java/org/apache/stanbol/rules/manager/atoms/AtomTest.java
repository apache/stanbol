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

import static org.junit.Assert.fail;

import org.apache.stanbol.rules.base.api.RuleAtom;
import org.junit.Before;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public abstract class AtomTest {

    protected OWLDataFactory factory;

    @Before
    public void init() {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        this.factory = manager.getOWLDataFactory();
    }

    protected void execTest(RuleAtom ruleAtom) {
        String stanbolSyntax = ruleAtom.toString();
        if (stanbolSyntax == null) {
            fail(this.getClass().getCanonicalName() + " does not produce any rule in Stanbol syntax.");
        }
    }

}
