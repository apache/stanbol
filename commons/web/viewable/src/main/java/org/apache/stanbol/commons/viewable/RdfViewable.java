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
package org.apache.stanbol.commons.viewable;

import org.apache.clerezza.rdf.utils.GraphNode;

/**
 * An RdfViewable is a GraphNode associated with a template path. The template 
 * path will be attempted to be resolved based on the accepted target formats
 * to create a representation of the GraphNode. 
 *
 */
public class RdfViewable {

	/**
	 * 
	 * @param templatePath the templatePath
	 * @param graphNode the graphNode with the actual content
	 */
	public RdfViewable(final String templatePath, final GraphNode graphNode) {
		this.templatePath = templatePath;
		this.graphNode = graphNode;
	}
	
	/**
	 * With this version of the constructor the templatePath is prefixed with
	 * the slash-separated package name of the given Class.
	 * 
	 */
	public RdfViewable(final String templatePath, final GraphNode graphNode, final Class<?> clazz) {
		final String slahSeparatedPacakgeName = clazz.getPackage().getName().replace('.', '/');
		if (templatePath.startsWith("/")) {
			this.templatePath = slahSeparatedPacakgeName+templatePath;
		} else {
			this.templatePath = slahSeparatedPacakgeName+'/'+templatePath;
		}
		this.graphNode = graphNode;
	}
	
	private String templatePath;
	private GraphNode graphNode;
	
	public String getTemplatePath() {
		return templatePath;
	}
	
	public GraphNode getGraphNode() {
		return graphNode;
	}
}
