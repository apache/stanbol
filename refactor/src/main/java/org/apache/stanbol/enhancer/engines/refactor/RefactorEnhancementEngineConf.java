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

package org.apache.stanbol.enhancer.engines.refactor;

import org.apache.felix.scr.annotations.Property;

/**
 *
 * @author anuzzolese
 * @author alberto.musetti
 * 
 */

public interface RefactorEnhancementEngineConf {

    /**
     * The OntoNet scope that the engine should use.
     */
    @Property(value="schema.org")
    String SCOPE = "engine.refactor.scope";
    
    /**
     * The location from which the recipe is loaded.
     */
    @Property(value="")
    String RECIPE_LOCATION = "engine.refactor.recipe";
    
    /**
     * The ID used for identifying the recipe in the RuleStore.
     */
    @Property(value="")
    String RECIPE_ID = "engine.refactor.recipe.id";
    
    /**
     * The set of ontology URIs that should be loaded in the core space of the scope.
     */
    @Property(value={"http://ontologydesignpatterns.org/ont/iks/kres/dbpedia_demo.owl", ""})
    String SCOPE_CORE_ONTOLOGY = "engine.refactor.scope.core.ontology";
    
    /**
     * If true: the previously generated RDF is deleted and substituted with the new one. 
     * If false: the new one is appended to the old RDF. 
     * Possible value in the configuration: true or false.
     */
    @Property(boolValue=true, description="If true: the previously generated RDF is deleted and substituted with the new one. If false: the new one is appended to the old RDF. Possible value: true or false.")
    String APPEND_OTHER_ENHANCEMENT_GRAPHS = "engine.refactor.append.graphs";
    
    /**
     * If true: entities are fetched via the EntityHub. 
     * If false: entities are fetched on-line. 
     * Possible value in the configuration: true or false.
     */
    @Property(boolValue=true, description="If true: entities are fetched via the EntityHub. If false: entities are fetched on-line. Possible value: true or false.")
    String USE_ENTITY_HUB  = "engine.refactor.entityhub";
    
    
    public String getScope();
    
    public void setScope(String scopeId);
    
    public String getRecipeLocation();
    
    public void setRecipeLocation(String recipeLocation);
    
    public String getRecipeId();
    
    public void setRecipeId(String recipeId);
    
    public String[] getScopeCoreOntology();
    
    public void setScopeCoreOntology(String[] coreOntologyIRI);
    
    public boolean isInGraphAppendMode();
    
    public void setInGraphAppendMode(boolean b);
    
    public boolean isEntityHubUsed();
    
    public void setEntityHubUsed(boolean b);
}
