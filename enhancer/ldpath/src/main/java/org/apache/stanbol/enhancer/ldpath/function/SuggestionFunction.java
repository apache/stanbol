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
package org.apache.stanbol.enhancer.ldpath.function;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.api.functions.SelectorFunction;
import at.newmedialab.ldpath.api.selectors.NodeSelector;
import at.newmedialab.ldpath.model.transformers.IntTransformer;
import at.newmedialab.ldpath.model.transformers.StringTransformer;

public class SuggestionFunction implements SelectorFunction<Resource> {
    
    private static final Comparator<Entry<Double,Resource>> SUGGESTION_COMPARATOR = 
            new Comparator<Entry<Double,Resource>>() {

        @Override
        public int compare(Entry<Double,Resource> e1, Entry<Double,Resource> e2) {
            return e2.getKey().compareTo(e1.getKey());
        }
        
    };
    private static final int MISSING_CONFIDENCE_FIRST = -1;
    private static final int MISSING_CONFIDENCE_FILTER = 0;
    private static final int MISSING_CONFIDENCE_LAST = -1;
    private static final int DEFAULT_MISSING_CONFIDENCE_MODE = MISSING_CONFIDENCE_FILTER;
    private static final Double MAX = Double.valueOf(Double.POSITIVE_INFINITY);
    private static final Double MIN = Double.valueOf(Double.NEGATIVE_INFINITY);
//    private static final String ANNOTATION_PROCESSING_MODE_SINGLE = "single";
//    private static final String ANNOTATION_PROCESSING_MODE_UNION = "union";
//    private static final String DEFAULT_ANNOTATION_PROCESSING_MODE = ANNOTATION_PROCESSING_MODE_SINGLE;

    Logger log = LoggerFactory.getLogger(SuggestionFunction.class);

    private final String name;
    private final IntTransformer<Resource> intTransformer;
    private final StringTransformer<Resource> stringTransformer;
    private final NodeSelector<Resource> suggestionSelector;
    private final NodeSelector<Resource> confidenceSelector;
    private final NodeSelector<Resource> resultSelector;
    public SuggestionFunction(String name,
                              NodeSelector<Resource> suggestionSelector,
                              NodeSelector<Resource> confidenceSelector){
        this(name,null,suggestionSelector,confidenceSelector);
    }
    public SuggestionFunction(String name,
                              NodeSelector<Resource> suggestionSelector,
                              NodeSelector<Resource> confidenceSelector,
                              NodeSelector<Resource> resultSelector) {
        intTransformer = new IntTransformer<Resource>();
        stringTransformer = new StringTransformer<Resource>();
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed function name MUST NOT be NULL nor empty!");
        }
        this.name = name;
        if(suggestionSelector == null){
            throw new IllegalArgumentException("The NodeSelector used to select the Suggestions for the parsed Context MUST NOT be NULL!");
        }
        this.suggestionSelector = suggestionSelector;
        if(confidenceSelector == null){
            throw new IllegalArgumentException("The NodeSelector used to select the Confidence for Suggestions MUST NOT be NULL!");
        }
        this.confidenceSelector = confidenceSelector;
        this.resultSelector = resultSelector;
    }
    
    @Override
    public Collection<Resource> apply(final RDFBackend<Resource> backend, Collection<Resource>... args) throws IllegalArgumentException {
        Integer limit = parseParamLimit(backend, args,1);
//        final String processingMode = parseParamProcessingMode(backend, args,2);
        final int missingConfidenceMode = parseParamMissingConfidenceMode(backend, args,2);
        List<Resource> result = new ArrayList<Resource>();
//        if(processingMode.equals(ANNOTATION_PROCESSING_MODE_UNION)){
            processAnnotations(backend, args[0], limit, missingConfidenceMode, result);
//        } else {
//            for(Resource context : args[0]){
//                processAnnotations(backend, singleton(context),
//                    limit, missingConfidenceMode, result);
//            }
//        }
        return result;
    }
    /**
     * Suggestions are selected by all Annotations returned by the parsed
     * {@link #annotationSelector}.
     * @param backend
     * @param annotations suggestions are selected for the union of the parsed
     * annotations - the {limit} most linked entities for the parsed
     * list of annotations.
     * @param limit the maximum number of suggestions for the parsed collection
     * of annotations.
     * @param missingConfidenceMode
     * @param result results are added to this list.
     */
    private void processAnnotations(final RDFBackend<Resource> backend,
                                    Collection<Resource> annotations,
                                    Integer limit,
                                    final int missingConfidenceMode,
                                    List<Resource> result) {
        List<Entry<Double,Resource>> suggestions = new ArrayList<Entry<Double,Resource>>();
        for(Resource annotation : annotations){
            for(Resource suggestion : suggestionSelector.select(backend, annotation)){
                Collection<Resource> cs = confidenceSelector.select(backend, suggestion);
                Double confidence = !cs.isEmpty() ? backend.doubleValue(cs.iterator().next()) : 
                        missingConfidenceMode == MISSING_CONFIDENCE_FILTER ?
                                null : missingConfidenceMode == MISSING_CONFIDENCE_FIRST ?
                                        MAX : MIN;
                if(confidence != null){
                    suggestions.add(singletonMap(confidence,suggestion).entrySet().iterator().next());

                }
            }
        }
        Collections.sort(suggestions, SUGGESTION_COMPARATOR);
        int resultSize = limit != null ? Math.min(limit, suggestions.size()) : suggestions.size();
        for(Entry<Double,Resource> suggestion : suggestions.subList(0, resultSize)){
            if(resultSelector == null){
                result.add(suggestion.getValue());
            } else {
                result.addAll(resultSelector.select(backend, suggestion.getValue()));
            }
        }
    }
    /*
     * Helper Method to parse the parameter
     */
    /**
     * @param backend
     * @param args
     * @return
     */
    private int parseParamMissingConfidenceMode(final RDFBackend<Resource> backend,
                                                Collection<Resource>[] args, int index) {
        final int missingConfidenceMode;
        if(args.length > index && !args[index].isEmpty()){
            String mode = stringTransformer.transform(backend, args[index].iterator().next());
            if("first".equalsIgnoreCase(mode)){
                missingConfidenceMode = MISSING_CONFIDENCE_FIRST;
            } else if("last".equalsIgnoreCase(mode)){
                missingConfidenceMode = MISSING_CONFIDENCE_LAST;
            } else if("filter".equalsIgnoreCase(mode)){
                missingConfidenceMode = MISSING_CONFIDENCE_FILTER;
            } else {
                missingConfidenceMode = DEFAULT_MISSING_CONFIDENCE_MODE;
                log.warn("Unknown value for parameter 'missing confidence value mode' '{}'" +
                		"(supported: 'first','last','filter') use default: 'filter')",mode);
            }
        } else {
            missingConfidenceMode = DEFAULT_MISSING_CONFIDENCE_MODE;
        }
        return missingConfidenceMode;
    }
