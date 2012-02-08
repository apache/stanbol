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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * Default configuration of the Refactor Enhancement Engine.<br/>
 * The default configuration load an instance of Refactor Engine which maps named-entities recognized by
 * other Enhancement Engines to Schema.org.
 *  
 * @author anuzzolese
 * @author alberto.musetti
 *
 */
public class DefaultRefactorEnhancementEngineConf implements RefactorEnhancementEngineConf {

	private Dictionary<String, Object> conf;
	
	/*
	public DefaultRefactorEnhancementEngineConf() {
		
	}
	*/
	public DefaultRefactorEnhancementEngineConf(Dictionary<String, Object> map) {
		this.conf = map;
	}
	
	@Override
	public String getScope() {
		return (String) conf.get(SCOPE);
	}

	@Override
	public void setScope(String scopeId) {
		conf.put(SCOPE, scopeId);
	}

	@Override
	public String getRecipeLocation() {
		return (String) conf.get(RECIPE_LOCATION);
	}

	@Override
	public void setRecipeLocation(String recipeLocation) {
		conf.put(RECIPE_LOCATION, recipeLocation);
		
	}

	@Override
	public String getRecipeId() {
		return (String) conf.get(RECIPE_ID);
	}

	@Override
	public void setRecipeId(String recipeId) {
		conf.put(RECIPE_ID, recipeId);
	}

	@Override
	public String[] getScopeCoreOntology() {
		return (String[]) conf.get(SCOPE_CORE_ONTOLOGY);
	}

	@Override
	public void setScopeCoreOntology(String[] coreOntologyURI) {
		conf.put(SCOPE_CORE_ONTOLOGY, coreOntologyURI);
	}

	@Override
	public boolean isInGraphAppendMode() {
		return ((Boolean) conf.get(APPEND_OTHER_ENHANCEMENT_GRAPHS)).booleanValue();
	}

	@Override
	public void setInGraphAppendMode(boolean b) {
		conf.put(APPEND_OTHER_ENHANCEMENT_GRAPHS, b);
		
	}

	@Override
	public boolean isEntityHubUsed() {
		return ((Boolean) conf.get(USE_ENTITY_HUB)).booleanValue();
	}

	@Override
	public void setEntityHubUsed(boolean b) {
		conf.put(USE_ENTITY_HUB, b);
		
	}

}
