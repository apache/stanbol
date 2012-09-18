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

/**
 * 
 * @author anuzzolese
 * @author alberto.musetti
 * 
 */

public interface RefactorEnhancementEngineConf {
    /**
     * The component name used for the Configuration Service
     */
    String NAME = "org.apache.stanbol.enhancer.engines.refactor.RefactorEnhancementEngine";

    /**
     * The OntoNet scope that the engine should use.
     */
    String SCOPE = "org.apache.stanbol.enhancer.engines.refactor.scope";

    /**
     * The location from which the recipe is loaded.
     */
    String RECIPE_LOCATION = "org.apache.stanbol.enhancer.engines.refactor.recipe.location";

    /**
     * The ID used for identifying the recipe in the RuleStore.
     */
    String RECIPE_ID = "org.apache.stanbol.enhancer.engines.refactor.recipe.id";

    /**
     * The set of ontology URIs that should be loaded in the core space of the scope.
     */
    String SCOPE_CORE_ONTOLOGY = "org.apache.stanbol.enhancer.engines.refactor.scope.core.ontology";

    /**
     * If true: the previously generated RDF is deleted and substituted with the new one. If false: the new
     * one is appended to the old RDF. Possible value in the configuration: true or false.
     */
    String APPEND_OTHER_ENHANCEMENT_GRAPHS = "org.apache.stanbol.enhancer.engines.refactor.append.graphs";

    /**
     * If true: entities are fetched via the EntityHub. If false: entities are fetched on-line. Possible value
     * in the configuration: true or false.
     */
    String USE_ENTITY_HUB = "org.apache.stanbol.enhancer.engines.refactor.entityhub";

    public String getScope();

    public void setScope(String scopeId);

    public String getRecipeLocation();

    public void setRecipeLocation(String recipeLocation);

    public String getRecipeId();

    public void setRecipeId(String recipeId);

    public String[] getScopeCoreOntologies();

    public void setScopeCoreOntologies(String[] coreOntologyIRI);

    public boolean isInGraphAppendMode();

    public void setInGraphAppendMode(boolean b);

    public boolean isEntityHubUsed();

    public void setEntityHubUsed(boolean b);
}