//    /**
//     * @param backend
//     * @param args
//     * @return
//     */
//    private String parseParamProcessingMode(final RDFBackend<Resource> backend, Collection<Resource>[] args, int index) {
//        final String processingMode;
//        if(args.length > index && !args[index].isEmpty()){
//            String mode = stringTransformer.transform(backend, args[index].iterator().next());
//            if(ANNOTATION_PROCESSING_MODE_SINGLE.equalsIgnoreCase(mode)){
//                processingMode = ANNOTATION_PROCESSING_MODE_SINGLE;
//            } else if(ANNOTATION_PROCESSING_MODE_UNION.equalsIgnoreCase(mode)) {
//                processingMode = ANNOTATION_PROCESSING_MODE_UNION;
//            } else {
//                processingMode = DEFAULT_ANNOTATION_PROCESSING_MODE;
//                log.warn("Unknown value for parameter 'annotation processing mode' '{}'" +
//                        "(supported: 'single','union') default: 'single')",mode);
//            }
//        } else {
//            processingMode = DEFAULT_ANNOTATION_PROCESSING_MODE;
//        }
//        return processingMode;
//    }
    /**
     * @param backend
     * @param args
     * @return
     */
    private Integer parseParamLimit(final RDFBackend<Resource> backend, Collection<Resource>[] args,int index) {
        Integer limit = null;
        if(args.length > index && !args[index].isEmpty()){
            Resource value = args[index].iterator().next();
            try {
                limit = intTransformer.transform(backend, value);
                if(limit < 1){
                    limit = null;
                }
            } catch (RuntimeException e) {
                log.warn("Unable to parse parameter 'limit' form the 2nd argument '{}'",value);
            }
        }
        return limit;
    }


    @Override
    public String getPathExpression(RDFBackend<Resource> backend) {
        return name;
    }

    
}
