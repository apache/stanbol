package org.apache.stanbol.enhancer.engines.celi.testutils;

import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestUtils {
    
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);
    
    private TestUtils(){}
    
    public static void logEnhancements(ContentItem ci) {
        JenaSerializerProvider serializer = new JenaSerializerProvider();
        ByteArrayOutputStream logOut = new ByteArrayOutputStream();
        serializer.serialize(logOut, ci.getMetadata(), TURTLE);
        log.info("Enhancements: \n{}",new String(logOut.toByteArray(),Charset.forName("UTF-8")));
    }


}
