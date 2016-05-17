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
package org.apache.stanbol.enhancer.it;

import static org.apache.stanbol.enhancer.it.MultipartContentItemTestUtils.buildPathWithParams;
import static org.apache.stanbol.enhancer.it.MultipartContentItemTestUtils.getHTMLContent;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.ContentDescriptor;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests RESTful API extensions to the Stanbol Enhancer as described by
 * STANBOL-481
 */
public class MultipartRequestTest extends EnhancerTestBase {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static Serializer serializer = Serializer.getInstance();
    
    private static final Logger log = LoggerFactory.getLogger(MultipartRequestTest.class);

    private final String TEXT_CONTENT = "The Apache Stanbol Enhancer.\n" +
    		"The Stanbol enhancer can detect famous cities such as Paris and " +
    		"people such as Bob Marley.";
    private final String[] TEXT_CONTENT_LINES = TEXT_CONTENT.split("\n");
    private final String HTML_CONTENT = getHTMLContent(TEXT_CONTENT_LINES);
    private final String[] HTML_CONTENT_LINES = HTML_CONTENT.split("\n");
    public MultipartRequestTest() {
        super(); //use the default endpoint
    }

    @Test
    public void testIllegalRdfFormat() throws IOException {
        String[] params = new String []{
                    "outputContent","*/*",
                    "rdfFormat","notAvalidMimeFormat"};
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(400); //BAD_REQUEST
    }
    @Test
    public void testIllegalOutputContent() throws IOException {
        String[] params = new String []{
                    "outputContent","notAvalidMimeFormat"};
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(400); //BAD_REQUEST
    }
    @Test
    public void testDefaultContentTypes() throws IOException {
        //'*/*', 'text/plain' and 'application/octet-stream' where considered as
        // Indicators that the default RDF serialisation format for the metadata
        // should be used. 
        //This is basically testing backward compatibility 
        
        String[] jsonLDMetadataTests = new String[]{
            "\"@id\" : \"http://dbpedia.org/resource/Paris\",",
            "\"@id\" : \"http://dbpedia.org/resource/Bob_Marley\",",
            "\"http://purl.org/dc/terms/creator\" : [ {",
            "\"@value\" : \"org.apache.stanbol.enhancer.engines.entitytagging.impl.NamedEntityTaggingEngine\""
        };
        String[] params = new String []{
                    "outputContent","text/plain"
        };
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","text/plain") //must be multipart/from-data
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200) //metadata as JSONLD
        .assertContentContains(jsonLDMetadataTests);
        
