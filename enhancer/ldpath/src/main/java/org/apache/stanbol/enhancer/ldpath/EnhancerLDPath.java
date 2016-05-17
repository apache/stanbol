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
package org.apache.stanbol.enhancer.ldpath;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_EXTRACTED_FROM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENHANCEMENT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TOPICANNOTATION;

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.apache.marmotta.ldpath.api.selectors.NodeSelector;
import org.apache.marmotta.ldpath.model.Constants;
import org.apache.marmotta.ldpath.parser.Configuration;
import org.apache.marmotta.ldpath.parser.DefaultConfiguration;
import org.apache.marmotta.ldpath.parser.ParseException;
import org.apache.stanbol.enhancer.ldpath.function.ContentFunction;
import org.apache.stanbol.enhancer.ldpath.function.PathFunction;
import org.apache.stanbol.enhancer.ldpath.function.SuggestionFunction;
import org.apache.stanbol.enhancer.ldpath.utils.Utils;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;

/**
 * Defines defaults for LDPath
 */
public final class EnhancerLDPath {
    
    private EnhancerLDPath(){}
    
    private static Configuration<RDFTerm> CONFIG;
    
    /**
     * The LDPath configuration including the <ul>
     * <li> Namespaces defined by the {@link NamespaceEnum}
     * <li> the LDPath functions for the Stanbol Enhancement Structure
     * </ul>
     * @return the LDPath configuration for the Stanbol Enhancer
     */
    public static final Configuration<RDFTerm> getConfig(){
        if(CONFIG == null){
            CONFIG = new DefaultConfiguration<RDFTerm>();
            //add the namespaces
            for(NamespaceEnum ns : NamespaceEnum.values()){
                CONFIG.addNamespace(ns.getPrefix(), ns.getNamespace());
            }
            //now add the functions
            addFunction(CONFIG, new ContentFunction());
            String path;
            NodeSelector<RDFTerm> selector;
            //TextAnnotations
            path = String.format("^%s[%s is %s]",
                ENHANCER_EXTRACTED_FROM,RDF_TYPE,ENHANCER_TEXTANNOTATION);
            try {
                selector = Utils.parseSelector(path);
            } catch (ParseException e) {
                throw new IllegalStateException("Unable to parse the ld-path selector '" +
                        path + "'used to select all TextAnnotations of a contentItem!", e);
            }
            addFunction(CONFIG, new PathFunction<RDFTerm>(
                    "textAnnotation",selector));
            
            //EntityAnnotations
            path = String.format("^%s[%s is %s]",
                ENHANCER_EXTRACTED_FROM,RDF_TYPE,ENHANCER_ENTITYANNOTATION);
            try {
                selector = Utils.parseSelector(path);
            } catch (ParseException e) {
                throw new IllegalStateException("Unable to parse the ld-path selector '" +
                        path + "'used to select all EntityAnnotations of a contentItem!", e);
            }
            addFunction(CONFIG,new PathFunction<RDFTerm>(
                    "entityAnnotation", selector));
            
            //TopicAnnotations
            path = String.format("^%s[%s is %s]",
                ENHANCER_EXTRACTED_FROM,RDF_TYPE,ENHANCER_TOPICANNOTATION);
            try {
                selector = Utils.parseSelector(path);
            } catch (ParseException e) {
                throw new IllegalStateException("Unable to parse the ld-path selector '" +
                        path + "'used to select all TopicAnnotations of a contentItem!", e);
            }
            addFunction(CONFIG,new PathFunction<RDFTerm>(
                    "topicAnnotation",selector));
            //Enhancements
            path = String.format("^%s[%s is %s]",
                ENHANCER_EXTRACTED_FROM,RDF_TYPE,ENHANCER_ENHANCEMENT);
            try {
                selector = Utils.parseSelector(path);
            } catch (ParseException e) {
                throw new IllegalStateException("Unable to parse the ld-path selector '" +
                        path + "'used to select all Enhancements of a contentItem!", e);
            }
            addFunction(CONFIG,new PathFunction<RDFTerm>(
                    "enhancement",selector));
            
            //Suggested EntityAnnotations for Text/TopicAnnotations
            
            //(1) to select the suggestions
            NodeSelector<RDFTerm> linkedEntityAnnotations;
            path = String.format("^%s[%s is %s]",
                DC_RELATION,RDF_TYPE,ENHANCER_ENTITYANNOTATION,ENHANCER_CONFIDENCE);
            try {
                linkedEntityAnnotations = Utils.parseSelector(path);
            } catch (ParseException e) {
                throw new IllegalStateException("Unable to parse the ld-path selector '" +
                        path + "'used to select all entity suggestions for an Enhancement!", e);
            }
            //(2) to select the confidence value of Enhancements
            NodeSelector<RDFTerm> confidenceSelector;
            path = ENHANCER_CONFIDENCE.toString();
            try {
                confidenceSelector = Utils.parseSelector(path);
            } catch (ParseException e) {
                throw new IllegalStateException("Unable to parse the ld-path selector '" +
                        path + "'used to select the confidence of suggestions!", e);
            }
            //The resultSelector is NULL because this directly returns the EntityAnnotations
            addFunction(CONFIG,new SuggestionFunction("suggestion",linkedEntityAnnotations,confidenceSelector,null));
            
            //Suggested Entities for Text/TopicAnnotations
            
            //The suggestion and confidence selectors can be the same as above,
            //but we need an additional result selector
            NodeSelector<RDFTerm> entityReferenceSelector;
            path = ENHANCER_ENTITY_REFERENCE.toString();
            try {
                entityReferenceSelector = Utils.parseSelector(path);
            } catch (ParseException e) {
                throw new IllegalStateException("Unable to parse the ld-path selector '" +
                        path + "'used to select the entity referenced by a EntityAnnotation!", e);
            }
            addFunction(CONFIG, new SuggestionFunction("suggestedEntity",
                linkedEntityAnnotations,confidenceSelector,entityReferenceSelector));
            
        }
        return CONFIG;
    }
    private static <Node> void addFunction(Configuration<Node> config, SelectorFunction<Node> function) {
        config.addFunction(Constants.NS_LMF_FUNCS + function.getPathExpression(null), function);
    }
}
