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
package org.apache.stanbol.entityhub.core.yard;

import java.util.Dictionary;

import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.osgi.service.cm.ConfigurationException;


/**
 * Default implementation for Yard. It uses only the fields defined by the
 * {@link Yard} interface and only enforces a valid ID.<p>
 * This implementation can be used for the configuration of Yards that do not
 * need any further configuration.
 * @author Rupert Westenthaler
 *
 */
public final class SimpleYardConfig extends YardConfig {

    /**
     * Creates a new configuration with the minimal set of required properties
     * @param id the ID of the Yard
     * @throws IllegalArgumentException if the parsed valued do not fulfil the
     * requirements.
     */
    public SimpleYardConfig(String id) throws IllegalArgumentException {
        super(id);
        try {
            isValid();
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage(),e);
        }
    }
    /**
     * Initialise the Yard configuration based on a parsed configuration. Usually
     * used on the context of an OSGI environment in the activate method.
     * @param config the configuration usually parsed within an OSGI activate
     * method
     * @throws ConfigurationException if the configuration is incomplete of
     * some values are not valid
     * @throws IllegalArgumentException if <code>null</code> is parsed as
     * configuration
     */
    public SimpleYardConfig(Dictionary<String, Object> config) throws ConfigurationException, IllegalArgumentException {
        super(config);
    }

    @Override
    protected void validateConfig() throws ConfigurationException {
    }

}
