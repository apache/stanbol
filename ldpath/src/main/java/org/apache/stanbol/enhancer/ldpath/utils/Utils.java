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
package org.apache.stanbol.enhancer.ldpath.utils;

import static org.apache.stanbol.enhancer.ldpath.EnhancerLDPath.getConfig;

import java.io.StringReader;
import java.util.Map;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.commons.ldpath.clerezza.ClerezzaBackend;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.api.selectors.NodeSelector;
import at.newmedialab.ldpath.parser.ParseException;
import at.newmedialab.ldpath.parser.RdfPathParser;

public final class Utils {
    
    private Utils(){};
    


    public static RDFBackend<Resource> EMPTY_BACKEND;
    
    /**
     * Returns an empty {@link RDFBackend} instance intended to be used to create
     * {@link RdfPathParser} instances<p>
     * {@link RDFBackend} has currently two distinct roles <ol>
     * <li> to traverse the graph ( basically the 
     * {@link RDFBackend#listObjects(Object, Object)} and
     * {@link RDFBackend#listSubjects(Object, Object)} methods)
     * <li> to create Nodes and convert Nodes
     * </ol>
     * The {@link RdfPathParser} while requiring an {@link RDFBackend} instance
     * depends only on the 2nd role. Therefore the data managed by the
     * {@link RDFBackend} instance are of no importance.<p>
     * The {@link RDFBackend} provided by this constant is intended to be only
     * used for the 2nd purpose and does contain no information!
     * <li>
     */
    public static RDFBackend<Resource> getEmptyBackend(){
        if(EMPTY_BACKEND == null){
            EMPTY_BACKEND = new ClerezzaBackend(new SimpleMGraph());
        }
        return EMPTY_BACKEND;
    }
    

    
    public static NodeSelector<Resource> parseSelector(String path) throws ParseException {
        return parseSelector(path, (Map<String,String>)null);
    }
    public static NodeSelector<Resource> parseSelector(String path, Map<String,String> additionalNamespaceMappings) throws ParseException {
        RdfPathParser<Resource> parser = new RdfPathParser<Resource>(
               getEmptyBackend(), getConfig(), new StringReader(path));
        return parser.parseSelector(additionalNamespaceMappings);
    }
}
