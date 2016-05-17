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
package org.apache.stanbol.commons.ldpathtemplate;

import freemarker.cache.TemplateLoader;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.Writer;

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.template.engine.TemplateEngine;
import org.apache.stanbol.commons.ldpath.clerezza.ClerezzaBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service renders a GraphNode to a Writer given the 
 * path of an ldpath template
 *
 */
@Component
@Service(LdRenderer.class)
public class LdRenderer {
	
	private static final String TEMPLATES_PATH_IN_BUNDLES = "templates/";

	private static final Logger log = LoggerFactory.getLogger(LdRenderer.class);
	
	@Reference
	private TemplateLoader templateLoader;

	/**
	 * Renders a GraphNode with a template located in the templates
	 * folder of any active bundle
	 * 
	 * @param node the GraphNode to be rendered
	 * @param templatePath the freemarker path to the template
	 * @param out where the result is written to
	 */
	public void render(GraphNode node, final String templatePath, Writer out) {	
		//A GraphNode backend could be graph unspecific, so the same engine could be
		//reused, possibly being signifantly more performant (caching, etc.)
		RDFBackend<RDFTerm> backend = new ClerezzaBackend(node.getGraph());
		RDFTerm context = node.getNode();
		TemplateEngine<RDFTerm> engine = new TemplateEngine<RDFTerm>(backend);
		engine.setTemplateLoader(templateLoader);
		try {
			engine.processFileTemplate(context, templatePath, null, out);
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}
	}
	
}
