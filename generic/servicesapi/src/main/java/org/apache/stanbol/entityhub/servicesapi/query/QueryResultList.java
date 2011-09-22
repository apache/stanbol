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
package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


public interface QueryResultList<T> extends Iterable<T> {

    /**
     * Getter for the query of this result set.
     * TODO: Change return Value to {@link Query}
     * @return the query used to create this result set
     */
    FieldQuery getQuery();
    /**
     * The selected fields of this query
     * @return
     */
    Set<String> getSelectedFields();

    /**
     * Iterator over the results of this query
     */
    Iterator<T> iterator();
    /**
     * Unmodifiable collection of the results
     * @return the resutls
     */
    Collection<? extends T> results();
    /**
     * <code>true</code> if the result set is empty
     * @return <code>true</code> if the result set is empty. Otherwise <code>false</code>
     */
    boolean isEmpty();

    /**
     * The size of this result set
     * @return
     */
    int size();
    /**
     * The type of the results in the list
     * @return the type
     */
    Class<T> getType();
}
