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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.tika.metadata.Metadata;

public final class ResourceMapping extends Mapping{

    private static final Mapping[] EMPTY = new Mapping[]{};
    
    Collection<Mapping> required;
    Collection<Mapping> optional;
    Collection<Mapping> additional;
    Set<String> mappedTikaProperties;
    
    public ResourceMapping(String ontProperty, Mapping...required) {
        this(new IRI(ontProperty), required);
    }
    public ResourceMapping(String ontProperty, Mapping[] required, Mapping[] optional,Mapping[] additional) {
        this(new IRI(ontProperty), required,optional,additional);
    }

    public ResourceMapping(IRI ontProperty, Mapping...requried) {
        this(ontProperty,requried,null,null);
    }
    public ResourceMapping(IRI ontProperty, Mapping[] required, Mapping[] optional,Mapping[] additional) {
        super(ontProperty,null);
        required = required == null ? EMPTY : required;
        optional = optional == null ? EMPTY : optional;
        additional = additional == null ? EMPTY : additional;
        if(required.length < 1 && optional.length <1){
            throw new IllegalArgumentException("Neighter optional nor required subMappings where parsed!");
        }
        Set<String> mapped = new HashSet<String>();
        this.required = Arrays.asList(required);
        if(this.required.contains(null)){
            throw new IllegalArgumentException("Tha parsed Array of required sub mappings MUST NOT contain a NULL element" +
            		"(parsed: "+this.required+")");
        }
        for(Mapping m : this.required){
            mapped.addAll(m.getMappedTikaProperties());
        }
        this.optional = Arrays.asList(optional);
        if(this.optional.contains(null)){
            throw new IllegalArgumentException("Tha parsed Array of optional sub mappings MUST NOT contain a NULL element" +
                    "(parsed: "+this.optional+")");
        }
        for(Mapping m : this.optional){
            mapped.addAll(m.getMappedTikaProperties());
        }
        mapped.remove(null);
        this.mappedTikaProperties = Collections.unmodifiableSet(mapped);
        //additional mappings
        if(additional != null){
            this.additional = Arrays.asList(additional);
        } else {
            this.additional = Collections.emptySet();
        }
        if(this.additional.contains(null)){
            throw new IllegalArgumentException("Tha parsed Array of additional sub mappings MUST NOT contain a NULL element" +
                    "(parsed: "+this.additional+")");
        }
        //NOTE: additional mappings are not added to the mappedTikaProperties
    }

    @Override
    public boolean apply(Graph graph, BlankNodeOrIRI subject, Metadata metadata) {
        boolean added = false;
        BlankNodeOrIRI s = new BlankNode();
        mappingLogger.log(subject, ontProperty, null, s);
        if(!required.isEmpty()) {
            Graph g = new SimpleGraph();
            for(Mapping m : required){
                if(!m.apply(g, s, metadata)){
                    return false;
                }
            }
            graph.addAll(g);
            added = true;
        }
        for(Mapping m : optional){
            if(m.apply(graph, s, metadata)){
                added = true;
            }
        }
        if(added){
            for(Mapping m : additional){
                m.apply(graph, s, metadata);
            }
            graph.add(new TripleImpl(subject,ontProperty,s));
        }
        return added;
    }
    @Override
    public Set<String> getMappedTikaProperties() {
        return mappedTikaProperties;
    }
}
