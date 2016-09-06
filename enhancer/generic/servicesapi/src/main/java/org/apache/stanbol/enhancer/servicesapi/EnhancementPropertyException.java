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

package org.apache.stanbol.enhancer.servicesapi;

/**
 * Exection thorwn if the an Enhancement Property parsed to an engine is
 * missing or has an invalid value
 * 
 * @since 0.12.1
 *
 */
public class EnhancementPropertyException extends EngineException {
    
    private static final long serialVersionUID = 1L;
    
    private String property;

    public EnhancementPropertyException(EnhancementEngine ee, ContentItem ci, String property, String reason, Throwable t){
        super(ee,ci, new StringBuilder("Enhancement Property '")
        .append(property).append("' - ").append(reason).toString(),t);
        this.property = property;
        
    }
    
    public String getProperty() {
        return property;
    }

    
}
