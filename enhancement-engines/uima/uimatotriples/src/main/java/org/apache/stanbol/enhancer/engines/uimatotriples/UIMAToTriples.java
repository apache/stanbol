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
package org.apache.stanbol.enhancer.engines.uimatotriples;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.caslight.Feature;
import org.apache.stanbol.commons.caslight.FeatureStructure;
import org.apache.stanbol.commons.caslight.FeatureStructureListHolder;
import org.apache.stanbol.enhancer.engines.uimatotriples.tools.FeatureFilter;
import org.apache.stanbol.enhancer.engines.uimatotriples.tools.FeatureStructureFilter;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

/**
 * @author Mihaly Heder
 */
@Component(immediate = true, metatype = true, inherit = true,
        label = "UIMA To RDF triples Enhancement Engine",
        description = "Filters and converts UIMA Feature Structures to RDF Triples")
@Service
@Properties(value = {
        @Property(name = EnhancementEngine.PROPERTY_NAME, value = "uimatotriples")
})
public class UIMAToTriples extends AbstractEnhancementEngine<RuntimeException, RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Integer defaultOrder = ServiceProperties.ORDERING_POST_PROCESSING;

    @Property(cardinality = 1000, label = "UIMA source names",
            description = "The name of the uima sources as defined in the UIMA Client Enhancement Engine")
    private static final String UIMA_SOURCENAMES = "stanbol.engine.uimatotriples.sourcenames";

    @Property(value = "uima.apache.org", label = "Content Part URI reference",
            description = "The URI Reference of the UIMA content part, as defined in the UIMA Client")
    private static final String UIMA_CONTENTPART_URIREF = "stanbol.engine.uimatotriples.contentpart.uriref";

    @Property(cardinality = 1000, label = "UIMA annotations to process",
            description = "The UIMA type names enumerated here will be converted to triples. "
                    + "You can filter by features, e.g TokenAnnotation;posTag=v.* will give you only "
                    + "those TokenAnnotations which have a posTag and its value matches the "
                    + "regexp 'v.*'. No other features will be converted. If you want to convert additional features, "
                    + "enumerate them: TokenAnnotation;posTag=v.*;lemma this will also convert the lemma feature of "
                    + "the filtered TokenAnnotations  ")
    private static final String UIMA_TYPENAMES = "stanbol.engine.uimatotriples.typenames";

    @Property(cardinality = 1000, label = "UIMA type/feature name to RDF name mappings",
            description = "Syntax: oldName;newname . You can provide here a mapping according to which names should be translated to RDF."
                    + "E.g. you might want to an UIMA posTag feature to appear as sso:posTag. You can give mappings for type"
                    + "names as well as for feature names here.")
    private static final String UIMA_MAPPINGS = "stanbol.engine.uimatotriples.mappings";

    protected static final Set<String> SUPPORTED_MIMETYPES =
            Collections.unmodifiableSet(new HashSet<String>(
                    Arrays.asList("text/plain", "text/html")));

    private String uimaUri;
    private String[] sourceNames;
    private Map<String, String> mappings;
    private FeatureStructureFilter tnfs;

    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        Dictionary<String, Object> props = ctx.getProperties();
        this.sourceNames = (String[]) props.get(UIMA_SOURCENAMES);
        this.tnfs = new FeatureStructureFilter();
        String[] typeNameStrings = (String[]) props.get(UIMA_TYPENAMES);
        if (typeNameStrings != null) {
            for (String typ : typeNameStrings) {
                String[] mainparts = typ.split(";", 2);
                FeatureFilter tnf = new FeatureFilter();
                tnf.setTypeName(mainparts[0]);
                if (mainparts.length == 1) {
                    tnfs.addFeatureFilter(tnf);
                    continue;
                }
                String[] subParts = mainparts[1].split(";");
                for (String subP : subParts) {
                    String[] subsubP = subP.split("=", 2);
                    if (subsubP.length == 1) {
                        tnf.addFeatureFilter(subsubP[0], "");
                        continue;
                    }
                    tnf.addFeatureFilter(subsubP[0], subsubP[1]);
                }
                tnfs.addFeatureFilter(tnf);
            }
        }
        mappings = new HashMap<String, String>();
        String[] mappingStings = (String[]) props.get(UIMA_MAPPINGS);
        if (mappingStings != null) {
            for (String map : mappingStings) {
                String[] mainparts = map.split(";", 2);
                if (mainparts.length == 2) {
                    mappings.put(mainparts[0], mainparts[1]);
                } else {
                    logger.warn(new StringBuilder("Mapping string '").append(map).append("' does not contain ';'. Skipping this mapping.").toString());
                }
            }
        }
        this.uimaUri = (String) props.get(UIMA_CONTENTPART_URIREF);
    }

    public int canEnhance(ContentItem ci) throws EngineException {
        return ENHANCE_SYNCHRONOUS;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        FeatureStructureListHolder holder;
        LiteralFactory literalFactory = LiteralFactory.getInstance();


        try {
            IRI uimaIRI = new IRI(uimaUri);
            logger.info(new StringBuilder("Trying to load holder for ref:").append(uimaUri).toString());
            holder = ci.getPart(uimaIRI, FeatureStructureListHolder.class);
            for (String source : sourceNames) {
                logger.info(new StringBuilder("Processing UIMA source:").append(source).toString());
                List<FeatureStructure> sourceList = holder.getFeatureStructureList(source);
                if (sourceList != null) {
                    logger.info(new StringBuilder("UIMA source:").append(source)
                            .append(" contains ").append(sourceList.size()).append(" annotations.").toString());
                } else {
                    logger.info(new StringBuilder("Source list is null:").append(source).toString());
                    continue;
                }
                for (FeatureStructure fs : sourceList) {
                    String typeName = fs.getTypeName();
                    logger.debug(new StringBuilder("Checking ").append(typeName).toString());
                    if (tnfs.checkFeatureStructureAllowed(typeName, fs.getFeatures())) {
                        logger.debug(new StringBuilder("Adding ").append(typeName).toString());
                        IRI textAnnotation = EnhancementEngineHelper.createTextEnhancement(
                                ci, this);
                        Graph metadata = ci.getMetadata();
                        String uriRefStr = uimaUri + ":" + typeName;
                        if (mappings.containsKey(typeName)) {
                            uriRefStr = mappings.get(typeName);
                        }
                        metadata.add(new TripleImpl(textAnnotation, DC_TYPE, new IRI(uriRefStr)));

                        if (fs.getFeature("begin") != null) {
                            metadata.add(new TripleImpl(textAnnotation, ENHANCER_START,
                                    literalFactory.createTypedLiteral(fs.getFeature("begin").getValueAsInteger())));
                        }
                        if (fs.getFeature("end") != null) {
                            metadata.add(new TripleImpl(textAnnotation, ENHANCER_END,
                                    literalFactory.createTypedLiteral(fs.getFeature("end").getValueAsInteger())));
                        }
                        if (fs.getCoveredText() != null && !fs.getCoveredText().isEmpty()) {
                            metadata.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, new PlainLiteralImpl(fs.getCoveredText())));
                        }

                        for (Feature f : fs.getFeatures()) {
                            if (!f.getName().equals("begin") && !f.getName().equals("end") && tnfs.checkFeatureToConvert(typeName, f)) {
                                String predRefStr = uimaUri + ":" + f.getName();

                                if (mappings.containsKey(f.getName())) {
                                    predRefStr = mappings.get(f.getName());
                                }

                                IRI predicate = new IRI(predRefStr);

                                metadata.add(new TripleImpl(textAnnotation, predicate, new PlainLiteralImpl(f.getValueAsString())));
                            }
                        }

                    }
                }
            }


        } catch (NoSuchPartException e) {
            logger.error(new StringBuilder("No UIMA results found with ref:").append(uimaUri).toString(), e);
        }
    }

    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING,
                (Object) defaultOrder));
    }
}
