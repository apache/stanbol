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

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.ResourceOrVariable;
import org.apache.clerezza.rdf.core.sparql.query.UriRefOrVariable;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleTriplePattern;
import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaSparqlObject;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;

/**
 * It adapts any BlankNodeAtom to a simple triple pattern in Clerezza.
 * 
 * @author anuzzolese
 * 
 */
public class BlankNodeAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.BlankNodeAtom tmp = (org.apache.stanbol.rules.manager.atoms.BlankNodeAtom) ruleAtom;

        UriRefOrVariable subject;
        UriRefOrVariable predicate;
        ResourceOrVariable object;

        IObjectAtom argument2UriResource = tmp.getArgument2();
        IObjectAtom argument1UriResource = tmp.getArgument1();

        ClerezzaSparqlObject subjectCSO = (ClerezzaSparqlObject) adapter.adaptTo(argument2UriResource,
            ConstructQuery.class);
        ClerezzaSparqlObject predicateCSO = (ClerezzaSparqlObject) adapter.adaptTo(argument1UriResource,
            ConstructQuery.class);

        subject = new UriRefOrVariable((IRI) subjectCSO.getClerezzaObject());
        predicate = new UriRefOrVariable((IRI) predicateCSO.getClerezzaObject());
        object = new ResourceOrVariable(new BlankNode());

        return (T) new ClerezzaSparqlObject(new SimpleTriplePattern(subject, predicate, object));

    }

}
