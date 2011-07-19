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
package org.apache.stanbol.cmsadapter.core.decorated;

import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.LOCAL_NAME;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.NAMESPACE;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.PATH;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.UNIQUE_REF;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;

// Unnecessarily, eagerly builds objects but OK for tests.
public class CMSObjectBuilder {

    private static ObjectFactory of = new ObjectFactory();
    private CMSObject instance = of.createCMSObject();
    private String prefix;

    public CMSObjectBuilder(String prefix) {
        this.prefix = prefix;
        instance.setUniqueRef(prefix + UNIQUE_REF);
        instance.setLocalname(prefix + LOCAL_NAME);
        instance.setPath(prefix + PATH);
    }

    public CMSObjectBuilder(String prefix, String id, String name, String path) {
        this.prefix = prefix;
        instance.setUniqueRef(prefix + id);
        instance.setLocalname(prefix + name);
        instance.setPath(prefix + path);
    }

    public CMSObjectBuilder namespace() {
        instance.setNamespace(prefix + NAMESPACE);
        return this;
    }

    public CMSObjectBuilder namespace(String namespace) {
        instance.setNamespace(prefix + namespace);
        return this;
    }

    public CMSObjectBuilder child(CMSObject child) {
        instance.getChildren().add(child);
        return this;
    }

    public CMSObject build() {
        return instance;
    }

}