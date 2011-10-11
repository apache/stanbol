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
package org.apache.stanbol.reengineer.base.api;

import org.semanticweb.owlapi.model.IRI;

public class Reengineer_OWL {

    public static final String URI = "http://ontologydesignpatterns.org/ont/iks/semion.owl";

    public static final String NS = "http://ontologydesignpatterns.org/ont/iks/semion.owl#";

    public static final IRI DataSource = IRI.create(NS + "DataSource");

    public static final IRI hasDataSourceType = IRI.create(NS + "hasDataSourceType");

    public static final IRI hasDataSourceURI = IRI.create(NS + "hasDataSourceURI");
}
