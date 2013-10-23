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
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLObjectVisitor;
import org.semanticweb.owlapi.model.SWRLObjectVisitorEx;
import org.semanticweb.owlapi.model.SWRLPredicate;

/**
 * Wrapper that allows to pass {@link SWRLArgument} objects as {@link SWRLAtom} objects
 * 
 * @author anuzzolese
 * 
 */
public class ArgumentSWRLAtom implements SWRLAtom {

    private SWRLDArgument swrldArgument;
    private SWRLIArgument swrliArgument;

    private String id;

    public ArgumentSWRLAtom(SWRLDArgument swrldArgument, String id) {
        this.swrldArgument = swrldArgument;
        this.id = id;

    }

    public ArgumentSWRLAtom(SWRLIArgument swrliArgument, String id) {
        this.swrliArgument = swrliArgument;
        this.id = id;
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
    public int compareTo(OWLObject o) {
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

    public SWRLArgument getSwrlArgument() {
        if (swrliArgument != null) {
            return swrliArgument;
        } else {
            return swrldArgument;
        }

    }

    public String getId() {
        return id;
    }

    @Override
    public Set<OWLAnonymousIndividual> getAnonymousIndividuals() {
        // TODO Auto-generated method stub
        return null;
    }
}
