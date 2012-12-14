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
package org.apache.stanbol.enhancer.engines.tika;

import static org.apache.stanbol.enhancer.engines.tika.metadata.OntologyMappings.addDcMappings;
import static org.apache.stanbol.enhancer.engines.tika.metadata.OntologyMappings.addGeoMappings;
import static org.apache.stanbol.enhancer.engines.tika.metadata.OntologyMappings.addMediaResourceOntologyMappings;
import static org.apache.stanbol.enhancer.engines.tika.metadata.OntologyMappings.addNepomukExifMappings;
import static org.apache.stanbol.enhancer.engines.tika.metadata.OntologyMappings.addNepomukMessageMappings;
import static org.apache.stanbol.enhancer.engines.tika.metadata.OntologyMappings.addRdfsMappings;
import static org.apache.stanbol.enhancer.engines.tika.metadata.OntologyMappings.addSkosMappings;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;
import static org.apache.tika.mime.MediaType.TEXT_PLAIN;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.tika.handler.MultiHandler;
import org.apache.stanbol.enhancer.engines.tika.handler.PlainTextHandler;
import org.apache.stanbol.enhancer.engines.tika.metadata.OntologyMappings;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSink;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

/**
 * EnhancementEngine based on Apache Tika that converts the content of parsed 
 * content items to xhtml and plain text. In addition it converts extracted 
 * metadata to RDF and adds them to the {@link ContentItem#getMetadata()}
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate = true, metatype = true, inherit=true)
@Service
@Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME,value="tika"),
    @Property(name=TikaEngine.SKIP_LINEBREAKS_WITHIN_CONTENT, boolValue=TikaEngine.DEFAULT_SKIP_LINEBREAKS),
    @Property(name=TikaEngine.MAPPING_MEDIA_RESOURCE,boolValue=TikaEngine.DEFAULT_MAPPING_MEDIA_RESOURCE_STATE),
    @Property(name=TikaEngine.MAPPING_DUBLIN_CORE_TERMS,boolValue=TikaEngine.DEFAULT_MAPPING_DUBLIN_CORE_TERMS_STATE),
    @Property(name=TikaEngine.MAPPING_NEPOMUK_MESSAGE,boolValue=TikaEngine.DEFAULT_MAPPING_NEPOMUK_MESSAGE_STATE),
    @Property(name=TikaEngine.MAPPING_NEPOMUK_EXIF,boolValue=TikaEngine.DEFAULT_MAPPING_NEPOMUK_EXIF_STATE),
    @Property(name=TikaEngine.MAPPING_SKOS,boolValue=TikaEngine.DEFAULT_MAPPING_SKOS_STATE),
    @Property(name=TikaEngine.MAPPING_RDFS,boolValue=TikaEngine.DEFAULT_MAPPING_RDFS_STATE),
    @Property(name=TikaEngine.MAPPING_GEO,boolValue=TikaEngine.DEFAULT_MAPPING_GEO_STATE)
})
public class TikaEngine 
        extends AbstractEnhancementEngine<RuntimeException,RuntimeException> 
        implements EnhancementEngine, ServiceProperties {
    private final Logger log = LoggerFactory.getLogger(TikaEngine.class);
        
    public static final String SKIP_LINEBREAKS_WITHIN_CONTENT = "stanbol.engines.tika.skipLinebreaks";
    //Metadata -> Ontology mapping configuration
    public static final String MAPPING_MEDIA_RESOURCE = "stanbol.engine.tika.mapping.mediaResource";
    public static final boolean DEFAULT_MAPPING_MEDIA_RESOURCE_STATE = true;
    public static final String MAPPING_DUBLIN_CORE_TERMS = "stanbol.engine.tika.mapping.dcTerms";
    public static final boolean DEFAULT_MAPPING_DUBLIN_CORE_TERMS_STATE = true;
    public static final String MAPPING_NEPOMUK_MESSAGE = "stanbol.engine.tika.mapping.nepomukMessage";
    public static final boolean DEFAULT_MAPPING_NEPOMUK_MESSAGE_STATE = true;
    public static final String MAPPING_NEPOMUK_EXIF = "stanbol.engine.tika.mapping.nepomukExif";
    public static final boolean DEFAULT_MAPPING_NEPOMUK_EXIF_STATE = true;
    public static final String MAPPING_SKOS = "stanbol.engine.tika.mapping.skos";
    public static final boolean DEFAULT_MAPPING_SKOS_STATE = false;
    public static final String MAPPING_RDFS = "stanbol.engine.tika.mapping.rdfs";
    public static final boolean DEFAULT_MAPPING_RDFS_STATE = false;
    public static final String MAPPING_GEO = "stanbol.engine.tika.mapping.geo";
    public static final boolean DEFAULT_MAPPING_GEO_STATE = true;
    
    public static final boolean DEFAULT_SKIP_LINEBREAKS = false;
    
    private boolean skipLinebreaks = DEFAULT_SKIP_LINEBREAKS;
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING;

    protected static final MediaType XHTML = new MediaType("application", "xhtml+xml");
    protected static final Charset UTF8 = Charset.forName("UTF-8");
    
    private TikaConfig config;
    private Parser parser;
    private Detector detector;
    private OntologyMappings ontologyMappings;
    /**
     * The {@link ContentItemFactory} is used to create {@link Blob}s for the
     * plain text and XHTML version of the processed ContentItem
     */
    @Reference
    private ContentItemFactory ciFactory;
    
    private static class MediaTypeAndStream {
        String uri;
        MediaType mediaType;
        InputStream in;
    }
    /**
     * Default constructor used by OSGI
     */
    public TikaEngine() {}
    /**
     * Used by the unit tests to init the {@link ContentItemFactory} outside
     * an OSGI environment.
     * @param cifactory
     */
    TikaEngine(ContentItemFactory cifactory) {
        this.ciFactory = cifactory;
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        return ENHANCE_ASYNC;
    }
    
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        MediaTypeAndStream mtas = extractMediaType(ci);
        if(mtas.mediaType == null){
            return; //unable to parse and detect content type
        }
        MediaType plainMediaType = mtas.mediaType.getBaseType();
        if(plainMediaType.equals(MediaType.TEXT_PLAIN)){
            return; //we need not to process plain text!
        }
        ParseContext context = new ParseContext();
        context.set(Parser.class,parser);
        Set<MediaType> supproted = parser.getSupportedTypes(context);
        if(supproted.contains(plainMediaType)) {
            final InputStream in;
            if(mtas.in == null){
                in = ci.getStream();
            } else {
                in = mtas.in;
            }
            Metadata metadata = new Metadata();
            //set the already parsed contentType
            metadata.set(Metadata.CONTENT_TYPE, mtas.mediaType.toString());
            ContentSink plainTextSink;
            try {
                plainTextSink = ciFactory.createContentSink(TEXT_PLAIN +"; charset="+UTF8.name());
            } catch (IOException e) {
                IOUtils.closeQuietly(in); //close the input stream
                throw new EngineException("Error while initialising Blob for" +
                		"writing the text/plain version of the parsed content",e);
            }
            final Writer plainTextWriter = new OutputStreamWriter(plainTextSink.getOutputStream(), UTF8);
            final ContentHandler textHandler = new BodyContentHandler( //only the Body
                new PlainTextHandler(plainTextWriter, false,skipLinebreaks)); //skip ignoreable
            final ToXMLContentHandler xhtmlHandler;
            final ContentHandler mainHandler;
            ContentSink xhtmlSink = null;
            try {
                if(!plainMediaType.equals(XHTML)){ //do not parse XHTML from XHTML
                    try {
                        xhtmlSink = ciFactory.createContentSink(XHTML +"; charset="+UTF8.name());
                    } catch (IOException e) {
                        throw new EngineException("Error while initialising Blob for" +
                                "writing the application/xhtml+xml version of the parsed content",e);
                    }
                    try {
                        xhtmlHandler = new ToXMLContentHandler(xhtmlSink.getOutputStream(),UTF8.name());
                    } catch (UnsupportedEncodingException e) {
                        throw new EngineException("This system does not support the encoding "+UTF8,e);
                    }
                    mainHandler = new MultiHandler(textHandler,xhtmlHandler);
                } else {
                    mainHandler = textHandler;
                    xhtmlHandler = null;
                    xhtmlSink = null;
                }
                try {
                    parser.parse(in, mainHandler, metadata, context);
                } catch (Exception e) {
                    throw new EngineException("Unable to convert ContentItem "+
                            ci.getUri()+" with mimeType '"+ci.getMimeType()+"' to "+
                            "plain text!",e);
                }
            } finally { //ensure that the writers are closed correctly
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(plainTextWriter);
                if(xhtmlSink != null){
                    IOUtils.closeQuietly(xhtmlSink.getOutputStream());
                }
            }
            String random = randomUUID().toString();
            UriRef textBlobUri = new UriRef("urn:tika:text:"+random);
            ci.addPart(textBlobUri, plainTextSink.getBlob());
            if(xhtmlHandler != null){
                UriRef xhtmlBlobUri = new UriRef("urn:tika:xhtml:"+random);
                ci.addPart(xhtmlBlobUri,  xhtmlSink.getBlob());
            }
            //add the extracted metadata
            if(log.isInfoEnabled()){
                for(String name : metadata.names()){
                    log.info("{}: {}",name,Arrays.toString(metadata.getValues(name)));
                }
            }
            ci.getLock().writeLock().lock();
            try {
                ontologyMappings.apply(ci.getMetadata(), ci.getUri(), metadata);
            }finally{
                ci.getLock().writeLock().unlock();
            }
        } //else not supported format

    }

    /**
     * Getter for the contentType. If not set or {@link MediaType#OCTET_STREAM}
     * than the media type is detected.<p>
     * This method returns the MediaType and the Stream used to detect the
     * MimeType. This allows to reuse the stream and the mediaType
     * @param ci
     * @param mediaTypeArray
     * @return
     */
    private MediaTypeAndStream extractMediaType(ContentItem ci) {
        MediaTypeAndStream mtas = new MediaTypeAndStream();
        mtas.mediaType = getMediaType(ci.getBlob());
        mtas.uri = ci.getUri().getUnicodeString();
        if(mtas.mediaType == null || mtas.mediaType.equals(MediaType.OCTET_STREAM)){
            mtas.in = new BufferedInputStream(ci.getStream());
            Metadata m = new Metadata();
            m.add(Metadata.RESOURCE_NAME_KEY, mtas.uri);
            try {
                mtas.mediaType = detector.detect(mtas.in, m);
            } catch (IOException e) {
                log.warn("Exception while detection the MediaType of the" +
                        "parsed ContentItem "+ci.getUri(),e);
                IOUtils.closeQuietly(mtas.in);
                mtas.in = null;
            }
        }
        return mtas;
    }

    /**
     * @param ci
     * @return
     */
    private MediaType getMediaType(Blob blob) {
        String[] mediaTypeArray = blob.getMimeType().split("/");
        if(mediaTypeArray.length != 2){
            log.warn("Encounterd illegal formatted mediaType '{}'  -> will try " +
            		"to detect the mediaType based on the parsed content!",
                blob.getMimeType());
            return null;
        } else {
            return new MediaType(mediaTypeArray[0], mediaTypeArray[1],
                blob.getParameter());
        }
    }
    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        config = TikaConfig.getDefaultConfig();
        this.detector = config.getDetector();
        this.parser = new AutoDetectParser(config);
        this.skipLinebreaks = getBoolean(ctx.getProperties(), 
            SKIP_LINEBREAKS_WITHIN_CONTENT, DEFAULT_SKIP_LINEBREAKS);
        this.ontologyMappings = new OntologyMappings();
        if(getBoolean(ctx.getProperties(), 
            MAPPING_MEDIA_RESOURCE, DEFAULT_MAPPING_MEDIA_RESOURCE_STATE)){
            addMediaResourceOntologyMappings(ontologyMappings);
        }
        if(getBoolean(ctx.getProperties(), 
            MAPPING_DUBLIN_CORE_TERMS, DEFAULT_MAPPING_DUBLIN_CORE_TERMS_STATE)){
            addDcMappings(ontologyMappings);
        }
        if(getBoolean(ctx.getProperties(), 
            MAPPING_NEPOMUK_MESSAGE, DEFAULT_MAPPING_NEPOMUK_MESSAGE_STATE)){
            addNepomukMessageMappings(ontologyMappings);
        }
        if(getBoolean(ctx.getProperties(), 
            MAPPING_NEPOMUK_EXIF, DEFAULT_MAPPING_NEPOMUK_EXIF_STATE)){
            addNepomukExifMappings(ontologyMappings);
        }
        if(getBoolean(ctx.getProperties(), 
            MAPPING_SKOS, DEFAULT_MAPPING_SKOS_STATE)){
            addSkosMappings(ontologyMappings);
        }
        if(getBoolean(ctx.getProperties(), 
            MAPPING_RDFS, DEFAULT_MAPPING_RDFS_STATE)){
            addRdfsMappings(ontologyMappings);
        }
        if(getBoolean(ctx.getProperties(), 
            MAPPING_GEO, DEFAULT_MAPPING_GEO_STATE)){
            addGeoMappings(ontologyMappings);
        }
    }
    @Override
    protected void deactivate(ComponentContext ctx) throws RuntimeException {
        this.config = null;
        this.parser = null;
        this.detector = null;
        this.skipLinebreaks = DEFAULT_SKIP_LINEBREAKS;
        this.ontologyMappings = null;
        super.deactivate(ctx);
    }
    private static boolean getBoolean(Dictionary<?,?> properties, String key, boolean defaultState){
        Object value = properties.get(key);
        return value instanceof Boolean ? (Boolean)value :
            value != null ? Boolean.parseBoolean(value.toString()) : defaultState;
    }

    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(
            Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    }

}
