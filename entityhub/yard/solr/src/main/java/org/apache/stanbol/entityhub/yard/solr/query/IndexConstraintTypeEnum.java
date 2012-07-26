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
package org.apache.stanbol.entityhub.yard.solr.query;

import org.apache.stanbol.entityhub.yard.solr.model.IndexDataType;

/**
 * Constraint Types defined for IndexFields
 * <p>
 * This could be replaced by a more flexible way to register supported constraint types in future versions
 * 
 * @author Rupert Westenthaler
 */
public enum IndexConstraintTypeEnum {
    /**
     * constraints the DataType of values
     * 
     * @see IndexDataType
     */
    DATATYPE,
    /**
     * Constraints the language of values
     */
    LANG,
    /**
     * Constraints the field
     */
    FIELD,
    /**
     * Constraints the Value
     */
    EQ,
    /**
     * REGEX based filter on values
     */
    REGEX,
    /**
     * Wildcard based filter on values
     */
    WILDCARD,
    /**
     * Greater than constraint
     */
    GT,
    /**
     * Lower than constraint
     */
    LT,
    /**
     * Greater else constraint
     */
    GE,
    /**
     * Lower else constraint
     */
    LE,
}
