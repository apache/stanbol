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
import org.apache.stanbol.rules.base.api.Symbols;

/**
 * It adapts any UObjectAtom to a SPARQL object.
 * 
 * @author anuzzolese
 * 
 */
public class UObjectAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) {

        org.apache.stanbol.rules.manager.atoms.UObjectAtom tmp = (org.apache.stanbol.rules.manager.atoms.UObjectAtom) ruleAtom;

        int actualType = tmp.getActualType();
        Object argument = tmp.getArgument();

        String argumentSPARQL = null;

        switch (actualType) {
            case 0:
                argumentSPARQL = "\"" + argument + "\"^^<http://www.w3.org/2001/XMLSchema#string>";
                break;
            case 1:
                argumentSPARQL = argument.toString() + "^^<http://www.w3.org/2001/XMLSchema#int>";
                break;
            case 2:
                argumentSPARQL = "?" + argument.toString().replace(Symbols.variablesPrefix, "");
                break;
            default:
                break;
        }

        if (argumentSPARQL != null) {
            return (T) new SPARQLFunction(argumentSPARQL);
        } else {
            return null;
        }
    }
}
