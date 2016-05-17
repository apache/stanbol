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

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.util.AtomList;

/**
 * Instances of this class are {@link Rule} objects bound to recipes.<br/>
 * When a rule is bound to a recipe, that it is an instance of this class.
 * 
 * @author anuzzolese
 * 
 */
public class RecipeRule extends RuleImpl {

    public RecipeRule(Recipe recipe, IRI ruleID, String ruleName, AtomList body, AtomList head) {
        super(ruleID, ruleName, body, head);

        bindToRecipe(recipe);
    }

    public RecipeRule(Recipe recipe, Rule rule) {
        this(recipe, rule.getRuleID(), rule.getRuleName(), rule.getBody(), rule.getHead());
    }

}
