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
package org.apache.stanbol.commons.web.viewable.ldpath.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.stanbol.commons.ldpath.clerezza.ClerezzaBackend;
import org.apache.stanbol.commons.web.viewable.RdfViewable;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.template.engine.TemplateEngine;

import freemarker.cache.TemplateLoader;
import freemarker.template.TemplateException;

@Produces(MediaType.TEXT_HTML)
@Provider
public class LdViewableWriter implements MessageBodyWriter<RdfViewable> {

	private TemplateLoader templateLoader;
	
	public LdViewableWriter(TemplateLoader templateLoader) {
	    if(templateLoader == null){
	        throw new IllegalArgumentException("The parsed TemplateLoader MUST NOT be NULL!");
	    }
        this.templateLoader = templateLoader;
    }

    @Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return RdfViewable.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(RdfViewable t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(RdfViewable t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		Writer out = new OutputStreamWriter(entityStream, "utf-8"); 
		render(t.getGraphNode(), "html/"+t.getTemplatePath(), out);
		out.flush();
	}
    /**
     * Renders a GraphNode with a template located in the templates
     * folder of any active bundle
     * 
     * @param node the GraphNode to be rendered
     * @param templatePath the freemarker path to the template
     * @param out where the result is written to
     */
    private void render(GraphNode node, final String templatePath, Writer out) { 
        //A GraphNode backend could be graph unspecific, so the same engine could be
        //reused, possibly being signifantly more performant (caching, etc.)
        RDFBackend<Resource> backend = new ClerezzaBackend(node.getGraph());
        Resource context = node.getNode();
        TemplateEngine<Resource> engine = new TemplateEngine<Resource>(backend);
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
