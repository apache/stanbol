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
package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

import com.hp.hpl.jena.rdf.model.RDFList;

/**
 * Represents how different type of OWL entities are named in an extraction context.
 * 
 * @author Suat
 * 
 */
public interface NamingStrategy {

    String getClassName(String ontologyURI, CMSObject cmsObject);

    String getClassName(String ontologyURI, ObjectTypeDefinition objectTypeDefinition);

    String getClassName(String ontologyURI, String reference);

    String getIndividualName(String ontologyURI, CMSObject cmsObject);

    String getIndividualName(String ontologyURI, String reference);

    String getPropertyName(String ontologyURI, String reference);

    String getObjectPropertyName(String ontologyURI, String reference);

    String getObjectPropertyName(String ontologyURI, PropertyDefinition propertyDefinition);

    String getDataPropertyName(String ontologyURI, String reference);

    String getDataPropertyName(String ontologyURI, PropertyDefinition propertyDefinition);

    String getUnionClassURI(String ontologyURI, RDFList list);

}