        params = new String []{
            "outputContent","application/octet-stream"}; //omitMetadata=false
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","text/plain") //must be multipart/from-data
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200) //metadata as JSONLD
        .assertContentContains(jsonLDMetadataTests);

        params = new String []{
           "outputContent","application/octet-stream"}; //omitMetadata=false
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","text/plain") //must be multipart/from-data
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200) //metadata as JSONLD
        .assertContentContains(jsonLDMetadataTests);
    }
    
    @Test
    public void testOutputMetadataAndAllContent() throws IOException {
        String[] params = new String []{
                    "outputContent","*/*",
                    "rdfFormat","text/rdf+nt"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
            "--contentItem",
            "Content-Disposition: form-data; name=\"content\"",
            "Content-Type: multipart/alternate; boundary=contentParts-",
            "Content-Type: text/plain; charset=UTF-8",
            "Content-Type: text/html",
            "--contentParts")
        .assertContentContains(TEXT_CONTENT_LINES)
        .assertContentContains(HTML_CONTENT_LINES) //line by line the HTML content
        .assertContentRegexp(
            "Content-Disposition: form-data; name=\"metadata\"; filename=.*",
            "Content-Disposition: form-data; name=\"urn:tika:text:.*",
            "Content-Disposition: form-data; name=\"urn:content-item-sha1-.*",
            "--contentItem-.*--",
            "--contentParts-.*--",
            //and the expected enhancements in the metadata
            "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*NamedEntityExtractionEnhancementEngine",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley")
        .getContent();
        log.debug("Content:\n{}\n",content);

    }
    @Test
    public void testOutputAllContentOmitMetadata() throws IOException {
        String[] params = new String []{
                    "outputContent","*/*",
                    "omitMetadata","true",
                    "rdfFormat","text/rdf+nt"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
            "--contentItem",
            "Content-Disposition: form-data; name=\"content\"",
            "Content-Type: multipart/alternate; boundary=contentParts-",
            "Content-Type: text/plain; charset=UTF-8",
            "Content-Type: text/html",
            "--contentParts")
        .assertContentContains(TEXT_CONTENT_LINES)
        .assertContentContains(HTML_CONTENT_LINES) //line by line the HTML content
        .assertContentRegexp( //MUST contain
            "--contentItem-.*--",
            "--contentParts-.*--",
            "Content-Disposition: form-data; name=\"urn:tika:text:.*",
            "Content-Disposition: form-data; name=\"urn:content-item-sha1-.*")
        .assertContentRegexp(false, //MUST NOT contain
            "Content-Disposition: form-data; name=\"metadata\"; filename=.*",
            //and the expected enhancements in the metadata
            "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*NamedEntityExtractionEnhancementEngine",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley")
        .getContent();
        log.debug("Content:\n{}\n",content);

    }

    @Test
    public void testOutputPlainTextContent() throws IOException {
        String[] params = new String []{
                    "outputContent","text/plain",
                    "rdfFormat","text/rdf+nt"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
            "--contentItem",
            "Content-Disposition: form-data; name=\"content\"",
            "Content-Type: multipart/alternate; boundary=contentParts-",
            "Content-Type: text/plain; charset=UTF-8",
            "--contentParts")
        .assertContentContains(TEXT_CONTENT_LINES)
        .assertContentRegexp(
            "--contentItem-.*--",
            "--contentParts-.*--",
            "Content-Disposition: form-data; name=\"metadata\"; filename=.*",
            "Content-Disposition: form-data; name=\"urn:tika:text:.*",
            //and the expected enhancements in the metadata
            "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*NamedEntityExtractionEnhancementEngine",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley")
        .getContent();
        log.debug("Content:\n{}\n",content);

    }
    @Test
    public void testOutputContentOtherThanParsed() throws IOException {
        //metadata and text content
        String[] params = new String []{
                    "outputContent","*/*",
                    "omitParsed","true",
                    "rdfFormat","text/rdf+nt"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
             "--contentItem",
             "Content-Disposition: form-data; name=\"content\"",
             "Content-Type: multipart/alternate; boundary=contentParts-",
             "Content-Type: text/plain; charset=UTF-8",
             "--contentParts")
         .assertContentContains(TEXT_CONTENT_LINES)
         .assertContentRegexp(
             "--contentItem-.*--",
             "--contentParts-.*--",
             "Content-Disposition: form-data; name=\"metadata\"; filename=.*",
             "Content-Disposition: form-data; name=\"urn:tika:text:.*",
             //and the expected enhancements in the metadata
             "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
             "http://purl.org/dc/terms/language.*en",
             "http://fise.iks-project.eu/ontology/entity-label.*Paris",
             "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*NamedEntityExtractionEnhancementEngine",
             "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley")
         .getContent();
        log.debug("Content:\n{}\n",content);
    }
    @Test
    public void testOutputContentPart() throws IOException {
        String[] params = new String []{
                    "outputContentPart","http://stanbol.apache.org/ontology/enhancer/executionmetadata#ChainExecution",
                    "omitMetadata","true",
                    "rdfFormat","application/rdf+xml"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/plain; charset=UTF-8")
            .withContent(TEXT_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
             "--contentItem",
             "Content-Disposition: form-data; name=\"http://stanbol.apache.org/ontology/enhancer/executionmetadata#ChainExecution\"",
             "Content-Type: application/rdf+xml; charset=UTF-8",
             "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionplan#ExecutionPlan\"/>",
             "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionplan#ExecutionNode\"/>",
             "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionmetadata#EngineExecution\"/>",
             "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionmetadata#ChainExecution\"/>")
        .assertContentRegexp("--contentItem-.*--")
        .getContent();
        log.debug("Content:\n{}\n",content);
    }
    /**
     * This uploads the HTML as well as the plain text version of an content.
     * This allows it CMS to parse already available alternate content versions
     * in a single request. Stanbol can than still use the original content
     * (e.g. to extract metadata) but other engines that require the alternate
     * version (e.g. plain text version) of an document will directly use the
     * parsed version .<p>
     * This UnitTest ensures this by adding a "secret" extension the to plain
     * text version and than checks if the two entities mentioned in that
     * part are included in the extracted entities.
     * @throws IOException
     */
    @Test
    public void testUploadMultipleContents() throws IOException {
        //It is a secret, that Berlin is the capital of Germany
        String extraTextConent = TEXT_CONTENT + 
                "\nIt is a secret, that the city of Berlin is the capital of Germany since 1990.";
        //The multipartBuilder used to construct the contentItem for the contentItem
        MultipartEntityBuilder ciBuilder = MultipartEntityBuilder.create();
        String boundary = "contentItem-47jjksnbue73fnis";
        ciBuilder.setBoundary(boundary);
        //use a small extension to deal with multipart/alternate
        Map<String, ContentBody> alternates = new LinkedHashMap<String,ContentBody>();
        alternates.put("http://www.example.com/test.html", 
            new StringBody(HTML_CONTENT, ContentType.TEXT_HTML.withCharset(UTF8)));
        alternates.put("http://www.example.com/test.txt", 
            new StringBody(extraTextConent, ContentType.TEXT_PLAIN.withCharset(UTF8)));
        ciBuilder.addPart("content", 
            new MultipartContentBody(alternates, "contentParts", 
                ContentType.create("multipart/alternate")));
        
        String receivedContent = executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withEntity(ciBuilder.build())
        )
        .assertStatus(200)
        .assertContentRegexp(
            //and the expected enhancements in the metadata
            "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*NamedEntityExtractionEnhancementEngine",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley",
            //check also for expeted entities extracted from the secret Text part!
            "http://fise.iks-project.eu/ontology/entity-label.*Berlin",
            "http://fise.iks-project.eu/ontology/entity-label.*Germany")
        .getContent();
        log.debug("Content:\n{}\n",receivedContent);

    }
    
    
    @Test
    public void testContentBeforeMetadata() throws IOException{
        final IRI contentItemId = new IRI("http://www.example.com/test.html");
        String rdfContentType = SupportedFormat.RDF_XML;
        String rdfContent = getDummyRdfMetadata(contentItemId, rdfContentType);
        MultipartEntityBuilder ciBuilder = MultipartEntityBuilder.create();
        ciBuilder.addTextBody("content",HTML_CONTENT,ContentType.TEXT_HTML.withCharset(UTF8));
        ciBuilder.addTextBody("metadata", rdfContent, ContentType.create(rdfContentType,UTF8));
        String receivedContent = executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withEntity(ciBuilder.build())
        )
        .assertStatus(400) //BAD request
        .getContent();
        //check also the error message
        Assert.assertTrue(receivedContent.contains(
            "The Multipart MIME part with the 'metadata' MUST BE before the " +
            "MIME part containing the 'content'"));
    }
    @Test
    public void testMissingContent() throws IOException{
        final IRI contentItemId = new IRI("http://www.example.com/test.html");
        String rdfContentType = SupportedFormat.RDF_XML;
        String rdfContent = getDummyRdfMetadata(contentItemId, rdfContentType);
        MultipartEntityBuilder ciBuilder = MultipartEntityBuilder.create();
        ciBuilder.addTextBody("metadata", rdfContent, ContentType.create(rdfContentType,UTF8));

        String receivedContent = executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withEntity(ciBuilder.build())
        )
        .assertStatus(400) //BAD request
        .getContent();
        //check also the error message
        Assert.assertTrue(receivedContent.contains(
            "The parsed multipart content item does not contain any content."));
    }

    /**
     * @param contentItemId
     * @param rdfContentType
     * @return
     */
    private String getDummyRdfMetadata(final IRI contentItemId, String rdfContentType) {
        Graph metadata = new SimpleGraph();
        metadata.add(new TripleImpl(new BlankNode(), Properties.ENHANCER_EXTRACTED_FROM, contentItemId));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, metadata, rdfContentType);
        String rdfContent = new String(out.toByteArray(),UTF8);
        return rdfContent;
    }
    
    /**
     * Stanbol also supports to upload pre-existing metadata with the content.
     * This UnitTest uses an example that parsed TextAnnotations for free text
     * tags provided by users that are than linked to Entities in DBPedia
     * @throws IOException
     */
    @Test
    public void testUploadWithMetadata() throws IOException {
        //create the metadata
        RDFTerm user = new PlainLiteralImpl("Rupert Westenthaler");
        final IRI contentItemId = new IRI("http://www.example.com/test.html");
        Graph metadata = new SimpleGraph();
        addTagAsTextAnnotation(metadata, contentItemId, 
            "Germany",DBPEDIA_PLACE, user);
        addTagAsTextAnnotation(metadata, contentItemId, 
            "Europe",DBPEDIA_PLACE, user);
        addTagAsTextAnnotation(metadata, contentItemId, 
            "NATO",DBPEDIA_ORGANISATION, user);
        addTagAsTextAnnotation(metadata, contentItemId, 
            "Silvio Berlusconi",DBPEDIA_PERSON, user);
        
        String rdfContentType = SupportedFormat.RDF_XML;
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, metadata, rdfContentType);
        String rdfContent = new String(out.toByteArray(),UTF8);
        
        MultipartEntityBuilder ciBuilder = MultipartEntityBuilder.create();
        //add the metadata
        /*
         * NOTE: We need here to override the getFilename, because this MUST
         *       BE the URI of the ContentItem. This is important, because the
         *       Metadata do contain triples about that ContentItem and therefore
         *       it MUST BE assured that the URI of the ContentItem created by
         *       the Stanbol Enhancer is the same of as the URI used in the
         *       Metadata!
         */
        ciBuilder.addPart("metadata", 
            new StringBody(rdfContent, ContentType.create(rdfContentType).withCharset(UTF8)){
            @Override
            public String getFilename() { //The filename MUST BE the
                return contentItemId.getUnicodeString(); //uri of the ContentItem
            }
        });
        //add the content
        ciBuilder.addTextBody("content", HTML_CONTENT, ContentType.TEXT_HTML.withCharset(UTF8));        
        //send the request
        String receivedContent = executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withEntity(ciBuilder.build())
        )
        .assertStatus(200)
        .assertContentRegexp(
            //and the expected enhancements based on the parsed content
            "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*NamedEntityExtractionEnhancementEngine",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley",
            //additional enhancements based on parsed metadata
            "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Germany.*",
            "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/NATO.*",
            "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Silvio_Berlusconi.*",
            "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Europe.*")
        .getContent();
        log.debug("Content:\n{}\n",receivedContent);
    }
    /**
     * Utility that creates an {@link TechnicalClasses#ENHANCER_TEXTANNOTATION TextAnnotation}
     * for the parsed contentItem, free text tag an user. 
     * @param graph the grpah to add the information
     * @param contentItem the {@link ContentItem#getUri() uri} of the {@link ContentItem}
     * @param tag the free text tag for the document
     * @param tagType the type of the tag. Typically Stanbol supports: <ul>
     * <li>{@link OntologicalClasses#DBPEDIA_PERSON}
     * <li>{@link OntologicalClasses#DBPEDIA_ORGANISATION}
     * <li>{@link OntologicalClasses#DBPEDIA_PLACE}
     * </ul>
     * But specific {@link EnhancementEngine}s might also process other types
     * or even TextAnnotations without an type
     * @param user the user that created the tag
     * @return the uri of the created annotation
     */
    private static final IRI addTagAsTextAnnotation(Graph graph, IRI contentItem, 
                                                       String tag, IRI tagType, RDFTerm user){
        IRI ta = new IRI("urn:user-annotation:"+EnhancementEngineHelper.randomUUID());
        graph.add(new TripleImpl(ta, RDF.type, TechnicalClasses.ENHANCER_TEXTANNOTATION));
        graph.add(new TripleImpl(ta, Properties.ENHANCER_EXTRACTED_FROM,contentItem));
        if(tagType != null){
            graph.add(new TripleImpl(ta, Properties.DC_TYPE, tagType));
        }
        graph.add(new TripleImpl(ta, Properties.ENHANCER_SELECTED_TEXT, new PlainLiteralImpl(tag)));
        graph.add(new TripleImpl(ta, RDF.type, TechnicalClasses.ENHANCER_ENHANCEMENT));
        if(user != null){
            graph.add(new TripleImpl(ta, Properties.DC_CREATOR,user));
        }
        return ta;
    }    
    /**
     * Supports sending multipart mime as {@link ContentBody}.
     * @author Rupert Westenthaler
     *
     */
    private class MultipartContentBody extends AbstractContentBody implements ContentBody,ContentDescriptor {

        private Map<String,ContentBody> parts;
        private String boundary;

        public MultipartContentBody(Map<String,ContentBody> parts, String boundary, ContentType contentType){
            super(ContentType.create(contentType.getMimeType(), new BasicNameValuePair("boundary",boundary)));
            this.parts = parts;
            this.boundary = boundary;
        }

        @Override
        public String getTransferEncoding() {
            return MIME.ENC_8BIT;
        }

        @Override
        public long getContentLength() {
            //not known as we would need to count the content length AND
            //the length of the different mime headers.
            return -1; 
        }

        @Override
        public String getFilename() {
            return null;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setBoundary(boundary);
            for(Entry<String,ContentBody> part : parts.entrySet()){
                builder.addPart(part.getKey(), part.getValue());
            }
            HttpEntity entity = builder.build();
            entity.writeTo(out);
        }
        
    }

}
