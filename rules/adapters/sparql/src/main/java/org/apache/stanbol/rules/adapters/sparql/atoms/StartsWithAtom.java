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
import org.apache.stanbol.rules.adapters.sparql.SPARQLComparison;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.StringFunctionAtom;

/**
 * It adapts any StartsWithAtom to the <http://www.w3.org/2005/xpath-functions#starts-with> function of XPath.
 * 
 * @author anuzzolese
 * 
 */
public class StartsWithAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption {

        org.apache.stanbol.rules.manager.atoms.StartsWithAtom tmp = (org.apache.stanbol.rules.manager.atoms.StartsWithAtom) ruleAtom;

        StringFunctionAtom argument = tmp.getArgument();
        StringFunctionAtom term = tmp.getTerm();

        try {
            SPARQLObject sparqlArgument = adapter.adaptTo(argument, SPARQLObject.class);
            SPARQLObject sparqlTerm = adapter.adaptTo(term, SPARQLObject.class);

            String argumentSparql = sparqlArgument.getObject();
            String termSparql = sparqlTerm.getObject();

            return (T) new SPARQLComparison("<http://www.w3.org/2005/xpath-functions#starts-with> ("
                                            + argumentSparql + ", " + termSparql + ")");

        } catch (UnsupportedTypeForExportException e) {
            throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
        } catch (UnavailableRuleObjectException e) {
            throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
        }

    }

}
