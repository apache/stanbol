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

package org.apache.stanbol.rules.adapters.jena;

import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.atoms.TypedLiteralAtom;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * It provides a static method (<code>getTypedLiteral</code>) that allows to convert an object to {@link Node}
 * that implements a typed literal in Jena.
 * 
 * @author anuzzolese
 * 
 */
public final class NodeFactory {

    /**
     * Restrict instantiation
     */
    private NodeFactory() {}

    /**
     * The argument is converted to a Jena {@link Node}
     * 
     * @param argument
     *            any Object
     * @return the {@link Node}
     */
    public static Node getTypedLiteral(Object argument) {

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
        } else if(argument instanceof String) {
            
            System.out.println(argument);
            String argString = (String) argument;
            if(argString.startsWith("\"") && argString.endsWith("\"")){
                argString = argString.substring(1, argString.length()-1);
            }
            literal = Node_RuleVariable.createLiteral(argString);
        } else if(argument instanceof Integer) {
            
            literal = Node_RuleVariable.createLiteral(argument.toString(), null, XSDDatatype.XSDint);
        } else {
            literal = Node_RuleVariable.createLiteral(argument.toString());
        }

        return literal;
    }

}
