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
package org.apache.stanbol.commons.web.viewable;

import org.apache.clerezza.rdf.utils.GraphNode;

/**
 * An RdfViewable is a GraphNode associated with a rendering specification. The 
 * rendering specification determines the way the GraphNode is rendered in the 
 * requested format. The rendering specification is typically a path to a 
 * template. 
 *
 */
public class RdfViewable {

    /**
     * 
     * @param renderingSpecification the rendering specification
     * @param graphNode the graphNode with the actual content
     */
    public RdfViewable(final String renderingSpecification, final GraphNode graphNode) {
        this.renderingSpecification = renderingSpecification;
        this.graphNode = graphNode;
    }
    
    /**
     * With this version of the constructor the rendering specification is prefixed with
     * the slash-separated package name of the given Class.
     * 
     * @param renderingSpecification the rendering specification
     * @param graphNode the graphNode with the actual content
     * @param clazz class which package name will be used as prefix
     * 
     */
    public RdfViewable(final String renderingSpecification, final GraphNode graphNode, final Class<?> clazz) {
        final String slahSeparatedPacakgeName = clazz.getPackage().getName().replace('.', '/');
        if (renderingSpecification.startsWith("/")) {
            this.renderingSpecification = slahSeparatedPacakgeName+renderingSpecification;
        } else {
            this.renderingSpecification = slahSeparatedPacakgeName+'/'+renderingSpecification;
        }
        this.graphNode = graphNode;
    }
    
    private String renderingSpecification;
    private GraphNode graphNode;
    
    public String getRenderingSpecification() {
        return renderingSpecification;
    }
    
    public GraphNode getGraphNode() {
        return graphNode;
    }
}
