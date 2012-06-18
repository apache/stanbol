package org.apache.stanbol.flow.cameljobmanager.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;


import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Component;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

@Component(immediate = true, metatype = true)
@Converter
public final class FromContentItemConverter {
	
	//TODO : see how to fix a global default mediatype for all stanbol app
	public static String defaultMediaType = "application/rdf+xml"; 
		//"text/turtle";
	
	public FromContentItemConverter(){}
	
	//TODO : see why the Reference annotation for serializer always return a null object
	// if solution found remove FrameworkUtil retrieving.
    public Serializer serializer;
    
	
    @Converter
	public ByteArrayOutputStream contentItemToByteArrayOutputStream(ContentItem ci, Exchange exchange) throws UnsupportedEncodingException, IOException{
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	
    	BundleContext bundleContext = FrameworkUtil.getBundle(FromContentItemConverter.class).getBundleContext();
    	serializer = null;
		ServiceReference ref = bundleContext.getServiceReference(Serializer.class.getName());
		if(ref != null){
            serializer = (Serializer) bundleContext.getService(ref);
            String mediatype = (String) exchange.getIn().getHeader("mediatype", defaultMediaType);
    		serializer.serialize(out, ci.getMetadata(), mediatype);
        } else {
        	out.write("Error during body conversion, serviceReference is null.".getBytes());
        }
		return out;
	}
    
    @Converter
	public InputStream contentItemToInputStream(ContentItem ci, Exchange exchange) throws UnsupportedEncodingException, IOException{
		return new ByteArrayInputStream(contentItemToByteArrayOutputStream(ci, exchange).toByteArray());
	}
    
    @Converter
	public ContentItem fromFileToCi(GenericFile file , Exchange exchange) throws UnsupportedEncodingException, IOException{
    	
    	//TODO : use Tika mimeType detection capability in order to process any file format
		String mimetype = "text/plain";
		
		TypeConverterRegistry registry = exchange.getContext().getTypeConverterRegistry();
		Class from = file.getBody().getClass();
		TypeConverter tc = registry.lookup(byte[].class, from);
        if (tc != null) {
            Object body = file.getBody();
            byte[] content = tc.convertTo(byte[].class, exchange, body); 
            return new InMemoryContentItem(content, mimetype);
        }
		 
		return new InMemoryContentItem("Error during transformation".getBytes(), mimetype);
	}
        
}
