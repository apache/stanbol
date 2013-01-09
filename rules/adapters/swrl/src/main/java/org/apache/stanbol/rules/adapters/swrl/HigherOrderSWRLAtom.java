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

package org.apache.stanbol.rules.adapters.swrl;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectVisitor;
import org.semanticweb.owlapi.model.OWLObjectVisitorEx;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLObjectVisitor;
import org.semanticweb.owlapi.model.SWRLObjectVisitorEx;
import org.semanticweb.owlapi.model.SWRLPredicate;

/**
 * It is used for represent higher order atoms.<br/>
 * It is used to convert Stanbol atoms that accept other atoms as arguments, such as
 * <code>sum(sum(5,?x), ?y)</code>.<br/>
 * In such a situation in SWRL we should use new variables as place holders, e.g., sum(?ph1, ?y, ?z) sum(5,
 * ?x, ?ph1).
 * 
 * @author anuzzolese
 * 
 */
public class HigherOrderSWRLAtom implements SWRLAtom {

    private SWRLDArgument bindableArgument;
    private List<SWRLAtom> atoms;

    public HigherOrderSWRLAtom(SWRLDArgument bindableArgument, List<SWRLAtom> atoms) {
        this.bindableArgument = bindableArgument;
        this.atoms = atoms;
    }

    public List<SWRLAtom> getAtoms() {
        return atoms;
    }

    public SWRLDArgument getBindableArgument() {
        return bindableArgument;
    }

    @Override
    public void accept(SWRLObjectVisitor arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public <O> O accept(SWRLObjectVisitorEx<O> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void accept(OWLObjectVisitor arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public <O> O accept(OWLObjectVisitorEx<O> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OWLClass> getClassesInSignature() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OWLDataProperty> getDataPropertiesInSignature() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OWLDatatype> getDatatypesInSignature() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OWLNamedIndividual> getIndividualsInSignature() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OWLClassExpression> getNestedClassExpressions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertiesInSignature() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OWLEntity> getSignature() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isBottomEntity() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isTopEntity() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int compareTo(OWLObject arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Collection<SWRLArgument> getAllArguments() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SWRLPredicate getPredicate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OWLAnonymousIndividual> getAnonymousIndividuals() {
        // TODO Auto-generated method stub
        return null;
    }
}
