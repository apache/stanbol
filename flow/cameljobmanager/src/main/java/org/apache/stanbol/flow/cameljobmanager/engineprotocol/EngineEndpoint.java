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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;


public class EngineEndpoint extends DefaultEndpoint {
	
	private EnhancementEngine engine;
    	
	public EngineEndpoint(String uri, EngineComponent component, EnhancementEngine e) {
        super(uri, component);
        this.engine = e;
    }

	public EnhancementEngine getEngine(){
		return engine;
	}
    public Producer createProducer() throws Exception {
        return new EngineProducer(this);
    }
    
    public Consumer createConsumer(Processor processor) throws Exception {
    	throw new UnsupportedOperationException("You cannot get messages from this endpoint: " + getEndpointUri());
    }

    public boolean isSingleton() {
        return true;
    }
}
