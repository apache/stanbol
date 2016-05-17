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

/**
 * It provides static methods in order to represent properties, classes and the namespace used in order to
 * represent rules in the store
 * 
 * @author anuzzolese
 * 
 */
public final class Symbols {

    /**
     * Restrict instantiation
     */
    private Symbols() {}

    public static String NS = "http://incubator.apache.org/stanbol/rules/";

    public static String variablesPrefix = "http://incubator.apache.org/stanbol/rules/variables/";

    public static IRI description = new IRI("http://incubator.apache.org/stanbol/rules/description");

    public static IRI hasRule = new IRI("http://incubator.apache.org/stanbol/rules/hasRule");

    public static IRI ruleName = new IRI("http://incubator.apache.org/stanbol/rules/ruleName");

    public static IRI ruleBody = new IRI("http://incubator.apache.org/stanbol/rules/ruleBody");

    public static IRI ruleHead = new IRI("http://incubator.apache.org/stanbol/rules/ruleHead");

    public static IRI Recipe = new IRI("http://incubator.apache.org/stanbol/rules/Recipe");

    public static IRI Rule = new IRI("http://incubator.apache.org/stanbol/rules/Rule");

}
