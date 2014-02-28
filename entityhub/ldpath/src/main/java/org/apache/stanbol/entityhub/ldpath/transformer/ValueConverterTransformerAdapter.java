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
package org.apache.stanbol.entityhub.ldpath.transformer;

import java.util.Map;

import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.transformers.NodeTransformer;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory.ValueConverter;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;

/**
 * LDPath {@link NodeTransformer} internally using the Entityhub 
 * {@link ValueConverter}. <p>
 * This transformer should be used for plain literals and references (xsd:anyURI)
 * to ensure that nodes are transformed to {@link Text} and {@link Reference}
 * instances.<p>
 * Users should use {@link LDPathUtils#createAndInitLDPath(RDFBackend, ValueFactory)}
 * to ensure that {@link LDPath} instances are configured accordingly.
 *  
 * @author Rupert Westenthaler.
 * @see LDPathUtils#createAndInitLDPath(RDFBackend, ValueFactory)
 * @param <T>
 */
public class ValueConverterTransformerAdapter<T> implements NodeTransformer<T,Object> {

    private final ValueFactory vf;
    private final ValueConverter<T> vc;
    
    public ValueConverterTransformerAdapter(ValueConverter<T> vc, ValueFactory vf){
        this.vf = vf == null ? InMemoryValueFactory.getInstance() : vf;
        this.vc = vc;
    }
    @Override
    public T transform(RDFBackend<Object> backend, Object node, Map<String, String> configuration) throws IllegalArgumentException {
        T value = vc.convert(node, vf);
        if(value == null){
            value = vc.convert(backend.stringValue(node), vf);
        }
        if(value == null){
            throw new IllegalArgumentException("Unable to transform node '"+
                node+"' to data type '"+vc.getDataType()+"'!");
        } else {
            return value;
        }
    }

}
