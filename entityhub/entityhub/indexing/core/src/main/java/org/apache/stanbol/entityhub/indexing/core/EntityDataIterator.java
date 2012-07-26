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
package org.apache.stanbol.entityhub.indexing.core;

import java.util.Iterator;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Interface used to iterate over all entities.
 * By calling {@link #next()} one can iterate over the IDs of the Entities.
 * The data ({@link Representation}) of the current entity are available by
 * calling {@link #getRepresentation()}.<p>
 * This interface is intended for data source that prefer to read entity
 * information as a stream (e.g. from an tab separated text file) and therefore
 * can not provide an implementation of the {@link EntityDataProvider} interface.
 * @see EntityDataProvider 
 * @author Rupert Westenthaler
 *
 */
public interface EntityDataIterator extends Iterator<String>{

    /**
     * Getter for the Representation of the current active Element. This is the
     * Representation of the Entity with the ID returned for the previous
     * call to {@link #next()}. This method does not change the current element
     * of the iteration.
     * @return the Representation for the entity with the ID returned by 
     * {@link #next()}
     */
    Representation getRepresentation();
    /**
     * Close the iteration.
     */
    void close();
}
