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
import org.apache.stanbol.rules.base.api.URIResource;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.vocabulary.XSD;

public abstract class AbstractRuleAtom implements RuleAtom {

    protected Node getTypedLiteral(Object argument) {

        Node literal;
        if (argument instanceof TypedLiteralAtom) {
            TypedLiteralAtom typedLiteralAtom = (TypedLiteralAtom) argument;

            URIResource xsdType = typedLiteralAtom.getXsdType();

            if (xsdType.getURI().equals(XSD.xboolean)) {
                literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDboolean);
            } else if (xsdType.getURI().equals(XSD.xdouble)) {
                literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDdouble);
            } else if (xsdType.getURI().equals(XSD.xfloat)) {
                literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDfloat);
            } else if (xsdType.getURI().equals(XSD.xint)) {
                literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDint);
            } else {
                literal = Node_RuleVariable.createLiteral(argument.toString());
            }
        } else if (argument instanceof String) {
            literal = Node_RuleVariable.createLiteral((String) argument, null, XSDDatatype.XSDstring);
        } else if (argument instanceof Integer) {
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDinteger);
        } else if (argument instanceof Double) {
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDdouble);
        } else if (argument instanceof Float) {
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDfloat);
        } else if (argument instanceof Boolean) {
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDboolean);
        } else {
            literal = Node_RuleVariable.createLiteral((String) argument);
        }

        return literal;
    }

}
