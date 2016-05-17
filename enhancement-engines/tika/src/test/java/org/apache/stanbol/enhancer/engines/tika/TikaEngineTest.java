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

import static java.util.Collections.singleton;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.stanbol.enhancer.engines.tika.TikaEngine.XHTML;
import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.CANNOT_ENHANCE;
import static org.apache.tika.mime.MediaType.OCTET_STREAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TikaEngineTest {

    private static final Logger log = LoggerFactory.getLogger(TikaEngineTest.class);
    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static TikaEngine engine;
    private static MockComponentContext context;
    private static LiteralFactory lf = LiteralFactory.getInstance();
    /**
     * Required to make this test independent of the timeZone of the local host.
     */
    private static SimpleDateFormat dateDefaultTimezone =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new DateFormatSymbols(Locale.US));

    @BeforeClass
    public static void setUpServices() throws IOException {
        context = new MockComponentContext();
        context.properties.put(TikaEngine.PROPERTY_NAME, "tika");
        //to test unmapped properties
        context.properties.put(TikaEngine.UNMAPPED_PROPERTIES, "true");
    }

    @Before
    public void bindServices() throws ConfigurationException {
        if(engine == null){
            engine = new TikaEngine(ciFactory);
            engine.activate(context);
        }
    }

    @Test
    public void testHtml() throws EngineException, IOException {
        log.info(">>> testHtml <<<");
        ContentItem ci = createContentItem("test.html", "text/html; charset=UTF-8");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<title>The Apache Stanbol Enhancer</title>",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    @Test
    public void testPdf() throws EngineException, IOException {
        log.info(">>> testPdf <<<");
        //PDF created by Apple Pages
        ContentItem ci = createContentItem("test.pdf", "application/pdf");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities ");        
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<div class=\"page\">",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
        
        //PDF created by OpenOffice
        ci = createContentItem("test2.pdf", "application/pdf");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        //validate plain text results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<div class=\"page\">",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    @Test
    public void testMsWord() throws EngineException, IOException {
        log.info(">>> testMsWord <<<");
        ContentItem ci = createContentItem("test.doc", "application/msword");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<title>",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");    }
    @Test
    public void testRtf() throws EngineException, IOException {
        log.info(">>> testRtf <<<");
        ContentItem ci = createContentItem("test.rtf", "application/rtf");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<title>",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    @Test
    public void testOdt() throws EngineException, IOException {
        log.info(">>> testOdt <<<");
        ContentItem ci = createContentItem("test.odt", "application/vnd.oasis.opendocument.text");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<title>",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    @Test
    public void testEMail() throws EngineException, IOException, ParseException {
        log.info(">>> testEMail <<<");
        ContentItem ci = createContentItem("test.email.txt", "message/rfc822");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "Julien Nioche commented on TIKA-461:",
            "I'll have a look at mime4j and try to use it in Tika",
            "> RFC822 messages not parsed",
            "Key: TIKA-461",
            "URL: https://issues.apache.org/jira/browse/TIKA-461");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob, 
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<title>\\[jira\\] Commented: \\(TIKA-461\\) RFC822 messages not parsed</title>",
            "<body><p>",
            "Julien Nioche commented on TIKA-461:",
            "I'll have a look at mime4j and try to use it in Tika",
            "&gt; RFC822 messages not parsed",
            "Key: TIKA-461",
            "URL: https://issues.apache.org/jira/browse/TIKA-461");
        //no check the extracted metadata!
        //DC
        //STANBOL-757: dc:date no longer added by Tika 1.2 (dc:created is still present)
        //verifyValue(ci, new IRI(NamespaceEnum.dc+"date"), XSD.dateTime,"2010-09-06T09:25:34Z");
        verifyValue(ci, new IRI(NamespaceEnum.dc+"format"), null,"message/rfc822");
        //STANBOL-757: dc:subject no longer added by Tika1.2 (dc:title is used instead)
        //verifyValue(ci, new IRI(NamespaceEnum.dc+"subject"), null,"[jira] Commented: (TIKA-461) RFC822 messages not parsed");
        verifyValue(ci, new IRI(NamespaceEnum.dc+"title"), null,"[jira] Commented: (TIKA-461) RFC822 messages not parsed");
        verifyValue(ci, new IRI(NamespaceEnum.dc+"creator"), null,"Julien Nioche (JIRA) <jira@apache.org>");
        verifyValue(ci, new IRI(NamespaceEnum.dc+"created"), XSD.dateTime,"2010-09-06T09:25:34Z");
        
        //Media Ontology
        verifyValue(ci, new IRI(NamespaceEnum.media+"creationDate"),XSD.dateTime,"2010-09-06T09:25:34Z");
        verifyValue(ci, new IRI(NamespaceEnum.media+"hasFormat"),null,"message/rfc822");
        verifyValue(ci, new IRI(NamespaceEnum.media+"hasCreator"),null,"Julien Nioche (JIRA) <jira@apache.org>");
        verifyValue(ci, new IRI(NamespaceEnum.media+"hasContributor"),null,"Julien Nioche (JIRA) <jira@apache.org>");
        //STANBOL-757: This was present with Tika 1.1 because its mapping from dc:subject 
//        verifyValue(ci, new IRI(NamespaceEnum.media+"hasKeyword"),null,"[jira] Commented: (TIKA-461) RFC822 messages not parsed");

        
        //Nepomuk Message
        String message = "http://www.semanticdesktop.org/ontologies/2007/03/22/nmo#";
        verifyValue(ci, new IRI(message+"from"),null,"Julien Nioche (JIRA) <jira@apache.org>");
        verifyValue(ci, new IRI(message+"to"),null,"dev@tika.apache.org");
        
    }
    @Test
    public void testMp3() throws EngineException, IOException, ParseException {
        log.info(">>> testMp3 <<<");
        ContentItem ci = createContentItem("testMP3id3v24.mp3", "audio/mpeg");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "Test Title",
            "Test Artist",
            "Test Album");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        //Test AudioTrack metadata
        BlankNodeOrIRI audioTrack = verifyBlankNodeOrIRI(ci, new IRI(NamespaceEnum.media+"hasTrack"));
        //types
        verifyValues(ci, audioTrack, RDF.type, 
            new IRI(NamespaceEnum.media+"MediaFragment"),
            new IRI(NamespaceEnum.media+"Track"),
            new IRI(NamespaceEnum.media+"AudioTrack"));
        //properties
        verifyValue(ci, audioTrack, new IRI(NamespaceEnum.media+"hasFormat"), XSD.string, "Mono");
        verifyValue(ci, audioTrack, new IRI(NamespaceEnum.media+"samplingRate"), XSD.int_, "44100");
        verifyValue(ci, audioTrack, new IRI(NamespaceEnum.media+"hasCompression"), XSD.string, "MP3");
    }
    /**
     * Tests mappings for the Mp4 metadata extraction capabilities added to
     * Tika 1.1 (STANBOL-627)
     * @throws EngineException
     * @throws IOException
     * @throws ParseException
     */
    @Test 
    public void testMp4() throws EngineException, IOException, ParseException {
        log.info(">>> testMp4 <<<");
        ContentItem ci = createContentItem("testMP4.m4a", "audio/mp4");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "Test Title",
            "Test Artist",
            "Test Album");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        //Test AudioTrack metadata
        BlankNodeOrIRI audioTrack = verifyBlankNodeOrIRI(ci, new IRI(NamespaceEnum.media+"hasTrack"));
        //types
        verifyValues(ci, audioTrack, RDF.type, 
            new IRI(NamespaceEnum.media+"MediaFragment"),
            new IRI(NamespaceEnum.media+"Track"),
            new IRI(NamespaceEnum.media+"AudioTrack"));
        //properties
        verifyValue(ci, audioTrack, new IRI(NamespaceEnum.media+"hasFormat"), XSD.string, "Stereo");
        verifyValue(ci, audioTrack, new IRI(NamespaceEnum.media+"samplingRate"), XSD.int_, "44100");
        verifyValue(ci, audioTrack, new IRI(NamespaceEnum.media+"hasCompression"), XSD.string, "M4A");
    }
    @Test
    public void testGEOMetadata() throws EngineException, IOException, ParseException{
        log.info(">>> testGEOMetadata <<<");
        //first validate Media RDFTerm Ontology
        IRI hasLocation = new IRI(NamespaceEnum.media+"hasLocation");
        IRI locationLatitude = new IRI(NamespaceEnum.media+"locationLatitude");
        IRI locationLongitude = new IRI(NamespaceEnum.media+"locationLongitude");
        //IRI locationAltitude = new IRI(NamespaceEnum.media+"locationAltitude");
        ContentItem ci = createContentItem("testJPEG_GEO.jpg", OCTET_STREAM.toString());//"video/x-ms-asf");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Iterator<Triple> it = ci.getMetadata().filter(ci.getUri(),hasLocation, null);
        assertTrue(it.hasNext());
        RDFTerm r = it.next().getObject();
        assertFalse(it.hasNext());
        assertTrue(r instanceof BlankNodeOrIRI);
        BlankNodeOrIRI location = verifyBlankNodeOrIRI(ci, hasLocation);
        //lat
        verifyValue(ci, location, locationLatitude, XSD.double_, "12.54321");
        //long
        verifyValue(ci, location, locationLongitude, XSD.double_, "-54.1234");
        
        //second the GEO ont
        IRI lat = new IRI(NamespaceEnum.geo+"lat");
        IRI lon = new IRI(NamespaceEnum.geo+"long");
        //lat
        verifyValue(ci, lat, XSD.double_, "12.54321");
        //long
        verifyValue(ci, lon, XSD.double_, "-54.1234");
    }
    

    
    public void testMetadata() throws EngineException, ParseException, IOException{
        log.info(">>> testMetadata <<<");
        ContentItem ci = createContentItem("testMP3id3v24.mp3", "audio/mpeg");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        verifyValue(ci,new IRI(NamespaceEnum.dc+"creator"),null,"Test Artist");
        verifyValue(ci, new IRI(NamespaceEnum.dc+"title"),null,"Test Album");
        verifyValue(ci, new IRI(NamespaceEnum.dc+"format"),null,"audio/mpeg");
        verifyValue(ci, new IRI(NamespaceEnum.media+"hasFormat"),null,"audio/mpeg");
        verifyValue(ci, new IRI(NamespaceEnum.media+"mainOriginalTitle"),null,"Test Album");
        verifyValue(ci, new IRI(NamespaceEnum.media+"hasContributor"),null,"Test Artist");
        verifyValue(ci, new IRI(NamespaceEnum.media+"releaseDate"),XSD.string,"2008");
        verifyValue(ci, new IRI(NamespaceEnum.media+"hasGenre"),null,"Rock");
        verifyValue(ci, new IRI(NamespaceEnum.media+"hasCreator"),null,"Test Artist");
    }
    @Test
    public void testExifMetadata() throws EngineException, ParseException, IOException {
        log.info(">>> testExifMetadata <<<");
        String exif = "http://www.semanticdesktop.org/ontologies/2007/05/10/nexif#";
        ContentItem ci = createContentItem("testJPEG_EXIF.jpg", "image/jpeg");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        verifyValue(ci, new IRI(exif+"make"),null,"Canon");
        verifyValue(ci, new IRI(exif+"software"),null,"Adobe Photoshop CS3 Macintosh");
        verifyValue(ci, new IRI(exif+"dateTimeOriginal"),XSD.dateTime,"2009-08-11T09:09:45");
        verifyValue(ci, new IRI(exif+"relatedImageWidth"),XSD.int_,"100");
        verifyValue(ci, new IRI(exif+"fNumber"),XSD.double_,"5.6");
        verifyValue(ci, new IRI(exif+"model"),null,"Canon EOS 40D");
        verifyValue(ci, new IRI(exif+"isoSpeedRatings"),XSD.int_,"400");
        verifyValue(ci, new IRI(exif+"xResolution"),XSD.double_,"240.0");
        verifyValue(ci, new IRI(exif+"flash"),XSD.boolean_,"false");
        verifyValue(ci, new IRI(exif+"exposureTime"),XSD.double_,"6.25E-4");
        verifyValue(ci, new IRI(exif+"yResolution"),XSD.double_,"240.0");
        verifyValue(ci, new IRI(exif+"resolutionUnit"),XSD.string,"Inch");
        verifyValue(ci, new IRI(exif+"focalLength"),XSD.double_,"194.0");
        verifyValue(ci, new IRI(exif+"relatedImageLength"),XSD.int_,"68");
        verifyValue(ci, new IRI(exif+"bitsPerSample"),XSD.int_,"8");
        //also Media Ontology mappings for Exif
        verifyValue(ci, new IRI(NamespaceEnum.media+"frameHeight"),XSD.int_,"68");
        verifyValue(ci, new IRI(NamespaceEnum.media+"frameWidth"),XSD.int_,"100");
        verifyValue(ci, new IRI(NamespaceEnum.media+"hasFormat"),null,"image/jpeg");
        verifyValue(ci, new IRI(NamespaceEnum.media+"creationDate"),XSD.dateTime,"2009-08-11T09:09:45");
        verifyValues(ci, new IRI(NamespaceEnum.media+"hasKeyword"),null,"serbor","moscow-birds","canon-55-250");
        //and finally the mapped DC properties
        verifyValue(ci, new IRI(NamespaceEnum.dc+"format"),null,"image/jpeg");
        verifyValue(ci, new IRI(NamespaceEnum.dc+"created"),XSD.dateTime,"2009-08-11T09:09:45");
        verifyValue(ci, new IRI(NamespaceEnum.dc+"modified"),XSD.dateTime,"2009-10-02T23:02:49");
        verifyValues(ci, new IRI(NamespaceEnum.dc+"subject"), null, "serbor","moscow-birds","canon-55-250");
    }
    
    /**
     * Tests unmapped properties as added by <a href="https://issues.apache.org/jira/browse/STANBOL-947">
     * STANBOL-947</a>
     * @throws EngineException
     * @throws IOException
     * @throws ParseException 
     */
    @Test
    public void testUnmappedProperties() throws EngineException, IOException, ParseException {
        log.info(">>> testUnmappedProperties <<<");
        //reuses the image with EXIF metadata
        ContentItem ci = createContentItem("testMP4.m4a", "audio/mp4");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        //test that the "xmpDM:logComment" is present
        verifyValue(ci, new IRI("urn:tika.apache.org:tika:xmpDM:logComment"), null,"Test Comments");
    }
    
    @Test
    public void testContentTypeDetection() throws EngineException, IOException {
        log.info(">>> testContentTypeDetection <<<");
        ContentItem ci = createContentItem("test.pdf", OCTET_STREAM.toString());
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<div class=\"page\">",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    /**
     * Tests that text is not processed
     */
    @Test
    public void testText() throws EngineException, IOException {
        log.info(">>> testText <<<");
        String text = "The Stanbol enhancer can detect famous cities such as " +
        		"Paris and people such as Bob Marley.";
        ContentItem ci = ciFactory.createContentItem(new StringSource(text));
        Assert.assertEquals(1, ContentItemHelper.getContentParts(ci, Blob.class).size());
    }
    @Test
    public void testUnsupported() throws EngineException, IOException {
        log.info(">>> testUnsupported <<<");
        ContentItem ci = createContentItem("test.pages", "application/x-iwork-pages-sffpages");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        //it MUST NOT give an error but also not add a content part
        assertNull(contentPart);
        //only the original content
        assertEquals(1, ContentItemHelper.getContentParts(ci, Blob.class).size());
    }
    @Test
    public void testXhtml() throws EngineException, IOException {
        log.info(">>> testXhtml <<<");
        ContentItem ci = createContentItem("test.xhtml", XHTML.toString()+"; charset=UTF-8");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities");
        //only the original and the plain text
        // this asserts that no xhtml is parsed from the parsed xhtml content
        assertEquals(2, ContentItemHelper.getContentParts(ci, Blob.class).size());
    }
    
    private ContentItem createContentItem(String resourceName, String contentType) throws IOException {
        InputStream in = TikaEngineTest.class.getClassLoader().getResourceAsStream(resourceName);
        assertNotNull(in);
        return ciFactory.createContentItem(new StreamSource(in,contentType));
    }
    /**
     * Tests if the parsed regex pattern are contained in any line of the parsed
     * test
     * @throws IOException 
     */
    public void assertContentRegexp(Blob blob, String... regexp) throws IOException {
        Charset charset;
        if(blob.getParameter().containsKey("charset")){
            charset = Charset.forName(blob.getParameter().get("charset"));
        } else {
            charset = Charset.defaultCharset();
        }
        Reader reader = null;
        nextPattern:
        for (String expr : regexp) {
            if(reader != null){
                closeQuietly(reader);
            }
            final Pattern p = Pattern.compile(".*" + expr + ".*");
            reader = new InputStreamReader(blob.getStream(), charset);
            final LineIterator it = new LineIterator(reader);
            while (it.hasNext()) {
                final String line = it.nextLine();
                if (p.matcher(line).matches()) {
                    continue nextPattern;
                }
            }
            fail(this + ": no match for regexp '" + expr + "', content=\n" + 
                    IOUtils.toString(blob.getStream(), charset.toString()));
        }
    }
    @After
    public void unbindServices() {/*nothing to do */}

    @AfterClass
    public static void shutdownServices() {
        engine.deactivate(context);
        engine = null;
    }

    /*
     * Internal helper methods 
     */
    private BlankNodeOrIRI verifyBlankNodeOrIRI(ContentItem ci, IRI property){
        return verifyBlankNodeOrIRI(ci, ci.getUri(), property);
    }
    private static BlankNodeOrIRI verifyBlankNodeOrIRI(ContentItem ci, IRI subject, IRI property){
        Iterator<Triple> it = ci.getMetadata().filter(subject,property, null);
        assertTrue(it.hasNext());
        RDFTerm r = it.next().getObject();
        assertFalse(it.hasNext());
        assertTrue(r instanceof BlankNodeOrIRI);
        return (BlankNodeOrIRI)r;
    }
    private static IRI verifyValue(ContentItem ci, IRI property, IRI value){
        return verifyValue(ci, ci.getUri(), property, value);
    }
    private static IRI verifyValue(ContentItem ci, BlankNodeOrIRI subject, IRI property, IRI value){
        Iterator<Triple> it = ci.getMetadata().filter(subject,property, null);
        assertTrue(it.hasNext());
        RDFTerm r = it.next().getObject();
        assertFalse(it.hasNext());
        assertTrue(r instanceof IRI);
        assertEquals(value,r);
        return (IRI)r;
   }
    private static Literal verifyValue(ContentItem ci, IRI property, IRI dataType, String lexValue) throws ParseException{
        return verifyValue(ci, ci.getUri(), property, dataType, lexValue);
    }
    private static Literal verifyValue(ContentItem ci, BlankNodeOrIRI subject, IRI property, IRI dataType, String lexValue) throws ParseException{
        Iterator<Triple> it = ci.getMetadata().filter(subject,property, null);
        assertTrue(it.hasNext());
        RDFTerm r = it.next().getObject();
        assertFalse(it.hasNext());
        if(dataType != null){
            assertEquals(dataType, ((Literal)r).getDataType());
        }
        //if we check dates and the lexical value is not UTC than we need to
        //consider the time zone of the host running this test
        if(XSD.dateTime.equals(dataType) && lexValue.charAt(lexValue.length()-1) != 'Z'){
            Date expectedDate = dateDefaultTimezone.parse(lexValue);
            assertEquals(expectedDate, lf.createObject(Date.class, ((Literal)r)));
        } else {
            assertEquals(lexValue,((Literal)r).getLexicalForm());
        }
        return (Literal)r;
    }
    private static Set<Literal> verifyValues(ContentItem ci, IRI property, IRI dataType, String...lexValues){
        return verifyValues(ci, ci.getUri(), property, dataType, lexValues);
    }
    private static Set<Literal> verifyValues(ContentItem ci, BlankNodeOrIRI subject, IRI property, IRI dataType, String...lexValues){
        Iterator<Triple> it = ci.getMetadata().filter(subject,property, null);
        assertTrue(it.hasNext());
        Set<String> expected = new HashSet<String>(Arrays.asList(lexValues));
        Set<Literal> found = new HashSet<Literal>(expected.size());
        while(it.hasNext()){
            RDFTerm r = it.next().getObject();
            if(dataType == null){
                assertTrue(r instanceof Literal);
            } else {
                assertTrue(r instanceof Literal);
                assertEquals(dataType, ((Literal)r).getDataType());
            }
            assertTrue(expected.remove(((Literal)r).getLexicalForm()));
            found.add((Literal)r);
        }
        return found;
    }
    private static Set<BlankNodeOrIRI> verifyValues(ContentItem ci, BlankNodeOrIRI subject, IRI property, BlankNodeOrIRI...references){
        Iterator<Triple> it = ci.getMetadata().filter(subject,property, null);
        assertTrue(it.hasNext());
        Set<BlankNodeOrIRI> expected = new HashSet<BlankNodeOrIRI>(Arrays.asList(references));
        Set<BlankNodeOrIRI> found = new HashSet<BlankNodeOrIRI>(expected.size());
        while(it.hasNext()){
            RDFTerm r = it.next().getObject();
            assertTrue(r instanceof BlankNodeOrIRI);
            assertTrue(expected.remove(r));
            found.add((BlankNodeOrIRI)r);
        }
        return found;
    }

}
