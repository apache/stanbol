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
package org.apache.stanbol.ontologymanager.sources.owlapi;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.hp.hpl.jena.sparql.vocabulary.FOAF;

/**
 * OWL entities used for tests.
 * 
 * @author alexdma
 * 
 */
public class Entities {

    private static OWLDataFactory df = OWLManager.getOWLDataFactory();

    private static String _NS = "http://stanbol.apache.org/ontologies/generic/entities#";

    public static OWLNamedIndividual ALEX = df.getOWLNamedIndividual(IRI.create(_NS + "Alex"));

    public static OWLNamedIndividual BEGONA = df.getOWLNamedIndividual(IRI.create(_NS + "Begona"));

    public static OWLClass FOAF_PERSON = df.getOWLClass(IRI.create(FOAF.NS + "Person"));

    public static OWLObjectProperty FOAF_KNOWS = df.getOWLObjectProperty(IRI.create(FOAF.NS + "knows"));

    public static OWLAnnotationProperty FOAF_KNOWS_AP = df.getOWLAnnotationProperty(IRI.create(FOAF.NS
                                                                                               + "knows"));
}
