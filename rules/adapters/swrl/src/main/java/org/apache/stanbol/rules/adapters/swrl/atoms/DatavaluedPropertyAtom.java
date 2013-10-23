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
package org.apache.stanbol.rules.adapters.swrl.atoms;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.swrl.ArgumentSWRLAtom;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLRule;

/**
 * It adapts any IndividualPropertyAtom to a SWRL datavalued property atom.
 * 
 * @author anuzzolese
 * 
 */

public class DatavaluedPropertyAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnsupportedTypeForExportException,
                                         UnavailableRuleObjectException {

        org.apache.stanbol.rules.manager.atoms.DatavaluedPropertyAtom tmp = (org.apache.stanbol.rules.manager.atoms.DatavaluedPropertyAtom) ruleAtom;

        IObjectAtom argument1 = tmp.getArgument1();

        IObjectAtom datatypeProperty = tmp.getDatatypeProperty();

        RuleAtom argument2 = tmp.getArgument2();

        SWRLAtom arg1Atom = (SWRLAtom) adapter.adaptTo(argument1, SWRLRule.class);
        SWRLAtom predicateAtom = (SWRLAtom) adapter.adaptTo(datatypeProperty, SWRLRule.class);
        SWRLAtom arg2Atom = (SWRLAtom) adapter.adaptTo(argument2, SWRLRule.class);

        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        OWLDataProperty owlDataProperty;
        SWRLIArgument swrliArgument;
        SWRLDArgument swrldArgument;

        if (predicateAtom instanceof ArgumentSWRLAtom) {
            owlDataProperty = factory.getOWLDataProperty(IRI.create(((ArgumentSWRLAtom) predicateAtom)
                    .getId()));
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (arg1Atom instanceof ArgumentSWRLAtom) {
            swrliArgument = (SWRLIArgument) ((ArgumentSWRLAtom) arg1Atom).getSwrlArgument();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }
        if (arg2Atom instanceof ArgumentSWRLAtom) {
            swrldArgument = (SWRLDArgument) ((ArgumentSWRLAtom) arg2Atom).getSwrlArgument();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        return (T) factory.getSWRLDataPropertyAtom(owlDataProperty, swrliArgument, swrldArgument);

    }
}
