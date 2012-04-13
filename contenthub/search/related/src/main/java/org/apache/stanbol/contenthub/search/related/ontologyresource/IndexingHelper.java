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

import java.util.List;

import org.apache.stanbol.contenthub.servicesapi.Constants;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This class is created to create LARQ index of external ontology provided for the search operation
 */
public class IndexingHelper {

    /**
     * Represents the special property which is used by Lucene while creating the index. At the beginning of a
     * search operation, user ontology is processed to add this special property to each class and individual
     * resource by using their local names.
     */
    public static final Property HAS_LOCAL_NAME = property("hasLocalName");

    private static Property property(String local) {
        return ResourceFactory.createProperty(Constants.SEARCH_URI, local);
    }

    public static void addIndexPropertyToOntResources(OntModel model) {
        // Add class names
        for (OntClass klass : model.listClasses().toList()) {
            if (klass == null || klass.isAnon()) continue;
            klass.addProperty(HAS_LOCAL_NAME, klass.getLocalName());
        }
        // Add individual names
        for (OntResource ind : model.listIndividuals().toList()) {
            if (ind == null || ind.isAnon()) continue;
            ind.addProperty(HAS_LOCAL_NAME, ind.getLocalName());
        }

        // Add CMS objects
        Resource cmsObject = ResourceFactory.createResource(Constants.CMS_OBJECT
                .getUnicodeString());
        List<Statement> cmsOBjects = model.listStatements(null, RDF.type, cmsObject).toList();
        for (Statement stmt : cmsOBjects) {
            Resource subject = stmt.getSubject();
            /*
             * As index is created based on SearchVocabulary.HAS_LOCAL_NAME property, it is necessary to add
             * name of CMS Objects in that property.
             */
            String name = getCMSObjectName(subject);
            if (!name.equals("")) {
                Statement s = ResourceFactory.createStatement(subject, HAS_LOCAL_NAME,
                    ResourceFactory.createPlainLiteral(name));
                model.add(s);
            }
        }
    }

    public static String getCMSObjectName(Resource subject) {
        String name = "";
        Property cmsNameProp = ResourceFactory.createProperty(Constants.CMS_OBJECT_NAME
                .getUnicodeString());
        if (subject.hasProperty(cmsNameProp)) {
            name = subject.getProperty(cmsNameProp).getString();
        }
        return name;
    }
}
