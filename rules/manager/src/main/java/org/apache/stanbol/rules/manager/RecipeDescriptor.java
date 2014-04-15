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
package org.apache.stanbol.rules.manager;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @deprecated
 * 
 * @author anuzzolese
 * 
 */
public final class RecipeDescriptor {

    /**
     * Restrict instantiation
     */
    private RecipeDescriptor() {}

    /**
     * <p>
     * The RDF model that holds the vocabulary terms
     * </p>
     */
    private static Model m_model = ModelFactory.createDefaultModel();

    /**
     * <p>
     * The namespace of the vocabulary as a string
     * </p>
     */
    public static final String NS = "http://stlab.istc.cnr.it/software/semion/ontologies/recipeDescriptor.owl#";

    /**
     * <p>
     * The namespace of the vocabulary as a string
     * </p>
     * 
     * @see #NS
     */
    public static String getURI() {
        return NS;
    }

    /**
     * <p>
     * The namespace of the vocabulary as a resource
     * </p>
     */
    public static final Resource NAMESPACE = m_model.createResource(NS);

    public static final Resource Recipe = m_model.createResource(NS + "Recipe");

    public static final Resource Model = m_model.createResource(NS + "Model");

    public static final Resource RuleSet = m_model.createResource(NS + "RuleSet");

    public static final Property hasRuleSet = m_model.createProperty(NS + "hasRuleSet");

    public static final Property isRuleSetOf = m_model.createProperty(NS + "isRuleSetOf");

    public static final Property hasInputModel = m_model.createProperty(NS + "hasInputModel");

    public static final Property isInputModelOf = m_model.createProperty(NS + "isInputModelOf");

    public static final Property hasTargetModel = m_model.createProperty(NS + "hasTargetModel");

    public static final Property isTargetModelOf = m_model.createProperty(NS + "isTargetModelOf");

    public static final Property hasURI = m_model.createProperty(NS + "hasURI");

}
