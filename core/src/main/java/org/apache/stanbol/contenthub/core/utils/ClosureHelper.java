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

package org.apache.stanbol.contenthub.core.utils;

import java.util.List;

import org.apache.stanbol.contenthub.servicesapi.search.execution.ClassResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.IndividualResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * 
 * @author cihan
 * 
 */
public final class ClosureHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClosureHelper.class);

    // private SearchContext context;
    private OntModel model;
    private SearchContextFactory factory;

    private ClosureHelper(SearchContext context) {
        // this.context = context;
        this.model = context.getSearchModel();
        this.factory = context.getFactory();
    }

    public static ClosureHelper getInstance(SearchContext context) {
        return new ClosureHelper(context);
    }

    public final void computeClassClosure(ClassResource klazz,
                                          int maxDepth,
                                          double degradingCoefficient,
                                          Keyword keyword) {
        long t1 = System.currentTimeMillis();
        computeSuperClassClosure(klazz, maxDepth, degradingCoefficient, keyword);
        computeSubClassClosure(klazz, maxDepth, degradingCoefficient, keyword);
        for (ClassResource res : keyword.getRelatedClassResources()) {
            computeInstanceClosure(res, degradingCoefficient, keyword);
        }
        LOGGER.debug("Computed class closure of {} in {} miliseconds", klazz.getClassURI(),
            System.currentTimeMillis() - t1);
    }

    public final void computeIndividualClosure(IndividualResource individual,
                                               int maxDepth,
                                               double degradingCoefficient,
                                               Keyword keyword) {
        long t1 = System.currentTimeMillis();
        Individual ind = model.getIndividual(individual.getIndividualURI());
        if (ind != null && ind.isURIResource()) {

            for (OntClass klazz : ind.listOntClasses(true).toSet()) {
                if (klazz != null && klazz.isURIResource()) {
                    ClassResource cr = factory.createClassResource(klazz.getURI(), 1.0,
                        individual.getScore(), keyword);
                    cr.addRelatedIndividual(individual);
                    computeClassClosure(cr, 6, 1.5, keyword);
                }
            }

        }
        LOGGER.debug("Computed individual closure of {} in {} miliseconds", individual.getIndividualURI(),
            System.currentTimeMillis() - t1);
    }

    private void computeSuperClassClosure(ClassResource klazz,
                                          int maxDepth,
                                          double degradingCoefficient,
                                          Keyword keyword) {
        OntClass ontClass = model.getOntClass(klazz.getClassURI());
        if (ontClass == null || ontClass.isAnon() || isClassNotValid(ontClass)) {
            LOGGER.warn("Can not find class with uri {}, skipping ...", klazz.getClassURI());
        } else if (maxDepth == 0) {
            LOGGER.debug("Max depth reached not examining the resource {}", klazz.getClassURI());
            return;
        } else {
            LOGGER.debug("Computing super class closure of {} ", klazz.getClassURI());
            double rank = klazz.getScore();
            List<OntClass> superClasses = ontClass.listSuperClasses(true).toList();
            for (OntClass superClass : superClasses) {
                if (superClass == null || superClass.isAnon() || isClassNotValid(superClass)) {
                    continue;
                }
                ClassResource newRes = factory.createClassResource(superClass.getURI(), 1.0,
                    rank / degradingCoefficient, keyword);
                // originalClass.addRelatedClass(newRes);
                LOGGER.debug("Added {} as class closure to keyword {} by super class relation",
                    newRes.getClassURI(), keyword.getKeyword());
                computeSuperClassClosure(newRes, maxDepth - 1, degradingCoefficient, keyword);
            }
        }
    }

    private void computeSubClassClosure(ClassResource klazz,
                                        int maxDepth,
                                        double degradingCoefficient,
                                        Keyword keyword) {
        OntClass ontClass = model.getOntClass(klazz.getClassURI());
        if (ontClass == null || ontClass.isAnon() || isClassNotValid(ontClass)) {
            LOGGER.warn("Can not find class with uri {}, skipping ...", klazz.getClassURI());
        } else if (maxDepth == 0) {
            LOGGER.debug("Max depth reached not examining the resource {}", klazz.getClassURI());
            return;
        } else {
            LOGGER.debug("Computing sub class closure of {} ", klazz.getClassURI());
            double rank = klazz.getScore();
            List<OntClass> subClasses = ontClass.listSubClasses(true).toList();
            for (OntClass subClass : subClasses) {
                if (subClass == null || subClass.isAnon() || isClassNotValid(subClass)) {
                    continue;
                }
                ClassResource newRes = factory.createClassResource(subClass.getURI(), 1.,
                    rank / degradingCoefficient, keyword);
                // klazz.addRelatedClass(newRes);
                LOGGER.debug("Added {} as  class closure to {} by sub class relation", newRes.getClassURI(),
                    keyword.getKeyword());
                computeSubClassClosure(newRes, maxDepth - 1, degradingCoefficient, keyword);
            }
        }
    }

    private void computeInstanceClosure(ClassResource klazz, double degradingCoefficient, Keyword keyword) {
        OntClass ontClass = model.getOntClass(klazz.getClassURI());
        if (ontClass == null || ontClass.isAnon() || isClassNotValid(ontClass)) {
            LOGGER.warn("Can not find class with uri {}, skipping ...", klazz.getClassURI());
        } else {
            LOGGER.debug("Computing instance closure of class {} ", klazz.getClassURI());
            double rank = klazz.getScore();
            List<? extends OntResource> instances = ontClass.listInstances(true).toList();
            for (OntResource instance : instances) {
                if (instance == null || instance.isAnon() || !instance.isIndividual()) {
                    continue;
                } else {
                    Individual individual = instance.asIndividual();
                    IndividualResource newRes = factory.createIndividualResource(individual.getURI(), 1.,
                        rank / degradingCoefficient, keyword);
                    // klazz.addRelatedIndividual(newRes);
                    LOGGER.debug("Added {} as a individual closure to keyword {} ",
                        newRes.getIndividualURI(), keyword.getKeyword());
                }
            }
        }
    }

    private boolean isClassNotValid(OntClass klass) {
        String uri = klass.getURI();
        return uri.contains(RDF.getURI()) || uri.contains(RDFS.getURI()) || uri.contains(OWL.getURI())
               || uri.contains(OWL2.getURI());
    }
}
