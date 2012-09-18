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
package org.apache.stanbol.enhancer.rdfentities.fise;

import org.apache.stanbol.enhancer.rdfentities.Rdf;


@Rdf(id="http://fise.iks-project.eu/ontology/TextAnnotation")
public interface TextAnnotation extends Enhancement {

    @Rdf(id="http://fise.iks-project.eu/ontology/start")
    Integer getStart();
    @Rdf(id="http://fise.iks-project.eu/ontology/start")
    void setStart(Integer start);

    @Rdf(id="http://fise.iks-project.eu/ontology/end")
    Integer getEnd();
    @Rdf(id="http://fise.iks-project.eu/ontology/end")
    void setEnd(Integer end);

    @Rdf(id="http://fise.iks-project.eu/ontology/selected-text")
    String getSelectedText();
    @Rdf(id="http://fise.iks-project.eu/ontology/selected-text")
    void setSelectedText(String selectedText);

    @Rdf(id="http://fise.iks-project.eu/ontology/selection-context")
    String getSelectionContext();
    @Rdf(id="http://fise.iks-project.eu/ontology/selection-context")
    void setSelectionContext(String selectionContext);
}
