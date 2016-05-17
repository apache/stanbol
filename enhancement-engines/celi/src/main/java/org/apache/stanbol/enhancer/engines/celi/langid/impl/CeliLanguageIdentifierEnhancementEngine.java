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
package org.apache.stanbol.enhancer.engines.celi.langid.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.DCTERMS_LINGUISTIC_SYSTEM;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.soap.SOAPException;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(immediate = true, metatype = true)
@Service
@Properties(value = { 
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "celiLangid"),
    @Property(name = CeliConstants.CELI_LICENSE),
    @Property(name = CeliConstants.CELI_TEST_ACCOUNT,boolValue=false),
    @Property(name = CeliConstants.CELI_CONNECTION_TIMEOUT, intValue=CeliConstants.DEFAULT_CONECTION_TIMEOUT) 
})
public class CeliLanguageIdentifierEnhancementEngine extends AbstractEnhancementEngine<IOException, RuntimeException> implements EnhancementEngine, ServiceProperties {
	/**
	 * This ensures that no connections to external services are made if Stanbol is started in offline mode 
	 * as the OnlineMode service will only be available if OfflineMode is deactivated. 
	 */
	@SuppressWarnings("unused")
    @Reference
    private OnlineMode onlineMode; 
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}-2 to ensure that it is
     * executed before "normal" pre-processing engines.<p>
     * NOTE: this information is used by the default and weighed {@link Chain}
     * implementation to determine the processing order of 
     * {@link EnhancementEngine}s. Other {@link Chain} implementation do not
     * use this information.
     */
	public static final Integer defaultOrder = ServiceProperties.ORDERING_PRE_PROCESSING -2;

	private Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * This contains the only MIME type directly supported by this enhancement
	 * engine.
	 */
	private static final String TEXT_PLAIN_MIMETYPE = "text/plain";

	/**
	 * Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
	 */
	private static final Set<String> SUPPORTED_MIMTYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);
	/**
	 * The literal factory
	 */
    private final LiteralFactory literalFactory = LiteralFactory.getInstance();


	@Property(value = "http://linguagrid.org/LSGrid/ws/language-identifier")
	public static final String SERVICE_URL = "org.apache.stanbol.enhancer.engines.celi.langid.url";

	private String licenseKey;
	private URL serviceURL;

	private LanguageIdentifierClientHTTP client;

	@Override
	@Activate
	public void activate(ComponentContext ctx) throws IOException, ConfigurationException {
		super.activate(ctx);
		Dictionary<String, Object> properties = ctx.getProperties();
        this.licenseKey = Utils.getLicenseKey(properties,ctx.getBundleContext());
		String url = (String) properties.get(SERVICE_URL);
		if (url == null || url.isEmpty()) {
			throw new ConfigurationException(SERVICE_URL, 
			    String.format("%s : please configure the URL of the CELI Web "
			            + "Service (e.g. by" + "using the 'Configuration' tab of "
			            +"the Apache Felix Web Console).", 
			            getClass().getSimpleName()));
		}
		try {
		    this.serviceURL = new URL(url);
		} catch (MalformedURLException e) {
            throw new ConfigurationException(SERVICE_URL, 
                String.format("%s : The URL of the CELI Web Service is not well formatted.", 
                    getClass().getSimpleName()),e);
        }
		int conTimeout = Utils.getConnectionTimeout(properties, ctx.getBundleContext());
		this.client = new LanguageIdentifierClientHTTP(this.serviceURL, this.licenseKey, conTimeout);
	}
	
	@Override
	@Deactivate
	protected void deactivate(ComponentContext ce) {
		super.deactivate(ce);
	}

	@Override
	public int canEnhance(ContentItem ci) throws EngineException {
		if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES) != null) {
			return ENHANCE_ASYNC;
		} else {
			return CANNOT_ENHANCE;
		}
	}
	
	@Override
	public void computeEnhancements(ContentItem ci) throws EngineException {
		Entry<IRI, Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
		if (contentPart == null) {
			throw new IllegalStateException("No ContentPart with Mimetype '" + TEXT_PLAIN_MIMETYPE + "' found for ContentItem " + ci.getUri() + ": This is also checked in the canEnhance method! -> This "
					+ "indicated an Bug in the implementation of the " + "EnhancementJobManager!");
		}
		String text = "";
		try {
			text = ContentItemHelper.getText(contentPart.getValue());
		} catch (IOException e) {
			throw new InvalidContentException(this, ci, e);
		}
		if (text.trim().length() == 0) {
			log.info("No text contained in ContentPart {"+contentPart.getKey()+"} of ContentItem {"+ci.getUri()+"}");
			return;
		}
		
		try {
			
			String[] tmps=text.split(" ");
			List<GuessedLanguage> lista = null;
			if(tmps.length>5)
				lista = this.client.guessLanguage(text);
			else 
				lista = this.client.guessQueryLanguage(text);
			
			Graph g = ci.getMetadata();
			//in ENHANCE_ASYNC we need to use read/write locks on the ContentItem
			ci.getLock().writeLock().lock();
			try {
    			GuessedLanguage gl = lista.get(0);
    			IRI textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
    		    g.add(new TripleImpl(textEnhancement, DC_LANGUAGE, new PlainLiteralImpl(gl.getLang())));
    			g.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(gl.getConfidence())));
				g.add(new TripleImpl(textEnhancement, DC_TYPE, DCTERMS_LINGUISTIC_SYSTEM));
			} finally {
			    ci.getLock().writeLock().unlock();
			}
			
		} catch (IOException e) {
		    throw new EngineException("Error while calling the CELI language"
		        +" identifier service (configured URL: "
		        +serviceURL+")!",e);
        } catch (SOAPException e) {
            throw new EngineException("Error wile encoding/decoding the request/"
                +"response to the CELI language identifier service!",e);
        } 

	}
	
	
	@Override
	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}


}