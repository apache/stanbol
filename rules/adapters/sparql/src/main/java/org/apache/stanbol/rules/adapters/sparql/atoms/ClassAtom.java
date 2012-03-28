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

import java.util.ArrayList;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.sparql.SPARQLNot;
import org.apache.stanbol.rules.adapters.sparql.SPARQLTriple;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;

/**
 * It adapts any ClassAtom to a triple pattern of SPARQL.
 * 
 * @author anuzzolese
 * 
 */
public class ClassAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {
        String argument1SPARQL = null;
        String argument2SPARQL = null;

        boolean negativeArg = false;
        boolean negativeClass = false;

        org.apache.stanbol.rules.manager.atoms.ClassAtom tmp = (org.apache.stanbol.rules.manager.atoms.ClassAtom) ruleAtom;

        SPARQLObject sparqlArgument1 = adapter.adaptTo(tmp.getArgument1(), SPARQLObject.class);
        SPARQLObject sparqlArgument2 = adapter.adaptTo(tmp.getClassResource(), SPARQLObject.class);

        if (negativeArg || negativeClass) {
            String optional = sparqlArgument1.getObject()
                              + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
                              + sparqlArgument2.getObject();

            ArrayList<String> filters = new ArrayList<String>();
            if (negativeArg) {
                filters.add("!bound(" + argument1SPARQL + ")");
            }
            if (negativeClass) {
                filters.add("!bound(" + argument2SPARQL + ")");
            }

            String[] filterArray = new String[filters.size()];
            filterArray = filters.toArray(filterArray);

            return (T) new SPARQLNot(optional, filterArray);
        } else {
            return (T) new SPARQLTriple(sparqlArgument1.getObject()
                                        + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
                                        + sparqlArgument2.getObject());
        }

    }
}
