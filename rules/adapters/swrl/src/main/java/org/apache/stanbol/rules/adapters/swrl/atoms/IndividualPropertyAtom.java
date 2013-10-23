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
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLRule;

/**
 * It adapts any IndividualPropertyAtom to a SWRL object property atom.
 * 
 * @author anuzzolese
 * 
 */
public class IndividualPropertyAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnsupportedTypeForExportException,
                                         UnavailableRuleObjectException {

        org.apache.stanbol.rules.manager.atoms.IndividualPropertyAtom tmp = (org.apache.stanbol.rules.manager.atoms.IndividualPropertyAtom) ruleAtom;

        IObjectAtom argument1 = tmp.getArgument1();
        IObjectAtom argument2 = tmp.getArgument2();
        IObjectAtom objectProperty = tmp.getObjectProperty();

        SWRLAtom predicateAtom = (SWRLAtom) adapter.adaptTo(objectProperty, SWRLRule.class);
        SWRLAtom subjectAtom = (SWRLAtom) adapter.adaptTo(argument1, SWRLRule.class);
        SWRLAtom objectAtom = (SWRLAtom) adapter.adaptTo(argument2, SWRLRule.class);

        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        OWLObjectProperty owlObjectProperty;
        SWRLIArgument swrliArgument1;
        SWRLIArgument swrliArgument2;

        if (predicateAtom instanceof ArgumentSWRLAtom) {
            owlObjectProperty = factory.getOWLObjectProperty(IRI.create(((ArgumentSWRLAtom) predicateAtom)
                    .getId()));
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (subjectAtom instanceof ArgumentSWRLAtom) {
            swrliArgument1 = (SWRLIArgument) ((ArgumentSWRLAtom) subjectAtom).getSwrlArgument();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }
        if (objectAtom instanceof ArgumentSWRLAtom) {
            swrliArgument2 = (SWRLIArgument) ((ArgumentSWRLAtom) objectAtom).getSwrlArgument();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        return (T) factory.getSWRLObjectPropertyAtom(owlObjectProperty, swrliArgument1, swrliArgument2);

    }

}
