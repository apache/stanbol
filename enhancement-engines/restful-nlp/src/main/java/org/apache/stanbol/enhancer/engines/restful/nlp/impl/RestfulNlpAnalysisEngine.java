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
package org.apache.stanbol.enhancer.engines.restful.nlp.impl;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.NER_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.SENTIMENT_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.stanbol.enhancer.nlp.json.AnalyzedTextParser;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.apache.stanbol.enhancer.nlp.utils.NIFHelper;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An enhancement engine that uses a RESTful service for NLP processing of
 * the pain text content part of processed {@link ContentItem}s.<p>
 * The RESTful API of the remote service is standardised by  
 * <a href="https://issues.apache.org/jira/browse/STANBOL-892">STANBOL-892</a> <p>
 * 
 * @author Rupert Westenthaler
 */

@Component(immediate = true, metatype = true,  
    policy = ConfigurationPolicy.REQUIRE, configurationFactory=true)
@Service
@Properties(value={
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="changeme"),
        @Property(name=RestfulNlpAnalysisEngine.CONFIG_LANGUAGES, value = {"*"},cardinality=Integer.MAX_VALUE),
        @Property(name=RestfulNlpAnalysisEngine.ANALYSIS_SERVICE_URL, value ="http://changeme"),
        @Property(name=RestfulNlpAnalysisEngine.ANALYSIS_SERVICE_USER, value =""),
        @Property(name=RestfulNlpAnalysisEngine.ANALYSIS_SERVICE_PWD, value =""),
        @Property(name=RestfulNlpAnalysisEngine.WRITE_TEXT_ANNOTATIONS_STATE, 
            boolValue=RestfulNlpAnalysisEngine.DEFAULT_WRITE_TEXT_ANNOTATION_STATE),
        @Property(name=Constants.SERVICE_RANKING,intValue=0)
})
public class RestfulNlpAnalysisEngine extends AbstractEnhancementEngine<IOException,RuntimeException> implements ServiceProperties {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    /**
     * The URI for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_URL = "enhancer.engine.restful.nlp.analysis.service";
    /**
     * The User for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_USER = "enhancer.engine.restful.nlp.analysis.service.user";
    /**
     * The User for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_PWD = "enhancer.engine.restful.nlp.analysis.service.pwd";
    /**
     * Allows to enable/disable the addition of <code>fise:TextAnnotation</code>s
     * to the enhancement metadata of the ContentItem
     */
    public static final String WRITE_TEXT_ANNOTATIONS_STATE = "enhancer.engine.restful.nlp.analysis.write-textannotations";
    public static final boolean DEFAULT_WRITE_TEXT_ANNOTATION_STATE = true;  
    /**
     * Language configuration. Takes a list of ISO language codes to be processed
     * by this engine. This list will be joined with the list of languages supported
     * by the RESTful NLP analysis service.
     */
    public static final String CONFIG_LANGUAGES = "enhancer.engine.restful.nlp.languages";

    /**
     * The maximum size of the preix/suffix for the selection context
     */
    private static final int DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE = 50;


