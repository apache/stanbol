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
package org.apache.stanbol.enhancer.engines.uimalocal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.analysis.uima.ae.AEProvider;
import org.apache.lucene.analysis.uima.ae.AEProviderFactory;
import org.apache.stanbol.commons.caslight.Feature;
import org.apache.stanbol.commons.caslight.FeatureStructure;
import org.apache.stanbol.commons.caslight.FeatureStructureListHolder;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeDescription;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mihaly Heder
 */
@Component(immediate = true, metatype = true, inherit = true, label = "UIMA Local Enhancement Engine",
        description = "Runs UIMA Analysis Engine and retuns values.")
@Service
@Properties(value = {
        @Property(name = EnhancementEngine.PROPERTY_NAME, value = "uimalocal")
})
public class UIMALocal extends AbstractEnhancementEngine<RuntimeException, RuntimeException>
        implements EnhancementEngine, ServiceProperties {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Property(value = "sourceName", label = "UIMA source name",
            description = "The name of this UIMA source which will be used for referring internally to the UIMA endpoint")
    public static final String UIMA_SOURCENAME = "stanbol.engine.uimalocal.sourcename";

    @Property(value = "/path/to/descriptor",
            label = "UIMA descriptor file path",
            description = "The file path to the UIMA descriptor XML to load")
    public static final String UIMA_DESCRIPTOR_PATH = "stanbol.engine.uimalocal.descriptorpath";

    @Property(value = "uima.apache.org", label = "Content Part URI reference",
            description = "The URI Reference of the UIMA content part to be created. This content part will "
                    + "contain Annotations from all the resources above.")
    public static final String UIMA_CONTENTPART_URIREF = "stanbol.engine.uimalocal.contentpart.uriref";

    @Property(cardinality = 1000, value = "text/plain", label = "Supported Mime Types",
            description = "Mime Types supported by this client. This should be aligned to the capabilities of the UIMA Endpoints.")
    public static final String UIMA_SUPPORTED_MIMETYPES = "stanbol.engine.uimalocal.contentpart.mimetypes";

    public static final Integer defaultOrder = ServiceProperties.ORDERING_PRE_PROCESSING;

    private AEProvider aeProvider;
    private Set<String> SUPPORTED_MIMETYPES;
    private String uimaUri;
    private String uimaSourceName;
    private String uimaDescriptorPath;
    private List<String> uimaTypeNames;


    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        Dictionary<String, Object> props = ctx.getProperties();

        this.uimaUri = (String) props.get(UIMA_CONTENTPART_URIREF);
        this.uimaSourceName = (String) props.get(UIMA_SOURCENAME);
        this.uimaDescriptorPath = (String) props.get(UIMA_DESCRIPTOR_PATH);

        SUPPORTED_MIMETYPES = Collections.unmodifiableSet(new HashSet<String>(
                Arrays.asList((String[]) props.get(UIMA_SUPPORTED_MIMETYPES))));

        aeProvider = AEProviderFactory.getInstance().getAEProvider(uimaSourceName,
                uimaDescriptorPath, new HashMap<String, Object>());
        try {
            AnalysisEngine ae = aeProvider.getAE();
            TypeDescription[] aeTypes = ae.getAnalysisEngineMetaData().getTypeSystem().getTypes();
            uimaTypeNames = new ArrayList<String>();
            for (TypeDescription aeType : aeTypes) {
                String aeTypeName = aeType.getName();
                logger.info("Configuring Analysis Engine Type:" + aeTypeName);
                uimaTypeNames.add(aeTypeName);
            }
        } catch (ResourceInitializationException ex) {
            logger.error("Cannot retrieve AE from AEProvider. ", ex);
            throw new ConfigurationException(uimaDescriptorPath, "Cannot retreive AE from AEProvider", ex);
        }
    }

    @Override
    protected void deactivate(ComponentContext ctx) {

        super.deactivate(ctx);
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES) != null) {
            return ENHANCE_ASYNC;
        }
        return CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        Entry<IRI, Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
        if (contentPart == null) {
            throw new IllegalStateException("No ContentPart with an supported Mimetype '"
                    + SUPPORTED_MIMETYPES + "' found for ContentItem " + ci.getUri()
                    + ": This is also checked in the canEnhance method! -> This "
                    + "indicated an Bug in the implementation of the "
                    + "EnhancementJobManager!");
        }
        String text;
        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        JCas jcas;
        try {
            logger.info("Processing text with UIMA AE...");
            jcas = processText(text);
        } catch (ResourceInitializationException ex) {
            logger.error("Error initializing UIMA AE", ex);
            throw new EngineException("Error initializing UIMA AE", ex);
        } catch (AnalysisEngineProcessException ex) {
            logger.error("Error running UIMA AE", ex);
            throw new EngineException("Error running UIMA AE", ex);
        }

        //just for being sure
        if (jcas == null) {
            return;
        }

        for (String typeName : uimaTypeNames) {
            List<FeatureStructure> featureSetList = concertToCasLight(jcas, typeName);
            IRI uimaIRI = new IRI(uimaUri);

            FeatureStructureListHolder holder;
            ci.getLock().writeLock().lock();
            try {
                holder = ci.getPart(uimaIRI, FeatureStructureListHolder.class);
            } catch (NoSuchPartException e) {
                holder = new FeatureStructureListHolder();
                logger.info("Adding FeatureSet List Holder content part with uri:" + uimaUri);
                ci.addPart(uimaIRI, holder);
                logger.info(uimaUri + " content part added.");
            } finally {
                ci.getLock().writeLock().unlock();
            }

            ci.getLock().writeLock().lock();
            try {
                holder.addFeatureStructureList(uimaSourceName, featureSetList);
            } finally {
                ci.getLock().writeLock().unlock();
            }
        }

    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING,
                (Object) defaultOrder));
    }

    /*
     * process a field value executing UIMA the CAS containing it as document
     * text - From SOLR.
     */
    private JCas processText(String textFieldValue) throws ResourceInitializationException,
            AnalysisEngineProcessException {
        logger.info(new StringBuffer("Analazying text").toString());
        /*
         * get the UIMA analysis engine
         */
        AnalysisEngine ae = aeProvider.getAE();

        /*
         * create a JCas which contain the text to analyze
         */
        JCas jcas = ae.newJCas();
        jcas.setDocumentText(textFieldValue);

        /*
         * perform analysis on text field
         */
        ae.process(jcas);
        logger.info(new StringBuilder("Text processing completed").toString());
        return jcas;
    }

    private List<FeatureStructure> concertToCasLight(JCas jcas, String typeName) {
        List<FeatureStructure> ret = new ArrayList<FeatureStructure>();

        Type type = jcas.getTypeSystem().getType(typeName);
        List<org.apache.uima.cas.Feature> featList = type.getFeatures();

        for (FSIterator<org.apache.uima.cas.FeatureStructure> iterator = jcas.getFSIndexRepository().getAllIndexedFS(type); iterator.hasNext(); ) {
            org.apache.uima.cas.FeatureStructure casFs = iterator.next();
            logger.debug("Processing UIMA CAS FeatureSet:" + casFs.toString());
            FeatureStructure newFs = new FeatureStructure(UUID.randomUUID().toString(), type.getShortName());
            for (org.apache.uima.cas.Feature casF : featList) {
                String fName = casF.getShortName();
                logger.debug("Feature Name:" + fName);

                if (casF.getRange().getName().equals("uima.cas.Sofa")) {
                    continue;
                }
                if (casF.getRange().isPrimitive()) {
                    logger.debug("Getting primitive value...");
                    if (casF.getRange().getName().equals("uima.cas.String")) {
                        String fVal = casFs.getStringValue(casF);
                        newFs.addFeature(new Feature<String>(fName, fVal));
                    } else if (casF.getRange().getName().equals("uima.cas.Integer")) {
                        int fVal = casFs.getIntValue(casF);
                        newFs.addFeature(new Feature<Integer>(fName, fVal));
                    } else if (casF.getRange().getName().equals("uima.cas.Short")) {
                        short fVal = casFs.getShortValue(casF);
                        newFs.addFeature(new Feature<Integer>(fName, (int) fVal));
                    } else if (casF.getRange().getName().equals("uima.cas.Byte")) {
                        byte fVal = casFs.getByteValue(casF);
                        newFs.addFeature(new Feature<Integer>(fName, (int) fVal));
                    } else if (casF.getRange().getName().equals("uima.cas.Double")) {
                        double fVal = casFs.getDoubleValue(casF);
                        newFs.addFeature(new Feature<Double>(fName, fVal));
                    } else if (casF.getRange().getName().equals("uima.cas.Float")) {
                        float fVal = casFs.getFloatValue(casF);
                        newFs.addFeature(new Feature<Double>(fName, (double) fVal));
                    } else {
                        Object fVal = casFs.clone();
                        newFs.addFeature(new Feature<Object>(fName, fVal));
                    }

                } else {
                    logger.debug("Getting FeatureStructure value...");
                    throw new UnsupportedOperationException("This client cannot handle FeatureStructure features");
                }
                if (casFs instanceof Annotation && "coveredText".equals(fName)) {
                    newFs.setCoveredText(((Annotation) casFs).getCoveredText());
                }
            }
            logger.debug("FeatureStructure:" + newFs);
            ret.add(newFs);
        }
        return ret;
    }
}
