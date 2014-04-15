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

package org.apache.stanbol.rules.adapters.swrl;

import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.atoms.TypedLiteralAtom;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;

import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Return a {@link SWRLIArgument} from a literal object represented according to Stanbol rules.
 * 
 * @author anuzzolese
 * 
 */
public final class SWRLLiteralBuilder {

    /**
     * Restrict instantiation
     */
    private SWRLLiteralBuilder() {}

    public static SWRLLiteralArgument getSWRLLiteral(Object argument) {

        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        OWLLiteral owlLiteral;

        if (argument instanceof TypedLiteralAtom) {
            TypedLiteralAtom typedLiteralAtom = (TypedLiteralAtom) argument;

            URIResource xsdType = typedLiteralAtom.getXsdType();

            if (xsdType.getURI().equals(XSD.xboolean)) {
                owlLiteral = factory.getOWLLiteral(Boolean.valueOf(argument.toString()).booleanValue());
            } else if (xsdType.getURI().equals(XSD.xdouble)) {
                owlLiteral = factory.getOWLLiteral(Double.valueOf(argument.toString()).doubleValue());
            } else if (xsdType.getURI().equals(XSD.xfloat)) {
                owlLiteral = factory.getOWLLiteral(Float.valueOf(argument.toString()).floatValue());
            } else if (xsdType.getURI().equals(XSD.xint)) {
                owlLiteral = factory.getOWLLiteral(Integer.valueOf(argument.toString()).intValue());
            } else {
                owlLiteral = factory.getOWLLiteral(argument.toString());
            }
        } else if (argument instanceof String) {
            owlLiteral = factory.getOWLLiteral((String) argument);
        } else if (argument instanceof Integer) {
            owlLiteral = factory.getOWLLiteral(((Integer) argument).intValue());
        } else if (argument instanceof Double) {
            owlLiteral = factory.getOWLLiteral(((Double) argument).doubleValue());
        } else if (argument instanceof Float) {
            owlLiteral = factory.getOWLLiteral(((Float) argument).floatValue());
        } else if (argument instanceof Boolean) {
            owlLiteral = factory.getOWLLiteral(((Boolean) argument).booleanValue());
        } else {
            owlLiteral = factory.getOWLLiteral(argument.toString());
        }

        return factory.getSWRLLiteralArgument(owlLiteral);
    }

}
