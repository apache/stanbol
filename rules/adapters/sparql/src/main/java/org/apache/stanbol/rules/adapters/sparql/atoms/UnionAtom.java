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
import org.apache.stanbol.rules.base.api.util.AtomList;

/**
 * It adapts any UnionAtom to a union term in SPARQL.
 * 
 * @author anuzzolese
 * 
 */
public class UnionAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption {

        org.apache.stanbol.rules.manager.atoms.UnionAtom tmp = (org.apache.stanbol.rules.manager.atoms.UnionAtom) ruleAtom;

        AtomList atomList1 = tmp.getAtomList1();
        AtomList atomList2 = tmp.getAtomList2();

        String scope1 = "";

        for (RuleAtom inGroupRuleAtom : atomList1) {
            if (!scope1.isEmpty()) {
                scope1 += " . ";
            }

            try {

                SPARQLObject inGroupSparqlAtom = adapter.adaptTo(inGroupRuleAtom, SPARQLObject.class);

                scope1 += inGroupSparqlAtom.getObject();

            } catch (UnsupportedTypeForExportException e) {
                throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
            } catch (UnavailableRuleObjectException e) {
                throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
            }

        }

        String scope2 = "";

        for (RuleAtom inGroupRuleAtom : atomList2) {
            if (!scope2.isEmpty()) {
                scope2 += " . ";
            }

            try {

                SPARQLObject inGroupSparqlAtom = adapter.adaptTo(inGroupRuleAtom, SPARQLObject.class);

                scope2 += inGroupSparqlAtom.getObject();

            } catch (UnsupportedTypeForExportException e) {
                throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
            } catch (UnavailableRuleObjectException e) {
                throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
            }

        }

        String sparqlUnion = " { " + scope1 + " } UNION { " + scope2 + " } ";

        return (T) new SPARQLFunction(sparqlUnion);
    }

}
