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
package org.apache.stanbol.commons.viewable.mbw;

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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.ldpathtemplate.LdRenderer;
import org.apache.stanbol.commons.viewable.Viewable;

@Component
@Service(ViewableWriter.class)
@Produces("text/html")
public class ViewableWriter implements MessageBodyWriter<Viewable> {

	@Reference
	private LdRenderer ldRenderer;
	
	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return Viewable.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(Viewable t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(final Viewable t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		Writer out = new OutputStreamWriter(entityStream, "utf-8"); 
		ldRenderer.renderPojo(new Wrapper(t.getPojo()), "html/"+t.getTemplatePath(), out);
		out.flush();
	}
	static public class Wrapper {

		private Object wrapped;
		public Wrapper(Object wrapped) {
			this.wrapped = wrapped;
		}
		public Object getIt() {
			return wrapped;
		}
		
		
	}

}
