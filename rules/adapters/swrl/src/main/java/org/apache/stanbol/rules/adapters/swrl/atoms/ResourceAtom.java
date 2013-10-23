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

import java.net.URI;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.swrl.ArgumentSWRLAtom;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.SWRLIArgument;

/**
 * It adapts any ResourceAtom to a {@link SWRLIArgument} object in SWRL
 * 
 * @author anuzzolese
 * 
 */
public class ResourceAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.ResourceAtom tmp = (org.apache.stanbol.rules.manager.atoms.ResourceAtom) ruleAtom;
        URI uri = tmp.getURI();

        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        OWLIndividual owlIndividual = factory.getOWLNamedIndividual(IRI.create(uri.toString()));
        SWRLIArgument swrliArgument = factory.getSWRLIndividualArgument(owlIndividual);
        return (T) new ArgumentSWRLAtom(swrliArgument, uri.toString());
    }

}
