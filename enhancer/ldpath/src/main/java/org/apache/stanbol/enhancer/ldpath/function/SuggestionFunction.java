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

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.apache.marmotta.ldpath.api.selectors.NodeSelector;
import org.apache.marmotta.ldpath.model.transformers.IntTransformer;
import org.apache.marmotta.ldpath.model.transformers.StringTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestionFunction extends SelectorFunction<RDFTerm> {
    
    private static final Comparator<Entry<Double,RDFTerm>> SUGGESTION_COMPARATOR = 
            new Comparator<Entry<Double,RDFTerm>>() {

        @Override
        public int compare(Entry<Double,RDFTerm> e1, Entry<Double,RDFTerm> e2) {
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
    private final IntTransformer<RDFTerm> intTransformer;
    private final StringTransformer<RDFTerm> stringTransformer;
    private final NodeSelector<RDFTerm> suggestionSelector;
    private final NodeSelector<RDFTerm> confidenceSelector;
    private final NodeSelector<RDFTerm> resultSelector;
    public SuggestionFunction(String name,
                              NodeSelector<RDFTerm> suggestionSelector,
                              NodeSelector<RDFTerm> confidenceSelector){
        this(name,null,suggestionSelector,confidenceSelector);
    }
    public SuggestionFunction(String name,
                              NodeSelector<RDFTerm> suggestionSelector,
                              NodeSelector<RDFTerm> confidenceSelector,
                              NodeSelector<RDFTerm> resultSelector) {
        intTransformer = new IntTransformer<RDFTerm>();
        stringTransformer = new StringTransformer<RDFTerm>();
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
    public Collection<RDFTerm> apply(final RDFBackend<RDFTerm> backend, RDFTerm context, Collection<RDFTerm>... args) throws IllegalArgumentException {
        int paramIndex = 0;
        Collection<RDFTerm> contexts = null;
        if(args != null && args.length > 0 && args[0] != null && !args[0].isEmpty()){
            contexts = new ArrayList<RDFTerm>();
            for(RDFTerm r : args[0]){
                if(backend.isURI(r)){
                    contexts.add(r);
                    paramIndex = 1;
                }
            }
        }
        if(paramIndex == 0){ //no contexts parsed os first param ... use the current context
            contexts = Collections.singleton(context);
        }
        Integer limit = parseParamLimit(backend, args,paramIndex);
//        final String processingMode = parseParamProcessingMode(backend, args,2);
        final int missingConfidenceMode = parseParamMissingConfidenceMode(backend, args,paramIndex+1);
        List<RDFTerm> result = new ArrayList<RDFTerm>();
//        if(processingMode.equals(ANNOTATION_PROCESSING_MODE_UNION)){
            processAnnotations(backend, contexts, limit, missingConfidenceMode, result);
//        } else {
//            for(RDFTerm context : args[0]){
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
    private void processAnnotations(final RDFBackend<RDFTerm> backend,
                                    Collection<RDFTerm> annotations,
                                    Integer limit,
                                    final int missingConfidenceMode,
                                    List<RDFTerm> result) {
        List<Entry<Double,RDFTerm>> suggestions = new ArrayList<Entry<Double,RDFTerm>>();
        for(RDFTerm annotation : annotations){
            //NOTE: no Path Tracking support possible for selectors wrapped in functions
            for(RDFTerm suggestion : suggestionSelector.select(backend, annotation,null,null)){
                Collection<RDFTerm> cs = confidenceSelector.select(backend, suggestion,null,null);
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
        for(Entry<Double,RDFTerm> suggestion : suggestions.subList(0, resultSize)){
            if(resultSelector == null){
                result.add(suggestion.getValue());
            } else {
                result.addAll(resultSelector.select(backend, suggestion.getValue(),null,null));
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
    private int parseParamMissingConfidenceMode(final RDFBackend<RDFTerm> backend,
                                                Collection<RDFTerm>[] args, int index) {
        final int missingConfidenceMode;
        if(args.length > index && !args[index].isEmpty()){
            String mode = stringTransformer.transform(backend, args[index].iterator().next(),
                Collections.<String,String>emptyMap());
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
//    private String parseParamProcessingMode(final RDFBackend<RDFTerm> backend, Collection<RDFTerm>[] args, int index) {
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
    private Integer parseParamLimit(final RDFBackend<RDFTerm> backend, Collection<RDFTerm>[] args,int index) {
        Integer limit = null;
        if(args.length > index && !args[index].isEmpty()){
            RDFTerm value = args[index].iterator().next();
            try {
                limit = intTransformer.transform(backend, value, Collections.<String,String>emptyMap());
                if(limit < 1){
                    limit = null;
                }
            } catch (RuntimeException e) {
                log.warn("Unable to parse parameter 'limit' form the {}nd argument '{}'",index, value);
                log.warn("Stacktrace:", e);
            }
        }
        return limit;
    }


    @Override
    protected String getLocalName(){
        return name;
    }
    @Override
    public String getSignature() {
        return "fn:"+name+"([{context},]{limit},{missing-confidence-mode})";
    }
    @Override
    public String getDescription() {
        return "Function that retrieves EntitySuggestions for TextAnnotations (sorted by highest confidence first)";
    }

    
}
