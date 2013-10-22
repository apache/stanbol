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
package org.apache.stanbol.enhancer.nlp.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParserRegistry;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.io.SerializedString;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value=AnalyzedTextParser.class)
public class AnalyzedTextParser {
    
    private final Logger log = LoggerFactory.getLogger(AnalyzedTextParser.class);
    
    private final static Charset UTF8 = Charset.forName("UTF-8");
    
    private static AnalyzedTextParser defaultInstance;
    
    protected ObjectMapper mapper = new ObjectMapper();    
    /**
     * Can be used when running outside of OSGI to obtain the default (singleton)
     * instance.
     * @return
     */
    public static final AnalyzedTextParser getDefaultInstance(){
        if(defaultInstance == null){
            defaultInstance = new AnalyzedTextParser(
                ValueTypeParserRegistry.getInstance());
        }
        return defaultInstance;
    }
    
    /**
     * Default constructor used by OSGI
     */
    public AnalyzedTextParser() {}
    
    /**
     * Constructs a new Parser instance for the parsed {@link ValueTypeParserRegistry}
     * instance. Typically this constructor should not be used as usages within
     * an OSGI environment MUST lookup the service via the service registry.
     * Usages outside an OSGI environment should prefer to use the
     * {@link #getDefaultInstance()} instance to obtain the singleton instance.
     * @param vtsr
     */
    public AnalyzedTextParser(ValueTypeParserRegistry vtpr){
        if(vtpr == null){
            throw new IllegalArgumentException("The parsed ValueTypeParserRegistry MUST NOT be NULL!");
        }
        this.valueTypeParserRegistry = vtpr;
    }
    
    @Reference
    protected ValueTypeParserRegistry valueTypeParserRegistry;
    
    /**
     * Parses {@link AnalysedText} {@link Span}s including annotations from the 
     * {@link InputStream}. The {@link AnalysedText} instance that is going to
     * be enrichted with the parsed data needs to be parsed. In the simplest case
     * the caller can create an empty instance by using a 
     * {@link AnalysedTextFactory}.
     * @param in The stream to read the data from
     * @param charset the {@link Charset} used by the stream
     * @param at The {@link AnalysedText} instance used to add the data to
     * @return the parsed {@link AnalysedText} instance enrichted with the
     * information parsed from the Stream
     * @throws IOException on any Error while reading or parsing the data
     * from the Stream
     */
    public AnalysedText parse(InputStream in, Charset charset, final AnalysedText at) throws IOException {
        if(in == null){
            throw new IllegalArgumentException("The parsed InputStream MUST NOT be NULL!");
        }
        if(charset == null){
            charset = UTF8;
        }
        JsonParser parser = mapper.getJsonFactory().createJsonParser(new InputStreamReader(in, charset));
        if(parser.nextToken() != JsonToken.START_OBJECT) { //start object
            throw new IOException("JSON serialized AnalyzedTexts MUST use a JSON Object as Root!");
        }
        if(!parser.nextFieldName(new SerializedString("spans"))){
            throw new IOException("JSON serialized AnalyzedText MUST define the 'spans' field as first entry "
                + "in the root JSON object!");
        }
        if(parser.nextValue() != JsonToken.START_ARRAY){
            throw new IOException("The value of the 'span' field MUST BE an Json Array!");
        }
        boolean first = true;
        while(parser.nextValue() == JsonToken.START_OBJECT){
            if(first){
                parseAnalyzedTextSpan(parser.readValueAsTree(), at);
                first = false;
            } else {
                parseSpan(at, parser.readValueAsTree());
            }
        }
        return at;
    }

