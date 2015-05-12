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
package org.apache.stanbol.commons.namespaceprefix.provider.stanbol;

import java.util.Collections;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.impl.NamespacePrefixProviderImpl;
import org.apache.stanbol.commons.namespaceprefix.mappings.DefaultNamespaceMappingsEnum;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service
@Property(name=Constants.SERVICE_RANKING,value="1000000")
public class DefaultNamespaceMappingsProvider extends NamespacePrefixProviderImpl implements NamespacePrefixProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultNamespaceMappingsProvider.class);

    public DefaultNamespaceMappingsProvider(){
        super(Collections.<String,String>emptyMap());
        for(DefaultNamespaceMappingsEnum m : DefaultNamespaceMappingsEnum.values()){
            String current = addMapping(m.getPrefix(), m.getNamespace(), true);
            if(current != null){
                log.warn("Found duplicate mapping for prefix '{}'->[{},{}] in {}",
                    new Object[]{m.getPrefix(),current,m.getNamespace(),
                                 DefaultNamespaceMappingsEnum.class.getSimpleName()});
            }
        }        
    }

}
