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

package org.apache.stanbol.contenthub.search.related.ontologyresource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.contenthub.search.related.RelatedKeywordImpl;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
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
    private static final Logger log = LoggerFactory.getLogger(ClosureHelper.class);

    private OntModel model;

    private ClosureHelper(OntModel userOntology) {
        this.model = userOntology;
    }

    public static ClosureHelper getInstance(OntModel userOntology) {
        return new ClosureHelper(userOntology);
    }

    public List<RelatedKeyword> computeClassClosure(String classURI,
                                                    int maxDepth,
                                                    double initialScore,
                                                    double degradingCoefficient,
                                                    String keyword,
                                                    List<RelatedKeyword> relatedKeywords) {
        long t1 = System.currentTimeMillis();
        List<String> relatedClassURIs = new ArrayList<String>();
        computeSuperClassClosure(classURI, maxDepth, initialScore, degradingCoefficient, keyword,
            relatedKeywords);
        computeSubClassClosure(classURI, maxDepth, initialScore, degradingCoefficient, keyword,
            relatedKeywords);
        for (String relatedClassURI : relatedClassURIs) {
            computeInstanceClosure(relatedClassURI, initialScore, degradingCoefficient, keyword,
                relatedKeywords);
        }
        log.debug("Computed class closure of {} in {} miliseconds", classURI, System.currentTimeMillis() - t1);
        return relatedKeywords;
    }

    public final void computeIndividualClosure(String individualURI,
                                               int maxDepth,
                                               double initialScore,
                                               double degradingCoefficient,
                                               String keyword,
                                               List<RelatedKeyword> relatedKeywords) {

        long t1 = System.currentTimeMillis();
        Individual ind = model.getIndividual(individualURI);
        if (ind != null && ind.isURIResource()) {
            for (OntClass ontClass : ind.listOntClasses(true).toSet()) {
                if (ontClass != null && ontClass.isURIResource()) {
                    computeClassClosure(ontClass.getURI(), 6, initialScore, 1.5, keyword, relatedKeywords);
                }
            }
        }
        log.debug("Computed individual closure of {} in {} miliseconds", individualURI,
            System.currentTimeMillis() - t1);
    }

    private void computeSuperClassClosure(String classURI,
                                          int maxDepth,
                                          double initialScore,
                                          double degradingCoefficient,
                                          String keyword,
                                          List<RelatedKeyword> relatedKeywords) {
        OntClass ontClass = model.getOntClass(classURI);
        if (ontClass == null || ontClass.isAnon() || isClassNotValid(ontClass)) {
            log.warn("Can not find class with uri {}, skipping ...", classURI);
        } else if (maxDepth == 0) {
            log.debug("Max depth reached not examining the resource {}", classURI);
            return;
        } else {
            String rkw = ontClass.getLocalName();
            relatedKeywords.add(new RelatedKeywordImpl(rkw, initialScore, "Ontology"));
            log.debug("Added {} as a related keyword to {} by super class relation", rkw, keyword);
            log.debug("Computing super class closure of {} ", classURI);
            List<OntClass> superClasses = ontClass.listSuperClasses(true).toList();
            for (OntClass superClass : superClasses) {
                computeSuperClassClosure(superClass.getURI(), maxDepth - 1, initialScore
                                                                            / degradingCoefficient,
                    degradingCoefficient, keyword, relatedKeywords);
            }
        }
    }

    private void computeSubClassClosure(String classURI,
                                        int maxDepth,
                                        double initialScore,
                                        double degradingCoefficient,
                                        String keyword,
                                        List<RelatedKeyword> relatedKeywords) {
        OntClass ontClass = model.getOntClass(classURI);
        if (ontClass == null || ontClass.isAnon() || isClassNotValid(ontClass)) {
            log.warn("Can not find class with uri {}, skipping ...", classURI);
        } else if (maxDepth == 0) {
            log.debug("Max depth reached not examining the resource {}", classURI);
            return;
        } else {
            String rkw = ontClass.getLocalName();
            relatedKeywords.add(new RelatedKeywordImpl(rkw, initialScore, "Ontology"));
            log.debug("Added {} as related keyword to {} by sub class relation", classURI, keyword);
            log.debug("Computing sub class closure of {} ", classURI);
            List<OntClass> subClasses = ontClass.listSubClasses(true).toList();
            for (OntClass subClass : subClasses) {
                computeSubClassClosure(subClass.getURI(), maxDepth - 1, initialScore / degradingCoefficient,
                    degradingCoefficient, keyword, relatedKeywords);
            }
        }
    }

    private void computeInstanceClosure(String classURI,
                                        double initialScore,
                                        double degradingCoefficient,
                                        String keyword,
                                        List<RelatedKeyword> relatedKeywords) {
        OntClass ontClass = model.getOntClass(classURI);
        if (ontClass == null || ontClass.isAnon() || isClassNotValid(ontClass)) {
            log.warn("Can not find class with uri {}, skipping ...", classURI);
        } else {
            log.debug("Computing instance closure of class {} ", classURI);
            List<? extends OntResource> instances = ontClass.listInstances(true).toList();
            for (OntResource instance : instances) {
                if (instance == null || instance.isAnon() || !instance.isIndividual()) {
                    continue;
                } else {
                    Individual individual = instance.asIndividual();
                    String rkw = individual.getLocalName();
                    relatedKeywords.add(new RelatedKeywordImpl(rkw, initialScore / degradingCoefficient,
                            "Ontology"));
                    log.debug("Added {} as a relate keyword to {} ", rkw, keyword);
                }
            }
        }
    }

    private boolean isClassNotValid(OntClass klass) {
        String uri = klass.getURI();
        return uri.contains(RDF.getURI()) || uri.contains(RDFS.getURI()) || uri.contains(OWL.getURI())
               || uri.contains(OWL2.getURI());
    }

    public void computeClosureWithProperty(Resource sourceURI,
                                           Property subsumptionProperty,
                                           int maxDepth,
                                           double initialScore,
                                           double degradingCoefficient,
                                           String keyword,
                                           List<RelatedKeyword> relatedKeywords) {

        computeSubclosureWithProperty(sourceURI, subsumptionProperty, maxDepth, initialScore,
            degradingCoefficient, keyword, relatedKeywords);
        computeSuperclosureWithProperty(sourceURI, subsumptionProperty, maxDepth, initialScore,
            degradingCoefficient, keyword, relatedKeywords);

    }

    private void computeSubclosureWithProperty(Resource uri,
                                               Property subsumptionProperty,
                                               int depth,
                                               double initialScore,
                                               double degradingCoefficient,
                                               String keyword,
                                               List<RelatedKeyword> relatedKeywords) {

        if (depth == 0) {
            log.debug("Max depth reached not examining the resource {}", uri.getURI());
            return;
        } else {
            log.debug("Computing sub concepts of {} ", uri);
            Set<Statement> children = model.listStatements(null, subsumptionProperty, uri).toSet();
            double score = initialScore / degradingCoefficient;
            for (Statement childStatement : children) {
                Resource subject = childStatement.getSubject();
                String childName = IndexingHelper.getCMSObjectName(subject);
                if (!childName.equals("")) {
                    childName = cropFileExtensionFromKeyword(childName);
                    relatedKeywords.add(new RelatedKeywordImpl(childName, score, "Ontology"));
                    log.debug("Added {} as a related keyword", childName);
                    computeSubclosureWithProperty(subject, subsumptionProperty, depth - 1, score,
                        degradingCoefficient, keyword, relatedKeywords);
                }
            }
        }
    }

    private void computeSuperclosureWithProperty(Resource uri,
                                                 Property subsumptionProperty,
                                                 int depth,
                                                 double initialScore,
                                                 double degradingCoefficient,
                                                 String keyword,
                                                 List<RelatedKeyword> relatedKeywords) {
        if (depth == 0) {
            log.debug("Max depth reached not examining the resource {}", uri.getURI());
            return;
        } else {
            log.debug("Computing parent concepts of {} ", uri);
            Set<Statement> parents = model.listStatements(uri, subsumptionProperty, (RDFNode) null).toSet();
            double score = initialScore / degradingCoefficient;
            for (Statement parentStatement : parents) {
                Resource object = parentStatement.getResource();
                String parentName = IndexingHelper.getCMSObjectName(object);
                if (!parentName.equals("")) {
                    parentName = cropFileExtensionFromKeyword(parentName);
                    relatedKeywords.add(new RelatedKeywordImpl(parentName, score, "Ontology"));
                    log.debug("Added {} as related keyword to ", parentName, keyword);
                    computeSuperclosureWithProperty(object, subsumptionProperty, depth - 1, score,
                        degradingCoefficient, keyword, relatedKeywords);
                }
            }
        }
    }

    /**
     * As name of the resources in the CMS Adapter generated ontology would most likely be file names, file
     * extension are cropped using the Apache Commons IO library.
     */
    public static String cropFileExtensionFromKeyword(String keyword) {
        return FilenameUtils.removeExtension(keyword);
    }
}
