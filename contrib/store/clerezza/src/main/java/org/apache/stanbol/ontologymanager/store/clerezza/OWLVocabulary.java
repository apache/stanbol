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
package org.apache.stanbol.ontologymanager.store.clerezza;

import org.apache.clerezza.rdf.core.UriRef;

public class OWLVocabulary {

    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String OWL = "http://www.w3.org/2002/07/owl#";

    public static final UriRef RDF_TYPE = new UriRef(RDF + "type");
    public static final UriRef OWL_CLASS = new UriRef(OWL + "Class");
    public static final UriRef OWL_DATATYPE_PROPERTY = new UriRef(OWL + "DatatypeProperty");
    public static final UriRef OWL_OBJECT_PROPERTY = new UriRef(OWL + "ObjectProperty");
    public static final UriRef OWL_INDIVIDUAL = new UriRef(OWL + "Individual");
}
