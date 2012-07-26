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
package org.apache.stanbol.entityhub.indexing.core.processor;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Returns the parsed Representation. Intended to be used in cases where a
 * <code>null</code> value is not allowed for the {@link EntityProcessor}.
 * @author Rupert Westenthaler
 *
 */
public class EmptyProcessor implements EntityProcessor{

    @Override
    public Representation process(Representation source) {
        return source;
    }

    @Override
    public void close() {
        //nothing to do
    }

    @Override
    public void initialise() {
        //nothing to do
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        //no configuration supported
    }

}
