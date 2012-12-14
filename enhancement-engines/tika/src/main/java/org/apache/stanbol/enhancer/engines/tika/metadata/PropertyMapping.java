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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.tika.metadata.Metadata;

public final class PropertyMapping extends Mapping {
    
    /**
     * A Set with the mapped properties
     */
    protected final Set<String> tikaProperties;

    public PropertyMapping(String ontProperty, UriRef ontType,String...tikaProperties) {
        this(ontProperty == null? null : new UriRef(ontProperty), ontType,tikaProperties);
    }
    public PropertyMapping(String ontProperty, UriRef ontType,Converter converter,String...tikaProperties) {
        this(ontProperty == null? null : new UriRef(ontProperty), ontType,converter,tikaProperties);
    }

    public PropertyMapping(String ontProperty,String...tikaProperties) {
        this(ontProperty == null? null : new UriRef(ontProperty),null,tikaProperties);
    }

    public PropertyMapping(UriRef ontProperty,String...tikaProperties) {
        this(ontProperty,null,tikaProperties);
    }
    public PropertyMapping(UriRef ontProperty, UriRef ontType,String...tikaProperties) {
        this(ontProperty,ontType,null,tikaProperties);
    }
    public PropertyMapping(UriRef ontProperty, UriRef ontType,Converter converter,String...tikaProperties) {
        super(ontProperty, ontType,converter);
        if(tikaProperties == null || tikaProperties.length < 1){
            throw new IllegalArgumentException("The list of parsed Tika properties MUST NOT be NULL nor empty!");
        }
        this.tikaProperties = Collections.unmodifiableSet(new HashSet<String>(
                Arrays.asList(tikaProperties)));
        if(this.tikaProperties.contains(null) || this.tikaProperties.contains("")){
            throw new IllegalArgumentException("Teh parsed list of Tika properties MUST NOT " +
            		"contain NULL or empty members (parsed: "+Arrays.toString(tikaProperties)+")!");
        }
    }

    @Override
    public boolean apply(MGraph graph, NonLiteral subject, Metadata metadata) {
        Set<Resource> values = new HashSet<Resource>();
        for(String tikaProperty : tikaProperties){
            String[] tikaPropValues = metadata.getValues(tikaProperty);
            if(tikaPropValues != null && tikaPropValues.length > 0){
                for(String tikaPropValue : tikaPropValues){
                    Resource resource = toResource(tikaPropValue, true);
                    if(resource != null){
                        values.add(resource);
                        mappingLogger.log(subject, ontProperty, tikaProperty, resource);
                    }
                }
            }
        }
        values.remove(null);
        values.remove("");
        if(values.isEmpty()){
            return false;
        } else {
            for(Resource resource : values){
                graph.add(new TripleImpl(subject, ontProperty, resource));
            }
            return true;
        }
    }

    @Override
    public Set<String> getMappedTikaProperties() {
        return tikaProperties;
    }
    
}
