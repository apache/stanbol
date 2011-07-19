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
package org.apache.stanbol.cmsadapter.cmis.processor;

import java.util.Arrays;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.RelationshipType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.cmis.repository.CMISObjectId;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntProperty;

/**
 * TODO: This processer is not tested yet.
 * 
 * This processor creates OWL object property assertions from CMIS {@link Relationship}s. Subject is the
 * individual generated for the source object of the relationship. Property is the property generated for the
 * type of the relationship. Object is the individual generated for the target object of the relationship.
 * 
 * @author cihan
 * 
 */

@Service
@Component(immediate = true)
public class RelationshipProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(RelationshipProcessor.class);

    @Override
    public Boolean canProcess(Object cmsObject, Object session) {
        // here we assume Document and Folder types of CMIS spec is converted to CMSObjects
        return cmsObject instanceof CMSObject && session instanceof Session;
    }

    @Override
    public void createObjects(List<Object> objects, MappingEngine engine) {
        if (objects != null) {
            for (Object object : objects) {
                if (canProcess(object, engine.getSession())) {
                    try {
                        CMSObject cmsObject = (CMSObject) object;
                        CmisObject node = ((Session) engine.getSession()).getObject(CMISObjectId
                                .getObjectId(cmsObject.getUniqueRef()));
                        List<Relationship> relations = node.getRelationships();
                        // FIXME find a better way to reuse cmislifters' func.
                        CMISNodeTypeLifter lifter = new CMISNodeTypeLifter(engine);

                        for (Relationship relation : relations) {
                            processRelation(cmsObject, relation, engine, lifter);
                        }
                    } catch (ClassCastException e) {
                        log.info("Class cast exception at processing Object: {} ", object);
                        log.info("Exception is ", e);
                    } catch (CmisBaseException e) {
                        log.info("Repository exception at processing Object: {}", object);
                        log.info("Exception is ", e);
                    }
                } else {
                    log.info("Incompatible type given as argument: {}. Skipping ...", object.getClass()
                            .getName());
                }
            }
        }
    }

    private void processRelation(CMSObject node,
                                 Relationship relation,
                                 MappingEngine engine,
                                 CMISNodeTypeLifter lifter) {
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();
        RelationshipType type = (RelationshipType) relation.getType();
        lifter.createObjectPropertyDefForRelationshipTypes(Arrays.asList(new RelationshipType[] {type}));
        OntProperty prop = orh.getDatatypePropertyByReference(type.getId());
        Individual ind = orh.getIndividualByReference(node.getUniqueRef());
        Individual target = orh.getIndividualByReference(relation.getTarget().getId());
        ind.addProperty(prop, target);
        log.debug("Added triple by relationship processor, {}, {}, {}",
            new Object[] {ind.getURI(), prop.getURI(), target.getURI()});

    }

    @Override
    public void deleteObjects(List<Object> objects, MappingEngine engine) {
        log.debug("Other processors should have already deleted my triples because I only add property assertions to an individual");
    }

}
