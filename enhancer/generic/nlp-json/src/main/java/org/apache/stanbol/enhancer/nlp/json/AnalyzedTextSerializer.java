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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializerRegistry;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializes an AnalysedText instance as JSON
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value=AnalyzedTextSerializer.class)
public class AnalyzedTextSerializer {
    
    Logger log = LoggerFactory.getLogger(AnalyzedTextSerializer.class);

    private final static Charset UTF8 = Charset.forName("UTF-8");
    
    private static AnalyzedTextSerializer defaultInstance;
    protected ObjectMapper mapper = new ObjectMapper();    
    /**
     * Can be used when running outside of OSGI to obtain the default (singelton)
     * instance.
     * @return
     */
    public static final AnalyzedTextSerializer getDefaultInstance(){
        if(defaultInstance == null){
            defaultInstance = new AnalyzedTextSerializer(ValueTypeSerializerRegistry.getInstance());
        }
        return defaultInstance;
    }
    
    /**
     * Default constructor used by OSGI
     */
    public AnalyzedTextSerializer() {}
    
    /**
     * Constructs a new Serializer instance for the parsed {@link ValueTypeSerializerRegistry}
     * instance. Typically this constructor should not be used as usages within
     * an OSGI environment MUST lookup the service via the service registry.
     * Usages outside an OSGI environment should prefer to use the
     * {@link #getDefaultInstance()} instance to obtain the singleton instance.
     * @param vtsr
     */
    public AnalyzedTextSerializer(ValueTypeSerializerRegistry vtsr){
        if(vtsr == null){
            throw new IllegalArgumentException("The parsed ValueTypeSerializerRegistry MUST NOT be NULL!");
        }
        this.valueTypeSerializerRegistry = vtsr;
    }
    
    @Reference
    protected ValueTypeSerializerRegistry valueTypeSerializerRegistry;

    /**
     * Serializes the parsed {@link AnalysedText} to the {@link OutputStream} by
     * using the {@link Charset}.
     * @param at the {@link AnalysedText} to serialize
     * @param out the {@link OutputStream} 
     * @param charset the {@link Charset}. UTF-8 is used as default if <code>null</code>
     * is parsed
     */
    public void serialize(AnalysedText at, OutputStream out, Charset charset) throws IOException {
        if(at == null){
            throw new IllegalArgumentException("The parsed AnalysedText MUST NOT be NULL!");
        }
        if(out == null){
            throw new IllegalArgumentException("The parsed OutputStream MUST NOT be NULL");
        }
        if(charset == null){
            charset = UTF8;
        }
        JsonFactory jsonFactory = mapper.getJsonFactory();
        JsonGenerator jg = jsonFactory.createJsonGenerator(new OutputStreamWriter(out, charset));
        jg.useDefaultPrettyPrinter();
        jg.writeStartObject();
        jg.writeArrayFieldStart("spans");
        jg.writeTree(writeSpan(at));
        for(Iterator<Span> it = at.getEnclosed(EnumSet.allOf(SpanTypeEnum.class));it.hasNext();){
            jg.writeTree(writeSpan(it.next()));
        }
        jg.writeEndArray();
        jg.writeEndObject();
        jg.close();
    }

    private ObjectNode writeSpan(Span span) throws IOException {
        log.trace("wirte {}",span);
        ObjectNode jSpan = mapper.createObjectNode();
        jSpan.put("type", span.getType().name());
        jSpan.put("start", span.getStart());
        jSpan.put("end", span.getEnd());
        for(String key : span.getKeys()){
            List<Value<?>> values = span.getValues(key);
            if(values.size() == 1){
                jSpan.put(key, writeValue(values.get(0)));
            } else {
                ArrayNode jValues = jSpan.putArray(key);
                for(Value<?> value : values){
                    jValues.add(writeValue(value));
                }
                jSpan.put(key, jValues);
            }
        }
        log.trace(" ... {}",jSpan);
        return jSpan;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ObjectNode writeValue(Value<?> value) {
        ObjectNode jValue;
        Class<?> valueType = value.value().getClass();
        ValueTypeSerializer vts = valueTypeSerializerRegistry.getSerializer(valueType);
        if(vts != null){
            jValue = vts.serialize(mapper,value.value());
            //TODO assert that jValue does not define "class" and "prob"!
        } else { //use the default binding and the "data" field
            jValue = mapper.createObjectNode();
            jValue.put("value", mapper.valueToTree(value.value()));
        }
        jValue.put("class",valueType.getName());
        if(value.probability() != Value.UNKNOWN_PROBABILITY){
            jValue.put("prob", value.probability());
        }
        return jValue;
    }    
}