    private void parseAnalyzedTextSpan(JsonNode node, AnalysedText at) throws IOException {
        if(node.isObject()){
            ObjectNode jSpan = (ObjectNode)node;
            int[] spanPos = new int[]{-1,-1}; 
            Collection<Entry<String,JsonNode>> jAnnotations = new ArrayList<Entry<String,JsonNode>>(4);
            SpanTypeEnum spanType = parseSpanData(jSpan, spanPos, jAnnotations);
            if(spanType != SpanTypeEnum.Text || spanPos[0] != 0 || spanPos[1] < 0){
                throw new IOException("The AnalyzedText span MUST have the SpanType 'text', a "
                        + "start position of '0' and an end position (ignored, json: "+jSpan);
            }
            if(at.getEnd() != spanPos[1]){
                throw new IOException("The size of the local text '"+at.getEnd()+"' does not "
                    + "match the span of the parsed AnalyzedText ["+spanPos[0]+","+spanPos[1]+"]!");
            }
            parseAnnotations(at, jAnnotations);
        } else {
            throw new IOException("Unable to parse AnalyzedText span form JsonNode "+node+" (expected JSON object)!");
        }
        
    }
    
    private void parseSpan(AnalysedText at, JsonNode node) throws IOException {
        if(node.isObject()){
            ObjectNode jSpan = (ObjectNode)node;
            int[] spanPos = new int[]{-1,-1}; 
            Collection<Entry<String,JsonNode>> jAnnotations = new ArrayList<Entry<String,JsonNode>>(4);
            SpanTypeEnum spanType = parseSpanData(jSpan, spanPos, jAnnotations);
            if(spanType == null || spanPos[0] < 0 || spanPos[1] < 0){
                log.warn("Illegal or missing span type, start and/or end position (ignored, json: "+jSpan);
                return;
            }
            //now create the Span
            Span span;
            switch (spanType) {
                case Text:
                    log.warn("Encounterd 'Text' span that is not the first span in the "
                        + "'spans' array (ignored, json: "+node+")");
                    return;
                case TextSection:
                    log.warn("Encountered 'TextSection' span. This SpanTypeEnum entry "
                        + "is currently unused. If this is no longer the case please "
                        + "update this implementation (ignored, json: "+node+")"); 
                    return;
                case Sentence:
                    span = at.addSentence(spanPos[0], spanPos[1]);
                    break;
                case Chunk:
                    span = at.addChunk(spanPos[0], spanPos[1]);
                    break;
                case Token:
                    span = at.addToken(spanPos[0], spanPos[1]);
                    break;
                default:
                    log.warn("Unsupported SpanTypeEnum  '"+spanType+"'!. Please "
                            + "update this implementation (ignored, json: "+node+")"); 
                    return;
            }
            if(!jAnnotations.isEmpty()){
                parseAnnotations(span,jAnnotations);
            }
        } else {
            log.warn("Unable to parse Span form JsonNode "+node+" (expected JSON object)!");
        }
    }

    /**
     * @param jSpan
     * @param spanPos
     * @param jAnnotations
     * @return the type of the parsed span
     */
    private SpanTypeEnum parseSpanData(ObjectNode jSpan, int[] spanPos,
            Collection<Entry<String,JsonNode>> jAnnotations) {
        SpanTypeEnum spanType = null;
        for(Iterator<Entry<String,JsonNode>> fields = jSpan.getFields(); fields.hasNext();){
            Entry<String,JsonNode> field = fields.next();
            if("type".equals(field.getKey())){
                if(field.getValue().isTextual()){
                    spanType = SpanTypeEnum.valueOf(field.getValue().getTextValue());
                } else if(field.getValue().isInt()){
                    spanType = SpanTypeEnum.values()[field.getValue().getIntValue()];
                } else {
                    log.warn("Unable to parse SpanType form JSON field "+field +" (ignored, json: "+jSpan+")");
                    return null;
                }
            } else if("start".equals(field.getKey())){
                if(field.getValue().isInt()){
                    spanPos[0] = field.getValue().getIntValue();
                } else {
                    log.warn("Unable to parse span start position form JSON field "
                            +field +" (ignored, json: "+jSpan+")");
                    return null;
                }
            } else if("end".equals(field.getKey())){
                if(field.getValue().isInt()){
                    spanPos[1] = field.getValue().getIntValue();
                } else {
                    log.warn("Unable to parse span end position form JSON field "
                            +field +" (ignored, json: "+jSpan+")");
                    return null;
                }
            } else {
                jAnnotations.add(field);
            }
        }
        if(spanType == null){
            log.warn("Missing required field 'type' defining the type of the Span!");
        }
        return spanType;
    }


