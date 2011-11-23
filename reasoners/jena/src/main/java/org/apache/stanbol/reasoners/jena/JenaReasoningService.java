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
package org.apache.stanbol.reasoners.jena;

import java.util.List;

import org.apache.stanbol.reasoners.servicesapi.ReasoningService;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * Interface for a Jena based reasoning services
 */
public interface JenaReasoningService extends ReasoningService<Model,Rule,Statement> {

    /**
     * Runs the reasoner over the given input data
     * 
     * @param data
     * @return
     */
    public abstract InfModel run(Model data);

    /**
     * Run the reasoner over the given data and rules
     * 
     * @param data
     * @param rules
     * @return
     */
    public abstract InfModel run(Model data, List<Rule> rules);
}