	//TODO: move those sentiment specific constants to o.a.s.enhancer.servicesapi as soon as
	//      Sentiment Annotations are normalized.
	//      NOTE: they are also define in the Sentiment Summarization engine!
    /**
     * The property used to write the sum of all positive classified words
     */
    public static final IRI POSITIVE_SENTIMENT_PROPERTY = new IRI(NamespaceEnum.fise+"positive-sentiment");
    /**
     * The property used to write the sum of all negative classified words
     */
    public static final IRI NEGATIVE_SENTIMENT_PROPERTY = new IRI(NamespaceEnum.fise+"negative-sentiment");
    /**
     * The sentiment of the section (sum of positive and negative classifications)
     */
    public static final IRI SENTIMENT_PROPERTY = new IRI(NamespaceEnum.fise+"sentiment");
    /**
     * The dc:type value used for fise:TextAnnotations indicating a Sentiment
     */
    public static final IRI SENTIMENT_TYPE = new IRI(NamespaceEnum.fise+"Sentiment");
    /**
     * The dc:Type value sued for the sentiment annotation of the whole document
     */
    public static final IRI DOCUMENT_SENTIMENT_TYPE = new IRI(NamespaceEnum.fise+"DocumentSentiment");

    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        //by default register as Tokenizing engine
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_TOKENIZING);
//        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
//            NlpProcessingRole.Tokenizing);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }


    private static Logger log = LoggerFactory.getLogger(RestfulNlpAnalysisEngine.class);

    private URI analysisServiceUrl;
    
    //Langauge configuration
    private LanguageConfiguration languageConfig = new LanguageConfiguration(CONFIG_LANGUAGES,new String[]{"*"});
    
    private final Set<String> supportedLanguages = new HashSet<String>();
    
    protected DefaultHttpClient httpClient;
    private BasicHttpParams httpParams;
    private PoolingClientConnectionManager connectionManager;
    
    @Reference
    private AnalysedTextFactory analysedTextFactory;
    
    /**
     * Used to parse {@link AnalysedText} instances from responses of the
     * RESTful analysis service.
     */
    @Reference
    private AnalyzedTextParser analyzedTextParser;

    private boolean writeTextAnnotations;

    private Boolean serviceInitialised;

    private Dictionary<String, Object> config;
    
    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager} can force sync/async mode if desired, it is
     * just a suggestion from the engine.
     * <p/>
     * Returns ENHANCE_ASYNC in case there is a text/plain content part and a tagger for the language identified for
     * the content item, CANNOT_ENHANCE otherwise.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the introspecting process of the content item
     *          fails
     */
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        Map.Entry<IRI,Blob> entry = NlpEngineHelper.getPlainText(this, ci, false);
        if(entry == null || entry.getValue() == null) {
            return CANNOT_ENHANCE;
        }
        checkRESTfulNlpAnalysisService();
        String language = getLanguage(this,ci,false);
        if(language == null) {
            return CANNOT_ENHANCE;
        }
        if(!languageConfig.isLanguage(language)){
            log.trace(" > can NOT enhance ContentItem {} because language {} is "
                + "not enabled by this engines configuration",ci,language);
            return CANNOT_ENHANCE;
        }
        if(!supportedLanguages.contains(language)){
            log.trace(" > the RESTful Analysis service does not support '{}' (supported: {})",
                language, supportedLanguages);
            return CANNOT_ENHANCE;
        }
        log.trace(" > can enhance ContentItem {} with language {}",ci,language);
        return ENHANCE_ASYNC;
    }


    /**
     * Compute enhancements for supplied ContentItem. The results of the process
     * are expected to be stored in the metadata of the content item.
     * <p/>
     * The client (usually an {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager}) should take care of
     * persistent storage of the enhanced {@link org.apache.stanbol.enhancer.servicesapi.ContentItem}.
     * <p/>
     * This method creates a new POSContentPart using {@link org.apache.stanbol.enhancer.engines.pos.api.POSTaggerHelper#createContentPart} from a text/plain part and
     * stores it as a new part in the content item. The metadata is not changed.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the underlying process failed to work as
     *          expected
     */
    @Override
    public void computeEnhancements(final ContentItem ci) throws EngineException {
        checkRESTfulNlpAnalysisService(); //validate that the service is active
        //get/create the AnalysedText
        final AnalysedText at = NlpEngineHelper.initAnalysedText(this, analysedTextFactory, ci);
        final Blob blob = at.getBlob();
        //send the text to the server
        final String language = getLanguage(this, ci, true);
        final HttpPost request = new HttpPost(analysisServiceUrl);
        request.addHeader(HttpHeaders.CONTENT_LANGUAGE, language);
        request.setEntity(new InputStreamEntity(
            blob.getStream(), blob.getContentLength(),
            ContentType.create(blob.getMimeType(), 
                blob.getParameter().get("charset"))));
        //execute the request
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<AnalysedText>() {
                public AnalysedText run() throws ClientProtocolException, IOException {
                    return httpClient.execute(request, new AnalysisResponseHandler(at));
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof ClientProtocolException) {
                //force re-initialisation upon error
                setRESTfulNlpAnalysisServiceUnavailable();
                throw new EngineException(this, ci, "Exception while executing Request "
                    + "on RESTful NLP Analysis Service at "+analysisServiceUrl, e);
            } else if(e instanceof IOException) {
                //force re-initialisation upon error
                setRESTfulNlpAnalysisServiceUnavailable();
                throw new EngineException(this, ci, "Exception while executing Request "
                        + "on RESTful NLP Analysis Service at "+analysisServiceUrl, e);
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        if(writeTextAnnotations){
			//if enabled fise:TextAnnotations are created for Named Entities and Sentiments
			
            double positiveSent = 0.0;
            int positiveCount = 0;
            double negativeSent = 0.0;
            int negativeCount = 0;
            int sentimentCount = 0;

            Iterator<Span> spans = at.getEnclosed(EnumSet.of(SpanTypeEnum.Sentence,SpanTypeEnum.Chunk));
            Sentence context = null;
            Graph metadata = ci.getMetadata();
            Language lang = new Language(language);
            LiteralFactory lf = LiteralFactory.getInstance();
            ci.getLock().writeLock().lock();
            try { //write TextAnnotations for Named Entities
                while(spans.hasNext()){
                    Span span = spans.next();
                    switch (span.getType()) {
                        case Sentence:
                            context = (Sentence)span;
                            //FALLThrough intended!!
                        default:
                            Value<NerTag> nerAnno = span.getAnnotation(NER_ANNOTATION);
                            if(nerAnno != null){
                                IRI ta = EnhancementEngineHelper.createTextEnhancement(ci, this);
                                //add span related data
                                metadata.add(new TripleImpl(ta, ENHANCER_SELECTED_TEXT, 
                                    new PlainLiteralImpl(span.getSpan(), lang)));
                                metadata.add(new TripleImpl(ta, ENHANCER_START, 
                                    lf.createTypedLiteral(span.getStart())));
                                metadata.add(new TripleImpl(ta, ENHANCER_END, 
                                    lf.createTypedLiteral(span.getEnd())));
                                metadata.add(new TripleImpl(ta, ENHANCER_SELECTION_CONTEXT, 
                                    new PlainLiteralImpl(context == null ?
                                            getDefaultSelectionContext(at.getSpan(), span.getSpan(), span.getStart()) :
                                                context.getSpan(), lang)));
                                //add the NER type
                                if(nerAnno.value().getType() != null){
                                    metadata.add(new TripleImpl(ta,DC_TYPE,nerAnno.value().getType()));
                                }
                                if(nerAnno.probability() >= 0) {
                                    metadata.add(new TripleImpl(ta, ENHANCER_CONFIDENCE, 
                                        lf.createTypedLiteral(nerAnno.probability())));
                                }
                            }

                            Value<Double> sentimentAnnotation = span.getAnnotation(SENTIMENT_ANNOTATION);
                            if (sentimentAnnotation != null) { //this span has a sentiment assigned

                                Double sentiment = sentimentAnnotation.value();

								//Create a fise:TextAnnotation for the sentiment
                                IRI ta = EnhancementEngineHelper.createTextEnhancement(ci, this);
                                metadata.add(new TripleImpl(ta, ENHANCER_START,
                                        lf.createTypedLiteral(span.getStart())));
                                metadata.add(new TripleImpl(ta, ENHANCER_END,
                                        lf.createTypedLiteral(span.getEnd())));
                                metadata.add(new TripleImpl(ta, SENTIMENT_PROPERTY,
                                        lf.createTypedLiteral(sentiment)));

                                //add the generic dc:type used for all Sentiment annotation
                                metadata.add(new TripleImpl(ta, DC_TYPE, SENTIMENT_TYPE));
								//determine the specific dc:type for the sentiment annotation
                                IRI ssoType = NIFHelper.SPAN_TYPE_TO_SSO_TYPE.get(span.getType());
                                if(ssoType != null){
                                    metadata.add(new TripleImpl(ta, DC_TYPE, ssoType));
                                }

                                //keep statistics for the overall sentiment for the Document
                                sentimentCount++ ;
                                if(sentiment > 0){
                                    positiveSent += sentiment;
                                    positiveCount++;
                                }else if(sentiment < 0){
                                    negativeSent += sentiment;
                                    negativeCount++;
                                }

                            }
                            break;
                    }
                }


                //Add the annotation for the overall sentiment of the document 
                if ( sentimentCount > 0 ) {
                IRI ta = EnhancementEngineHelper.createTextEnhancement(ci, this);
                    //calculate the average sentiment for a document
                    //TODO: Think on a better way to calculate a general sentiment value for a document.
                    metadata.add(new TripleImpl(ta, SENTIMENT_PROPERTY,
                            lf.createTypedLiteral((positiveSent + negativeSent) / sentimentCount)));

                    if ( positiveCount > 0 ){
                        //average positive sentiment calculation for the document
                        metadata.add(new TripleImpl(ta, POSITIVE_SENTIMENT_PROPERTY,
                                lf.createTypedLiteral( positiveSent / positiveCount )));
                    }
                    if ( negativeCount > 0 ){
                        //average negative sentiment calculation for the document
                        metadata.add(new TripleImpl(ta, NEGATIVE_SENTIMENT_PROPERTY,
                                lf.createTypedLiteral( negativeSent / negativeCount )));
                    }
                    metadata.add(new TripleImpl(ta, DC_TYPE, SENTIMENT_TYPE));
                    metadata.add(new TripleImpl(ta, DC_TYPE, DOCUMENT_SENTIMENT_TYPE));
                } // no sentiment annotation present ... nothing to do

            } finally {
                ci.getLock().writeLock().unlock();
            }
        } //else do not write fise:TextAnnotations
    }

    protected class AnalysisResponseHandler implements ResponseHandler<AnalysedText>{
        
        protected final AnalysedText at;


        protected AnalysisResponseHandler(AnalysedText at){
            this.at = at;
        }

        @Override
        public AnalysedText handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            StatusLine statusLine = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (statusLine.getStatusCode() >= 300) {
                String reason;
                if(entity != null) {
                    StringBuilder sb = new StringBuilder(statusLine.getReasonPhrase());
                    String message = EntityUtils.toString(entity);
                    if(message != null && !message.isEmpty()){
                        sb.append("\nMessage:\n").append(message);
                    }
                    reason = sb.toString();
                } else {
                    reason = statusLine.getReasonPhrase();
                }
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(), reason);
            }
            //parse the results
            InputStream in = null;
            try {
                in = entity.getContent();
                Charset charset = entity.getContentEncoding() != null ? 
                        Charset.forName(entity.getContentEncoding().getValue()) : UTF8;
                return analyzedTextParser.parse(in, charset, at);
            } finally {
                //ensure that the stream is closed
                IOUtils.closeQuietly(in);
            }
        }
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
    /**
     * Activate and read the properties. Configures and initialises a POSTagger for each language configured in
     * CONFIG_LANGUAGES.
     *
     * @param ce the {@link org.osgi.service.component.ComponentContext}
     */
    @Activate
    protected void activate(ComponentContext ce) throws ConfigurationException, IOException {
        super.activate(ce);
        log.info("activate {} '{}'",getClass().getSimpleName(),getName());
        config = ce.getProperties();

        Object value = config.get(ANALYSIS_SERVICE_URL);
        if(value == null){
            throw new ConfigurationException(ANALYSIS_SERVICE_URL, 
                "The RESTful Analysis Service URL is missing in the provided configuration!");
        } else {
            try {
                analysisServiceUrl = new URI(value.toString());
                log.info("  ... service: {}",analysisServiceUrl);
            } catch (URISyntaxException e) {
                throw new ConfigurationException(ANALYSIS_SERVICE_URL, 
                        "The parsed RESTful Analysis Service URL '"+ value
                        + "'is not a valid URL!",e);
            }
        }
        String usr;
        String pwd;
        value = config.get(ANALYSIS_SERVICE_USER);
        if(value != null && !value.toString().isEmpty()){
            usr = value.toString();
            value = config.get(ANALYSIS_SERVICE_PWD);
            pwd = value == null ? null : value.toString();
        } else { // no user set
            usr = null;
            pwd = null;
        }
        
        //init the http client
        httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.USER_AGENT, "Apache Stanbol RESTful NLP Analysis Engine");
        httpParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
        httpParams.setIntParameter(ClientPNames.MAX_REDIRECTS, 3);
        httpParams.setBooleanParameter(CoreConnectionPNames.SO_KEEPALIVE, true);

        connectionManager = new PoolingClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(20);

        //NOTE: The list of supported languages is the combination of the
        //      languages enabled by the configuration (#languageConfig) and the
        //      languages supported by the RESTful NLP Analysis Service 
        //      (#supportedLanguages)
        //init the language configuration with the engine configuration
        languageConfig.setConfiguration(config);
        
        httpClient = new DefaultHttpClient(connectionManager,httpParams);
        if(usr != null){
            log.info("  ... setting user to {}",usr);
            httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(usr, pwd));
            // And add request interceptor to have preemptive authentication
            httpClient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
        }
        //STANBOL-1389: deactivated initialization during activation as this can create
        //issues in cases where Stanbol and the NLP service do run in the same
        //servlet container.
        //initRESTfulNlpAnalysisService();
        
        value = config.get(WRITE_TEXT_ANNOTATIONS_STATE);
        if(value instanceof Boolean){
            this.writeTextAnnotations = ((Boolean)value).booleanValue();
        } else if(value != null){
            this.writeTextAnnotations = Boolean.parseBoolean(value.toString());
        } else {
            this.writeTextAnnotations = DEFAULT_WRITE_TEXT_ANNOTATION_STATE;
        }
    }
    /**
     * @throws EngineException
     */
    private void checkRESTfulNlpAnalysisService() throws EngineException {
        if(!initRESTfulNlpAnalysisService()){
            throw new EngineException("The configured RESTful NLP Analysis Service is "
                + "currently not available (url: '"+analysisServiceUrl+"')");
        }
    }
    /**
     * to be called after handling an exception while calling the remote service
     * that indicates that the service is no longer available.
     */
    private void setRESTfulNlpAnalysisServiceUnavailable(){
        serviceInitialised = false;
        supportedLanguages.clear();
    }
    
    /**
     * initialises the RESRfulNlpAnalysis if not yet done.
     */
    private boolean initRESTfulNlpAnalysisService() {
        if(serviceInitialised != null && serviceInitialised){
            return true; //already initialised
        }
        if(serviceInitialised == null){
            log.info(" ... checking configured RESTful NLP Analysis service {}", analysisServiceUrl);
            serviceInitialised = false;
        } else {
            log.info(" ... re-trying to initialise RESTful NLP Analysis service {}", analysisServiceUrl);
        }
        //get the supported languages
        String supported;
        try {
            supported = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                public String run() throws IOException {
                    HttpGet request = new HttpGet(analysisServiceUrl);
                    request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
                    return httpClient.execute(request,new BasicResponseHandler());
                }
            });
            serviceInitialised = true;
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            setRESTfulNlpAnalysisServiceUnavailable();
            if(e instanceof IOException){
                log.warn("Unable to initialise RESTful NLP Analysis Service!", e);
                return false;
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        //NOTE: The list of supported languages is the combination of the
        //      languages enabled by the configuration (#languageConfig) and the
        //      languages supported by the RESTful NLP Analysis Service 
        //      (#supportedLanguages)
        //parse the supported languages from the initialization response
        StringTokenizer st = new StringTokenizer(supported, "{[\",]}");
        while(st.hasMoreElements()){
            supportedLanguages.add(st.nextToken());
        }
        return true;
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        languageConfig.setDefault();
        supportedLanguages.clear();
        //shutdown the Http Client
        httpClient = null;
        httpParams = null;
        connectionManager.shutdown();
        connectionManager = null;
        serviceInitialised = null;
        super.deactivate(context);
    }
    
    /**
     * Extracts the selection context based on the content, selection and
     * the start char offset of the selection
     * @param content the content
     * @param selection the selected text
     * @param selectionStartPos the start char position of the selection
     * @return the context
     */
    private String getDefaultSelectionContext(String content, String selection,int selectionStartPos){
        //extract the selection context
        int beginPos;
        if(selectionStartPos <= DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE){
            beginPos = 0;
        } else {
            int start = selectionStartPos-DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            beginPos = content.indexOf(' ',start);
            if(beginPos < 0 || beginPos >= selectionStartPos){ //no words
                beginPos = start; //begin within a word
            }
        }
        int endPos;
        if(selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE >= content.length()){
            endPos = content.length();
        } else {
            int start = selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            endPos = content.lastIndexOf(' ', start);
            if(endPos <= selectionStartPos+selection.length()){
                endPos = start; //end within a word;
            }
        }
        return content.substring(beginPos, endPos);
    }
    
    /**
     * HttpRequestInterceptor for preemptive authentication, based on httpclient
     * 4.0 example
     */
    private static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {

        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
            HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

            // If not auth scheme has been initialized yet
            if (authState.getAuthScheme() == null) {
                AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());

                // Obtain credentials matching the target host
                Credentials creds = credsProvider.getCredentials(authScope);

                // If found, generate BasicScheme preemptively
                if (creds != null) {
                    authState.update(new BasicScheme(), creds);
                }
            }
        }
    }
}
