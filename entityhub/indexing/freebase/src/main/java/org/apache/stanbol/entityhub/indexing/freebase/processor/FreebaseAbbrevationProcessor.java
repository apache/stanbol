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
package org.apache.stanbol.entityhub.indexing.freebase.processor;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;

public class FreebaseAbbrevationProcessor implements EntityProcessor {

    public static final String FB_NS = "http://rdf.freebase.com/ns/";
    private static final String FB_ALIAS = FB_NS + "common.topic.alias";
    private static final String FB_NAME = FB_NS + "type.object.name";
    private static final String RDFS_LABEL = NamespaceEnum.rdfs+"label";

    @Override
    public void setConfiguration(Map<String,Object> config) {
    }

    @Override
    public boolean needsInitialisation() {
        return true;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void close() {
    }

    @Override
    public Representation process(Representation rep) {
        Iterator<Text> aliases = rep.getText(FB_ALIAS);
        while(aliases.hasNext()){
            Text alias = aliases.next();
            if(StringUtils.isAllUpperCase(alias.getText())){
                rep.add(FB_NAME, alias);
                rep.add(RDFS_LABEL,alias);
            }
        }
        return rep;
    }

}
