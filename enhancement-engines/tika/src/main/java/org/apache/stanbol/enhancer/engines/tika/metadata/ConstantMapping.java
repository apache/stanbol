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
package org.apache.stanbol.enhancer.engines.tika.metadata;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.tika.metadata.Metadata;

public class ConstantMapping extends Mapping{

    
    private Collection<RDFTerm> values;

    public ConstantMapping(IRI ontProperty, RDFTerm...values) {
        super(ontProperty, null);
        if(values == null || values.length < 1){
            throw new IllegalArgumentException("The parsed values MUST NOT be NULL nor an empty array");
        }
        this.values = Arrays.asList(values);
        if(this.values.contains(null)){
            throw new IllegalArgumentException("The parsed values MUST NOT contain a NULL element " +
            		"(parsed: "+this.values+")!");
        }
    }

    @Override
    public boolean apply(Graph graph, BlankNodeOrIRI subject, Metadata metadata) {
        for(RDFTerm value : values){
            graph.add(new TripleImpl(subject, ontProperty, value));
            mappingLogger.log(subject, ontProperty, null, value);
        }
        return true;
    }

    @Override
    public Set<String> getMappedTikaProperties() {
        return Collections.emptySet();
    }
}
