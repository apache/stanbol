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

package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.CASE_SENSITIVE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_CASE_SENSITIVE_MATCHING_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_INCLUDE_SIMILAR_SCORE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_MATCHING_LANGUAGE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.ENTITY_TYPES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.INCLUDE_SIMILAR_SCORE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.TYPE_MAPPINGS;
import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.PROPERTY_NAME;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * OSGI component used to configure a {@link FstLinkingEngine} with
 * {@link LinkingModeEnum#PLAIN}. <p>
 * <b>NOTE:</b> In this configuration no NLP processing other than language
 * detection is required for linking.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", 
    metatype = true, 
    immediate = true, 
    inherit = false)
@Properties(value={
        @Property(name=PROPERTY_NAME), //the name of the engine
        @Property(name=FstLinkingEngineComponent.SOLR_CORE),
        @Property(name=IndexConfiguration.FIELD_ENCODING, options={
            @PropertyOption(
                value='%'+IndexConfiguration.FIELD_ENCODING+".option.none",
                name="None"),
            @PropertyOption(
                value='%'+IndexConfiguration.FIELD_ENCODING+".option.solrYard",
                name="SolrYard"),
            @PropertyOption(
                value='%'+IndexConfiguration.FIELD_ENCODING+".option.minusPrefix",
                name="MinusPrefix"),
            @PropertyOption(
                value='%'+IndexConfiguration.FIELD_ENCODING+".option.underscorePrefix",
                name="UnderscorePrefix"),
            @PropertyOption(
                value='%'+IndexConfiguration.FIELD_ENCODING+".option.minusSuffix",
                name="MinusSuffix"),
            @PropertyOption(
                value='%'+IndexConfiguration.FIELD_ENCODING+".option.underscoreSuffix",
                name="UnderscoreSuffix"),
            @PropertyOption(
                value='%'+IndexConfiguration.FIELD_ENCODING+".option.atPrefix",
                name="AtPrefix"),
            @PropertyOption(
                value='%'+IndexConfiguration.FIELD_ENCODING+".option.atSuffix",
                name="AtSuffix")
            },value="SolrYard"),
        @Property(name=IndexConfiguration.FST_CONFIG, cardinality=Integer.MAX_VALUE),
        @Property(name=IndexConfiguration.FST_FOLDER, 
        value=IndexConfiguration.DEFAULT_FST_FOLDER),
        @Property(name=IndexConfiguration.SOLR_TYPE_FIELD, value="rdf:type"),
        @Property(name=IndexConfiguration.SOLR_RANKING_FIELD, value="entityhub:entityRank"),
        @Property(name=FstLinkingEngineComponent.FST_THREAD_POOL_SIZE,
            intValue=FstLinkingEngineComponent.DEFAULT_FST_THREAD_POOL_SIZE),
        @Property(name=FstLinkingEngineComponent.ENTITY_CACHE_SIZE, 
            intValue=FstLinkingEngineComponent.DEFAULT_ENTITY_CACHE_SIZE),
        @Property(name=SUGGESTIONS, intValue=DEFAULT_SUGGESTIONS),
        @Property(name=INCLUDE_SIMILAR_SCORE, boolValue=DEFAULT_INCLUDE_SIMILAR_SCORE),
        @Property(name=CASE_SENSITIVE,boolValue=DEFAULT_CASE_SENSITIVE_MATCHING_STATE),
        @Property(name=DEFAULT_MATCHING_LANGUAGE,value=""),
        @Property(name=ENTITY_TYPES,cardinality=Integer.MAX_VALUE),
        @Property(name=TYPE_MAPPINGS,cardinality=Integer.MAX_VALUE, value={
            "dbp-ont:Organisation; dbp-ont:Newspaper; schema:Organization > dbp-ont:Organisation",
            "dbp-ont:Person; foaf:Person; schema:Person > dbp-ont:Person",
            "dbp-ont:Place; schema:Place; geonames:Feature > dbp-ont:Place",
            "dbp-ont:Work; schema:CreativeWork > dbp-ont:Work",
            "dbp-ont:Event; schema:Event > dbp-ont:Event",
            "schema:Product > schema:Product",
            "skos:Concept > skos:Concept"}),
        @Property(name=SERVICE_RANKING,intValue=0)
    })
public class PlainFstLinkingComponnet extends FstLinkingEngineComponent {

    /**
     * used to resolve '{prefix}:{local-name}' used within the engines configuration
     */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    private NamespacePrefixService prefixService;    

    
    @Activate
    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        log.info("activate {}",getClass().getSimpleName());
        this.bundleContext = ctx.getBundleContext();
        super.applyConfig(LinkingModeEnum.PLAIN, ctx.getProperties(), prefixService);
    }
    
    @Deactivate
    @Override
    protected void deactivate(ComponentContext ctx) {
        super.deactivate(ctx);
    }
}
