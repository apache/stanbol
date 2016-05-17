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

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.rules.base.api.util.AtomList;

/**
 * A Rule is a Java object that represent a rule in Stanbol.
 * 
 * @author anuzzolese
 * 
 */
public interface Rule extends Adaptable {

    /**
     * Gets the ID of the rule.
     * 
     * @return the {@link IRI} representing the name of the rule.
     */
    IRI getRuleID();

    /**
     * Gets the name of the rule.
     * 
     * @return the {@link String} representing the name of the rule.
     */
    String getRuleName();

    /**
     * It allows to return the textual description of the rule.
     * 
     * @return the {@link String} containing the description of the rule.
     */
    String getDescription();

    /**
     * It sets the description of the rule.
     * 
     * @param description
     *            {@link String}
     */
    void setDescription(String description);

    /**
     * Sets the rule's name
     * 
     * @param ruleName
     *            {@link String}
     */
    void setRuleName(String ruleName);

    /**
     * Sets the rule expressed in Rule syntax
     * 
     * @param rule
     *            {@link String}
     */
    void setRule(String rule);

    /**
     * Rules are composed by an antecedent (body) and a consequent (head). This method returnn the consequent
     * expressed as a list of its atoms ({@link AtomList}).
     * 
     * @return the {@link AtomList} of the consequent's atoms.
     */
    AtomList getHead();

    /**
     * Rules are composed by an antecedent (body) and a consequent (head). This method returnn the antecedent
     * expressed as a list of its atoms ({@link AtomList}).
     * 
     * @return the {@link AtomList} of the antecedent's atoms.
     */
    AtomList getBody();

    /**
     * A rule is always a member of recipe. This method returns the recipe in which the rule exists.
     * 
     * @return the recipe {@link Recipe}
     */
    Recipe getRecipe();
}
