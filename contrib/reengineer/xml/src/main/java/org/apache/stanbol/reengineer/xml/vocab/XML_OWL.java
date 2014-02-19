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
package org.apache.stanbol.reengineer.xml.vocab;

import org.semanticweb.owlapi.model.IRI;

public class XML_OWL {

    /**
     * <p>
     * The namespace of the vocabulary as a string
     * </p>
     */
    public static final String NS = "http://www.ontologydesignpatterns.org/ont/iks/oxml.owl#";

    /**
     * <p>
     * The namespace of the vocabulary as a string
     * </p>
     */
    public static final String URI = "http://www.ontologydesignpatterns.org/ont/iks/oxml.owl";

    /**
     * <p>
     * The namespace of the vocabulary as a string
     * </p>
     * 
     * @see #NS
     */

    public static final IRI SpecialAttrs = IRI.create(NS + "SpecialAttrs");

    public static final IRI XMLElement = IRI.create(NS + "XMLElement");

    public static final IRI XMLAttribute = IRI.create(NS + "XMLAttribute");

    public static final IRI Node = IRI.create(NS + "Node");

    public static final IRI hasSpecialAttrs = IRI.create(NS + "hasSpecialAttrs");

    public static final IRI isSpecialAttrsOf = IRI.create(NS + "isSpecialAttrsOf");

    public static final IRI hasXMLAttribute = IRI.create(NS + "hasXMLAttribute");

    public static final IRI isXMLAttributeOf = IRI.create(NS + "isXMLAttributeOf");

    public static final IRI nodeName = IRI.create(NS + "nodeName");

    public static final IRI nodeValue = IRI.create(NS + "nodeValue");

    public static final IRI hasElementDeclaration = IRI.create(NS + "hasElementDeclaration");

    public static final IRI isElementDeclarationOf = IRI.create(NS + "isElementDeclarationOf");

    public static final IRI hasAttributeDeclaration = IRI.create(NS + "hasAttributeDeclaration");

    public static final IRI isAttributetDeclarationOf = IRI.create(NS + "isAttributeDeclarationOf");

    public static final IRI textContent = IRI.create(NS + "textContent");
}
