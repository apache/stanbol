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
package org.apache.stanbol.enhancer.nlp.json.valuetype.impl;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value={ValueTypeParser.class,ValueTypeSerializer.class})
@Property(name=ValueTypeParser.PROPERTY_TYPE, value=NerTagSupport.TYPE_VALUE)
public class NerTagSupport implements ValueTypeParser<NerTag>, ValueTypeSerializer<NerTag> {
    
    public static final String TYPE_VALUE = "org.apache.stanbol.enhancer.nlp.ner.NerTag";
    
    @Override
    public Class<NerTag> getType() {
        return NerTag.class;
    }
    @Override
    public NerTag parse(ObjectNode jValue, AnalysedText at) {
        JsonNode tag = jValue.path("tag");
        if(!tag.isTextual()){
            throw new IllegalStateException("Unable to parse NerTag. The value of the "
                +"'tag' field MUST have a textual value (json: "+jValue+")");
        }
        JsonNode uri = jValue.path("uri");
        if(uri.isTextual()){
            return new NerTag(tag.getTextValue(), new IRI(uri.getTextValue()));
        } else {
            return new NerTag(tag.getTextValue());
        }
    }

    @Override
    public ObjectNode serialize(ObjectMapper mapper, NerTag nerTag){
        ObjectNode jNerTag = mapper.createObjectNode();
        jNerTag.put("tag", nerTag.getTag());
        if(nerTag.getType() != null){
            jNerTag.put("uri", nerTag.getType().getUnicodeString());
        }
        return jNerTag;
    }
    
}
