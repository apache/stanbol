package org.apache.stanbol.commons.ldviewable.mbw;

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
import org.apache.stanbol.commons.ldviewable.LdViewable;

@Component
@Service(LdViewableWriter.class)
@Produces("text/html")
public class LdViewableWriter implements MessageBodyWriter<LdViewable> {

	@Reference
	private LdRenderer ldRenderer;
	
	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return LdViewable.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(LdViewable t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(LdViewable t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		Writer out = new OutputStreamWriter(entityStream, "utf-8"); 
		ldRenderer.render(t.getGraphNode(), "html/"+t.getTemplatePath(), out);
		out.flush();
	}

}
