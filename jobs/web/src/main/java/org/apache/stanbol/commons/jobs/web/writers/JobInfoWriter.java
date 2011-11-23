package org.apache.stanbol.commons.jobs.web.writers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.jobs.api.JobInfo;
import org.apache.stanbol.commons.jobs.impl.JobInfoImpl;

/**
 * Writer for job info
 * 
 * @author enridaga
 *
 */
@Provider
@Produces({"application/json","text/plain"})
public class JobInfoWriter implements MessageBodyWriter<JobInfo> {
    
    private ByteArrayOutputStream stream = null;
    
    public ByteArrayOutputStream toStream(JobInfo t, String mediaType) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (mediaType.equals("application/json")) {
            // Json
            StringBuilder b = new StringBuilder()
            .append("{")
            .append("\n\t").append("\"status\": ").append("\"").append(t.getStatus()).append("\"")
            .append(",\n\t").append("\"outputLocation\": ").append("\"").append(t.getOutputLocation()).append("\"")
            .append(",\n\t").append("\"messages\": ").append("[");
            for(String m : t.getMessages()){
                b.append("\n\t\t\"").append(m).append("\",");
            }
            b.append("\n\t\t]\n}");
            IOUtils.write(b.toString(), stream);
        } else if (mediaType.equals("text/plain")) {
            // Plain text
            StringBuilder b = new StringBuilder()
            .append("Status: ").append(t.getStatus())
            .append("\nOutput location: ").append(t.getOutputLocation())
            .append("\nMessages:");
            for(String m : t.getMessages()){
                b.append("\n - ").append(m);
            }
            b.append("\n");
            IOUtils.write(b.toString(), stream);
        }
        return stream;
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JobInfoImpl.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(JobInfo t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        try {
            stream = toStream(t, mediaType.toString());
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
        return Integer.valueOf(stream.toByteArray().length).longValue();
    }

    @Override
    public void writeTo(JobInfo t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String,Object> httpHeaders,
                        OutputStream entityStream) throws WebApplicationException {
        if (stream == null) {
            try {
                toStream(t, mediaType.toString()).writeTo(entityStream);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        } else {
            try {
                stream.writeTo(entityStream);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
            stream = null;
        }
    }

}
