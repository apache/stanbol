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
package org.apache.stanbol.enhancer.nlp.json.valuetype;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.codehaus.jackson.node.ObjectNode;
/**
 * Interface allowing to extend how Classes used as generic type for
 * {@link org.apache.stanbol.enhancer.nlp.model.annotation.Value}s are parsed from JSON
 * <p>
 * Implementation MUST register itself as OSGI services AND also provide
 * a <code>META-INF/services/org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser</code> 
 * file required for the {@link java.util.ServiceLoader} utility.

 * @param <T>
 */
public interface ValueTypeParser<T> {


    String PROPERTY_TYPE = "type";

    Class<T> getType();
    
    T parse(ObjectNode jAnnotation, AnalysedText at);
    
}
