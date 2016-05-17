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

import java.util.ArrayList;
import java.util.List;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.rules.base.api.NoSuchRuleInRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.util.RuleList;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * The RecipeImpl is a concrete implementation of the Recipe interface. A Recipe is a collection identified by
 * an URI of rules. Each rules of the recipe is also identified by an URI. Rules are expressed both in SWRL
 * and in KReS rules syntax.
 * 
 * @author anuzzolese
 * 
 */
public class RecipeImpl implements Recipe {

    private IRI recipeID;
    private String recipeDescription;
    private RuleList ruleList = new RuleList();

    /**
     * Create a new {@code RecipeImpl} from a set of rule expressed in KReS rule syntax.
     * 
     * @param recipeID
     * @param recipeDescription
     * @param ruleList
     */
    public RecipeImpl(IRI recipeID, String recipeDescription, RuleList ruleList) {
        this.recipeID = recipeID;
        this.recipeDescription = recipeDescription;
        if ( ruleList != null ) {
          this.ruleList.addAll( ruleList );
        }
    }

    public RuleList getRuleList() {
        return ruleList;
    }

    public IRI getRecipeID() {
        return recipeID;
    }

    public String getRecipeDescription() {
        return recipeDescription;
    }

    public Model getRecipeAsRDFModel() {

        return null;
    }

    public Rule getRule(String ruleName) throws NoSuchRuleInRecipeException {
        for (Rule rule : ruleList) {
            if (rule.getRuleName().equals(ruleName)) {
                return rule;
            }
        }

        StringBuilder message = new StringBuilder();

        message.append("No rule named ");
        message.append(ruleName);
        message.append(" exists in recipe ");
        message.append(this.getRecipeID());

        throw new NoSuchRuleInRecipeException(message.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String separator = System.getProperty("line.separator");
        boolean firstLoop = true;
        for (Rule rule : ruleList) {
            if (!firstLoop) {
                sb.append(" . ");
                sb.append(separator);
            } else {
                firstLoop = false;
            }
            sb.append(rule.toString());
        }

        return sb.toString();
    }

    @Override
    public void addRule(Rule rule) {
        ruleList.add(rule);
    }

    @Override
    public String prettyPrint() {

        String separator = System.getProperty("line.separator");

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("The recipe contains the following rules:");
        stringBuilder.append(separator);

        for (Rule rule : ruleList) {
            stringBuilder.append(rule.prettyPrint());
            stringBuilder.append(separator);
        }

        return stringBuilder.toString();
    }

    @Override
    public void removeRule(Rule rule) {
        ruleList.remove(rule);
    }

    @Override
    public Rule getRule(IRI ruleID) throws NoSuchRuleInRecipeException {
        for (Rule rule : ruleList) {
            if (rule.getRuleID().toString().equals(ruleID.toString())) {
                return rule;
            }
        }

        StringBuilder message = new StringBuilder();

        message.append("No rule with ID ");
        message.append(ruleID.toString());
        message.append(" exists in recipe ");
        message.append(this.getRecipeID());

        throw new NoSuchRuleInRecipeException(message.toString());
    }

    @Override
    public List<IRI> listRuleIDs() {
        List<IRI> ruleIDs = new ArrayList<IRI>();

        for (Rule rule : ruleList) {
            ruleIDs.add(rule.getRuleID());
        }

        return ruleIDs;
    }

    @Override
    public List<String> listRuleNames() {
        List<String> ruleNames = new ArrayList<String>();

        for (Rule rule : ruleList) {
            ruleNames.add(rule.getRuleName());
        }

        return ruleNames;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Recipe) {
            Recipe recipe = (Recipe) obj;

            if (recipe.getRecipeID().toString().equals(this.getRecipeID().toString())) {
                if (recipe.getRecipeDescription().equals(this.getRecipeDescription())) {
                    if (recipe.toString().equals(this.toString())) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }
}