    private void parseAnnotations(Span span, Collection<Entry<String,JsonNode>> jAnnotations) throws IOException {
        for(Entry<String,JsonNode> jAnnotation : jAnnotations){
            if(jAnnotation.getValue().isObject()){
                parseAnnotation(span, jAnnotation.getKey(), (ObjectNode)jAnnotation.getValue());
            } else if(jAnnotation.getValue().isArray()){
                ArrayNode jValues = (ArrayNode)jAnnotation.getValue();
                for(int i=0;i< jValues.size();i++){
                    JsonNode jValue = jValues.get(i);
                    if(jValue.isObject()){
                        parseAnnotation(span, jAnnotation.getKey(), (ObjectNode)jValue);
                    } else {
                        log.warn("unable to parse the {} value of the annotation {} "
                            + "because value is no JSON object (ignored, json: {}",
                            new Object[]{i,jAnnotation.getKey(),jAnnotation.getValue()});
                    }
                }
            } else {
                log.warn("unable to parse Annotation {} because value is no JSON object (ignored, json: {}",
                    jAnnotation.getKey(),jAnnotation.getValue());
            }
        }
    
    }

    private void parseAnnotation(Span span, String key, ObjectNode jValue) throws IOException {
        JsonNode jClass = jValue.path("class");
        if(!jClass.isTextual()){
            log.warn("unable to parse Annotation {} because 'class' field "
                + "is not set or not a stringis no JSON object (ignored, json: {}",
                key,jValue);
            return;
        }
        Class<?> clazz;
        try {
            clazz = AnalyzedTextParser.class.getClassLoader().loadClass(jClass.getTextValue());
        } catch (ClassNotFoundException e) {
            log.warn("Unable to parse Annotation "+key 
                + " because the 'class' "+jClass.getTextValue()+" of the "
                + "the value can not be resolved (ignored, json: "+jValue+")",e);
            return;
        }
        ValueTypeParser<?> parser = this.valueTypeParserRegistry.getParser(clazz);
        Object value;
        if(parser != null){
            value = parser.parse(jValue, span.getContext());
        } else {
            JsonNode valueNode = jValue.path("value");
            if(valueNode.isMissingNode()){
                log.warn("unable to parse value for annotation {} because the "
                    + "field 'value' is not present (ignored, json: {}",
                    key,jValue);
                return;
            } else {
                try {
                    value = mapper.treeToValue(valueNode, clazz);
                } catch (JsonParseException e) {
                    log.warn("unable to parse value for annotation "
                            + key+ "because the value can"
                            + "not be converted to the class "+ clazz.getName()
                            + "(ignored, json: "+jValue+")",e);
                    return;
                } catch (JsonMappingException e) {
                    log.warn("unable to parse value for annotation "
                            + key+ "because the value can"
                            + "not be converted to the class "+ clazz.getName()
                            + "(ignored, json: "+jValue+")",e);
                    return;
                }
            }
        }
        JsonNode jProb = jValue.path("prob");
        if(!jProb.isDouble()){
            span.addValue(key, Value.value(value));
        } else {
            span.addValue(key, Value.value(value,jProb.getDoubleValue()));
        }        
    }


    /**
     * Parses the SpanType for the parsed {@link ObjectNode} representing a {@link Span}
     * @param jSpan the JSON root node of the span
     * @return the type or <code>null</code> if the information is missing
     */
    private SpanTypeEnum parseSpanType(ObjectNode jSpan) {
        EnumSet<SpanTypeEnum> spanTypes = JsonUtils.parseEnum(jSpan, "type", SpanTypeEnum.class);
        if(spanTypes.isEmpty()){
            log.warn("Unable to parse Span with missing 'type' (json: "+jSpan+")!");
            return null;
        }
        if(spanTypes.size() > 1){
            log.warn("Found Span with multiple 'types' (Json:"+jSpan+")!");
        }
        return spanTypes.iterator().next();
    }
    
}
