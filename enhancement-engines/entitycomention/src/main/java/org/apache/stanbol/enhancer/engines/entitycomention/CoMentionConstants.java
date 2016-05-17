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
package org.apache.stanbol.enhancer.engines.entitycomention;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;

public interface CoMentionConstants {

    /**
     * The {@link EntityLinkerConfig#NAME_FIELD} uri internally used by the
     * {@link EntityCoMentionEngine}.
     */
    IRI CO_MENTION_LABEL_FIELD = new IRI("urn:org.stanbol:enhander.engine.entitycomention:co-mention-label");
    
    /**
     * The {@link EntityLinkerConfig#TYPE_FIELD} uri internally used by the
     * {@link EntityCoMentionEngine}.
     */
    IRI CO_MENTION_TYPE_FIELD = new IRI("urn:org.stanbol:enhander.engine.entitycomention:co-mention-type");
}
