package org.apache.stanbol.enhancer.nlp.json.valuetype.impl;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
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
    public NerTag parse(ObjectNode jValue) {
        JsonNode tag = jValue.path("tag");
        if(!tag.isTextual()){
            throw new IllegalStateException("Unable to parse NerTag. The value of the "
                +"'tag' field MUST have a textual value (json: "+jValue+")");
        }
        JsonNode uri = jValue.path("uri");
        if(uri.isTextual()){
            return new NerTag(tag.getTextValue(), new UriRef(uri.getTextValue()));
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
