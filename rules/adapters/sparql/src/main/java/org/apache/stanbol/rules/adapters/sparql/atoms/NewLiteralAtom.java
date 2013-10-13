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

package org.apache.stanbol.rules.adapters.sparql.atoms;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.sparql.SPARQLFunction;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;

/**
 * It adapts any NewLiteralAtom to a BIND operator in SPARQL, which binds to a variable a literal value.
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

        SPARQLObject binding = adapter.adaptTo(tmp.getBinding(), SPARQLObject.class);
        SPARQLObject variable = adapter.adaptTo(tmp.getNewNodeVariable(), SPARQLObject.class);

        SPARQLObject sparqlObject = new SPARQLFunction("BIND(" + binding.getObject() + " AS "
                                                       + variable.getObject() + ")");
        return (T) sparqlObject;

    }

}
