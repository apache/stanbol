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

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;

public class LessEqualThanAtom extends ComparisonAtom {

    private ExpressionAtom argument1;
    private ExpressionAtom argument2;

    public LessEqualThanAtom(ExpressionAtom argument1, ExpressionAtom argument2) {
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    @Override
    public String toString() {
        return "leq(" + argument1.toString() + ", " + argument2.toString() + ")";
    }

    private OWLLiteral getOWLTypedLiteral(Object argument) {

        OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();

        OWLLiteral owlLiteral;
        if (argument instanceof String) {
            owlLiteral = factory.getOWLTypedLiteral((String) argument);
        } else if (argument instanceof Integer) {
            owlLiteral = factory.getOWLTypedLiteral(((Integer) argument).intValue());
        } else if (argument instanceof Double) {
            owlLiteral = factory.getOWLTypedLiteral(((Double) argument).doubleValue());
        } else if (argument instanceof Float) {
            owlLiteral = factory.getOWLTypedLiteral(((Float) argument).floatValue());
        } else if (argument instanceof Boolean) {
            owlLiteral = factory.getOWLTypedLiteral(((Boolean) argument).booleanValue());
        } else {
            owlLiteral = factory.getOWLStringLiteral(argument.toString());
        }

        return owlLiteral;
    }

    @Override
    public String prettyPrint() {
        return argument1.prettyPrint() + "<=" + argument2.prettyPrint();
    }

    public ExpressionAtom getArgument1() {
        return argument1;
    }

    public ExpressionAtom getArgument2() {
        return argument2;
    }
}
