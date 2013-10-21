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
package org.apache.stanbol.entityhub.model.sesame;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * A {@link Reference} implementation backed by a Sesame {@link URI}
 * @author Rupert Westenthaler
 *
 */
public class RdfReference implements Reference, RdfWrapper {

    private final URI uri;


    protected RdfReference(URI uri){
        this.uri = uri;
    }
    
    @Override
    public String getReference() {
        return uri.stringValue();
    }
    /**
     * The wrapped Sesame {@link URI}
     * @return the URI
     */
    public URI getURI() {
        return uri;
    }
    @Override
    public Value getValue() {
        return uri;
    }
    
    @Override
    public int hashCode() {
        return uri.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Reference && 
                getReference().equals(((Reference)obj).getReference());
    }
    
    @Override
    public String toString() {
        return uri.toString();
    }
}