package org.apache.stanbol.commons.ldviewable;

import org.apache.clerezza.rdf.utils.GraphNode;

/**
 * An LdViewable is a GraphNode associated with a template path. The template 
 * path will be attempted to be resolved based on the accepted target formats
 * to create a representation of the GraphNode. 
 *
 */
public class LdViewable {

	/**
	 * 
	 * @param templatePath the templatePath
	 * @param graphNode the graphNode with the actual content
	 */
	public LdViewable(final String templatePath, final GraphNode graphNode) {
		this.templatePath = templatePath;
		this.graphNode = graphNode;
	}
	
	/**
	 * With this version of the constructor the templatePath is prefixed with
	 * the slash-separated package name of the given Class.
	 * 
	 */
	public LdViewable(final String templatePath, final GraphNode graphNode, final Class<?> clazz) {
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
