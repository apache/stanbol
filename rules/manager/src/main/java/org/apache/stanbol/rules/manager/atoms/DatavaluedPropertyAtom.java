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

import org.apache.stanbol.rules.base.api.RuleAtom;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;

public class DatavaluedPropertyAtom extends CoreAtom {

    private IObjectAtom datatypeProperty;
    private IObjectAtom argument1;
    private RuleAtom argument2;

    public DatavaluedPropertyAtom(IObjectAtom datatypeProperty, IObjectAtom argument1, RuleAtom argument2) {
        this.datatypeProperty = datatypeProperty;
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    @Override
    public String prettyPrint() {
        return "Individual " + argument1.toString() + " has datatype property " + argument1.toString()
               + " with value " + argument2.toString();
    }

    private SWRLLiteralArgument getSWRLTypedLiteral(OWLDataFactory factory, Object argument) {

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

        return factory.getSWRLLiteralArgument(owlLiteral);
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
    public String toString() {

        return "values(" + datatypeProperty.toString() + ", " + argument1.toString() + ", "
               + argument2.toString() + ")";

        /*
         * String arg1 = null; String arg2 = null; String arg3 = null;
         * 
         * if(argument1.toString().startsWith(Symbols.variablesPrefix)){ arg1 =
         * "?"+argument1.toString().replace(Symbols.variablesPrefix, ""); VariableAtom variable =
         * (VariableAtom) argument1; if(variable.isNegative()){ arg1 = "notex(" + arg1 + ")"; } } else{ arg1 =
         * argument1.toString(); }
         * 
         * 
         * if(datatypeProperty.toString().startsWith(Symbols.variablesPrefix)){ arg3 =
         * "?"+datatypeProperty.toString().replace(Symbols.variablesPrefix, ""); VariableAtom variable =
         * (VariableAtom) datatypeProperty; if(variable.isNegative()){ arg3 = "notex(" + arg3 + ")"; } } else{
         * arg3 = datatypeProperty.toString(); }
         * 
         * if(argument2.toString().startsWith(Symbols.variablesPrefix)){ arg2 =
         * "?"+argument2.toString().replace(Symbols.variablesPrefix, "");
         * 
         * VariableAtom variable = (VariableAtom) argument2; if(variable.isNegative()){ arg2 = "notex(" + arg2
         * + ")"; }
         * 
         * return "values(" + arg3 + ", " + arg1 + ", " + arg2 +")"; } else{ OWLLiteral literal =
         * getOWLTypedLiteral(argument2);
         * 
         * return "values(" + arg3 + ", " + arg1 + ", " + literal.getLiteral() +")"; }
         */
    }

    public IObjectAtom getArgument1() {
        return argument1;
    }

    public RuleAtom getArgument2() {
        return argument2;
    }

    public IObjectAtom getDatatypeProperty() {
        return datatypeProperty;
    }

}
