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

import org.apache.clerezza.rdf.core.sparql.query.BinaryOperation;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.Expression;
import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaSparqlObject;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.ExpressionAtom;

/**
 * It adapts any DifferentAtom to not equal to (!=) binary operation in Clerezza.
 * 
 * @author anuzzolese
 * 
 */
public class DifferentAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.DifferentAtom tmp = (org.apache.stanbol.rules.manager.atoms.DifferentAtom) ruleAtom;

        ExpressionAtom stringFunctionAtom1 = tmp.getStringFunctionAtom1();
        ExpressionAtom stringFunctionAtom2 = tmp.getStringFunctionAtom1();

        ClerezzaSparqlObject argument1 = (ClerezzaSparqlObject) adapter.adaptTo(stringFunctionAtom1,
            ConstructQuery.class);
        ClerezzaSparqlObject argument2 = (ClerezzaSparqlObject) adapter.adaptTo(stringFunctionAtom2,
            ConstructQuery.class);

        Expression lhe = (Expression) argument1.getClerezzaObject();
        Expression rhe = (Expression) argument2.getClerezzaObject();

        BinaryOperation binaryOperation = new BinaryOperation("!=", lhe, rhe);

        return (T) new ClerezzaSparqlObject(binaryOperation);

    }

}
