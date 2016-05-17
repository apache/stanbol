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
package org.apache.stanbol.entityhub.yard.clerezza.impl;

import java.util.Dictionary;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.osgi.service.cm.ConfigurationException;

public class ClerezzaYardConfig extends YardConfig {

    
    
    
    public ClerezzaYardConfig(String id) throws IllegalArgumentException {
        super(id);
    }
    public ClerezzaYardConfig(Dictionary<String,Object> config) throws ConfigurationException, IllegalArgumentException {
        super(config);
    }

    /**
     * Getter for the {@link ClerezzaYard#GRAPH_URI} property
     * @return the graph URI or <code>null</code> if non is configured
     */
    public IRI getGraphUri(){
        Object value = config.get(ClerezzaYard.GRAPH_URI);
        if(value instanceof IRI){
            return (IRI)value;
        } else if (value != null){
            return new IRI(value.toString());
        } else {
            return null;
        }
    }
    /**
     * Setter for the {@link ClerezzaYard#GRAPH_URI} property
     * @param uri the uri or <code>null</code> to remove this configuration
     */
    public void setGraphUri(IRI uri){
        if(uri == null){
            config.remove(ClerezzaYard.GRAPH_URI);
        } else {
            config.put(ClerezzaYard.GRAPH_URI, uri.getUnicodeString());
        }
    }
    
    @Override
    protected void validateConfig() throws ConfigurationException {
        //nothing to validate
    }

}
