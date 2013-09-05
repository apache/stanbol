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
package org.apache.stanbol.ontologymanager.core.session;

import java.util.Date;
import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.session.SessionIDGenerator;
import org.apache.stanbol.ontologymanager.servicesapi.util.StringUtils;
import org.semanticweb.owlapi.model.IRI;

/**
 * FIXME make sure to manage namespaces independently!
 * 
 * @author alexdma
 * 
 */
public class TimestampedSessionIDGenerator implements SessionIDGenerator {

    private IRI baseIRI;

    public TimestampedSessionIDGenerator() {
        this.baseIRI = null;
    }

    public TimestampedSessionIDGenerator(IRI baseIRI) {
        this.baseIRI = baseIRI;
    }

    @Override
    public String createSessionID() {
        String id = "";
        if (baseIRI != null) id += StringUtils.stripIRITerminator(baseIRI) + "/session/";
        return id + new Date().getTime();
    }

    @Override
    public String createSessionID(Set<String> exclude) {
        String id = null;
        do {
            id = createSessionID();
        } while (exclude.contains(id));
        return id;
    }

    @Override
    public IRI getBaseIRI() {
        return baseIRI;
    }

    @Override
    public void setBaseIRI(IRI baseIRI) {
        this.baseIRI = baseIRI;
    }

}
