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
package org.apache.stanbol.enhancer.servicesapi;

import java.util.Collection;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.helper.Rdf;


@Rdf(id="http://fise.iks-project.eu/ontology/EntityAnnotation")
public interface EntityAnnotation extends Enhancement {

    @Rdf(id="http://fise.iks-project.eu/ontology/entity-reference")
    UriRef getEntityReference();
    @Rdf(id="http://fise.iks-project.eu/ontology/entity-reference")
    void setEntityReference(UriRef reference);

    @Rdf(id="http://fise.iks-project.eu/ontology/entity-label")
    String getEntityLabel();
    @Rdf(id="http://fise.iks-project.eu/ontology/entity-label")
    void setEntityLabel(String label);

    @Rdf(id="http://fise.iks-project.eu/ontology/entity-type")
    Collection<UriRef> getEntityTypes();
}
