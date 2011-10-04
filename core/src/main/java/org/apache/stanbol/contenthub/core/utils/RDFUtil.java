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

package org.apache.stanbol.contenthub.core.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * @author cihan
 * 
 */
public final class RDFUtil {

    private static final String BASE_URI = "http://stanbol.org.apache/ontology";
    private static final RDFUtil INSTANCE = new RDFUtil();

    private RDFUtil() {

    }

    public static RDFUtil getInstance() {
        return RDFUtil.INSTANCE;
    }

    public OntModel getOntModel(String content) {
        InputStream is = new ByteArrayInputStream(content.getBytes());
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        model.read(is, BASE_URI);
        return model;
    }

}
