/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.flow.cameljobmanager.engineprotocol;

import org.apache.camel.Exchange;
import org.apache.camel.TypeConverter;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;

public class EngineProducer extends DefaultProducer {
    private EngineEndpoint endpoint;

    public EngineProducer(EngineEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(Exchange exchange) throws Exception {
    	ContentItem ci = exchange.getIn().getBody(ContentItem.class);
    	
    	EnhancementEngine stanbolEngine = endpoint.getEngine();
    	
    	if (stanbolEngine.canEnhance(ci) != EnhancementEngine.CANNOT_ENHANCE){
    		stanbolEngine.computeEnhancements(ci);
    		exchange.getIn().setBody(ci);
    	}
    	
    }

}
