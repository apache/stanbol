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
package org.apache.stanbol.rules.base.api;

import java.io.InputStream;
import java.util.Set;

import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public interface RuleStore {

    /**
     * The key used to configure default namespace of Stanbol rules.
     */
    String RULE_NAMESPACE = "org.apache.stanbol.rules.base.rule_namespace";

    /**
     * The key used to configure the path of the default rule ontology.
     */
    String RULE_ONTOLOGY = "org.apache.stanbol.rules.base.rule_ontology";

    boolean addRecipe(IRI recipeIRI, String recipeDescription);

    Recipe addRuleToRecipe(Recipe recipe, String rRuleInKReSSyntax);
    
    Recipe addRuleToRecipe(Recipe recipe, InputStream ruleInKReSSyntax);

    Recipe addRuleToRecipe(String recipeID, String ruleInKReSSyntax) throws NoSuchRecipeException;
    
    Recipe addRuleToRecipe(String recipeID, InputStream ruleInKReSSyntax) throws NoSuchRecipeException;

    void createRecipe(String recipeID, String rulesInKReSSyntax);

    String getFilePath();

    OWLOntology getOntology();

    Recipe getRecipe(IRI recipe) throws NoSuchRecipeException;

    String getRuleStoreNamespace();

    Set<IRI> listIRIRecipes();

    RecipeList listRecipes();

    boolean removeRecipe(IRI recipeIRI);

    boolean removeRecipe(Recipe recipe);

    boolean removeRule(Rule rule);

    void saveOntology() throws OWLOntologyStorageException;

    void setStore(OWLOntology owl);

}
