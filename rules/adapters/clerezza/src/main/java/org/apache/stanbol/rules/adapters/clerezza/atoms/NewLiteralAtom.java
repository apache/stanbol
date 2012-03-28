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
import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaSparqlObject;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;
import org.apache.stanbol.rules.manager.atoms.StringFunctionAtom;

/**
 * It adapts any NewLiteralAtom to the BIND built in call in Clerezza for creating new literals binding the
 * value to a variable.
 * 
 * @author anuzzolese
 * 
 */
public class NewLiteralAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.NewLiteralAtom tmp = (org.apache.stanbol.rules.manager.atoms.NewLiteralAtom) ruleAtom;

        StringFunctionAtom bindingAtom = tmp.getBinding();
        IObjectAtom variableAtom = tmp.getNewNodeVariable();

        ClerezzaSparqlObject binding = (ClerezzaSparqlObject) adapter.adaptTo(bindingAtom,
            ConstructQuery.class);
        ClerezzaSparqlObject variable = (ClerezzaSparqlObject) adapter.adaptTo(variableAtom,
            ConstructQuery.class);

        List<Expression> bindArgumentExpressions = new ArrayList<Expression>();
        bindArgumentExpressions.add((Expression) binding.getClerezzaObject());
        bindArgumentExpressions.add((Expression) variable.getClerezzaObject());

        BuiltInCall bindBuiltInCall = new BuiltInCall("BIND", bindArgumentExpressions);

        return (T) new ClerezzaSparqlObject(bindBuiltInCall);

    }

}
