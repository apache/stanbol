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
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.util.AtomList;

/**
 * 
 * A concrete implementation of a {@link Rule}.
 * 
 * @author anuzzolese
 * 
 */
public class RuleImpl implements Rule {

    private IRI ruleID;

    private String ruleName;
    private String rule;

    private AtomList head;
    private AtomList body;

    protected Recipe recipe;
    protected String description;

    public RuleImpl(IRI ruleID, String ruleName, AtomList body, AtomList head) {
        this.ruleID = ruleID;
        this.ruleName = ruleName;
        this.head = head;
        this.body = body;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRule() {

        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    @Override
    public String prettyPrint() {
        String rule = null;
        String tab = "       ";
        if (head != null && body != null) {
            boolean addAnd = false;
            rule = "RULE " + ruleName + " ASSERTS THAT " + System.getProperty("line.separator");
            rule += "IF" + System.getProperty("line.separator");
            for (RuleAtom atom : body) {
                rule += tab;
                if (addAnd) {
                    rule += "AND ";
                } else {
                    addAnd = true;
                }
                rule += atom.toString() + System.getProperty("line.separator");

            }

            rule += "IMPLIES" + System.getProperty("line.separator");

            addAnd = false;
            for (RuleAtom atom : head) {
                rule += tab;
                if (addAnd) {
                    rule += "AND ";
                } else {
                    addAnd = true;
                }
                rule += atom.toString() + System.getProperty("line.separator");

            }
        }
        return rule;
    }

    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(ruleName);
        stringBuilder.append("[");

        boolean firstLoop = true;

        // add the rule body
        for (RuleAtom atom : body) {
            if (!firstLoop) {
                stringBuilder.append(" . ");
            } else {
                firstLoop = false;
            }
            stringBuilder.append(atom.toString());
        }

        // add the rule head
        if (head != null) {

            stringBuilder.append(" -> ");

            firstLoop = true;
            for (RuleAtom atom : head) {
                if (!firstLoop) {
                    stringBuilder.append(" . ");
                } else {
                    firstLoop = false;
                }
                stringBuilder.append(atom.toString());
            }
        }

        stringBuilder.append("]");

        return stringBuilder.toString();
    }

    @Override
    public AtomList getBody() {
        return body;
    }

    @Override
    public AtomList getHead() {
        return head;
    }

    @Override
    public IRI getRuleID() {
        return ruleID;
    }

    protected void bindToRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public Recipe getRecipe() {
        return recipe;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

}
