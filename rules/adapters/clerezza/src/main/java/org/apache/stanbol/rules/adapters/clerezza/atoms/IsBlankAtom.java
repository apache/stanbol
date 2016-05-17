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

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.sparql.query.BuiltInCall;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.Expression;
import org.apache.clerezza.rdf.core.sparql.query.UriRefExpression;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaSparqlObject;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;

/**
 * It adapts any LengthAtom to the <code>isBLANK</code> built in callin Clerezza.
 * 
 * @author anuzzolese
 * 
 */
public class IsBlankAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.IsBlankAtom tmp = (org.apache.stanbol.rules.manager.atoms.IsBlankAtom) ruleAtom;

        IObjectAtom uriResource = tmp.getUriResource();

        ClerezzaSparqlObject argumentCSO = (ClerezzaSparqlObject) adapter.adaptTo(uriResource,
            ConstructQuery.class);
        ;

        Object arg = argumentCSO.getClerezzaObject();

        Expression argumentExpression;
        if (arg instanceof Variable) {
            argumentExpression = (Variable) arg;
        } else if (arg instanceof IRI) {
            argumentExpression = new UriRefExpression((IRI) arg);
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        List<Expression> expressions = new ArrayList<Expression>();
        expressions.add(argumentExpression);

        return (T) new ClerezzaSparqlObject(new BuiltInCall("isBLANK", expressions));

    }

}
