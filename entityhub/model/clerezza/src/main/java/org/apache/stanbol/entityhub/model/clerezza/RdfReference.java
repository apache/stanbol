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
package org.apache.stanbol.entityhub.model.clerezza;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;

public class RdfReference implements Reference,Cloneable {
    private final IRI uri;
    protected RdfReference(String reference){
        if(reference == null){
            throw new IllegalArgumentException("The parsed Reference MUST NOT be NULL!");
        } else if(reference.isEmpty()){
            throw new IllegalArgumentException("The parsed Reference MUST NOT be Empty!");
        } else {
            this.uri = new IRI(reference);
        }
    }
    protected RdfReference(IRI uri){
        if(uri == null){
            throw new IllegalArgumentException("The parsed Reference MUST NOT be NULL!");
        } else if(uri.getUnicodeString().isEmpty()){
            throw new IllegalArgumentException("The parsed Reference MUST NOT be represent an empty string!");
        } else {
            this.uri = uri;
        }
    }
    @Override
    public String getReference() {
        return uri.getUnicodeString();
    }
    public IRI getIRI(){
        return uri;
    }
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new RdfReference(new IRI(uri.getUnicodeString()));
    }
    @Override
    public int hashCode() {
        return uri.getUnicodeString().hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Reference && uri.getUnicodeString().equals(((Reference)obj).getReference());
    }
    @Override
    public String toString() {
        return uri.getUnicodeString();
    }

}
