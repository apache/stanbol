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
package org.apache.stanbol.rules.adapters.clerezza.atoms;

import java.util.ArrayList;
import java.util.List;

import org.apache.clerezza.rdf.core.sparql.query.BuiltInCall;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.Expression;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaSparqlObject;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.StringFunctionAtom;
import org.apache.stanbol.rules.manager.atoms.VariableAtom;

/**
 * It adapts any LetAtom to the IRI built in call in Clerezza.
 * 
 * @author anuzzolese
 * 
 */
public class LetAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.LetAtom tmp = (org.apache.stanbol.rules.manager.atoms.LetAtom) ruleAtom;

        StringFunctionAtom parameterFunctionAtom = tmp.getParameterFunctionAtom();
        URIResource variableUriResource = tmp.getVariable();

        ClerezzaSparqlObject iriArgument;

        iriArgument = (ClerezzaSparqlObject) adapter.adaptTo(parameterFunctionAtom, ConstructQuery.class);

        List<Expression> iriArgumentExpressions = new ArrayList<Expression>();
        iriArgumentExpressions.add((Expression) iriArgument.getClerezzaObject());

        BuiltInCall iriBuiltInCall = new BuiltInCall("IRI", iriArgumentExpressions);

        if (variableUriResource instanceof VariableAtom) {
            String variableName = ((VariableAtom) variableUriResource).getVariableName();
            Variable variable = new Variable(variableName);

            List<Expression> bindArgumentExpressions = new ArrayList<Expression>();
            bindArgumentExpressions.add(iriBuiltInCall);
            bindArgumentExpressions.add(variable);

            BuiltInCall bindBuiltInCall = new BuiltInCall("BIND", iriArgumentExpressions);

            return (T) new ClerezzaSparqlObject(bindBuiltInCall);
        } else {
            throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
        }
    }

}
