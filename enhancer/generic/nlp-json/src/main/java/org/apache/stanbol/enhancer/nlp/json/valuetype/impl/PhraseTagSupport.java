package org.apache.stanbol.enhancer.nlp.json.valuetype.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value={ValueTypeParser.class,ValueTypeSerializer.class})
@Property(name=ValueTypeParser.PROPERTY_TYPE, value=PhraseTagSupport.TYPE_VALUE)
public class PhraseTagSupport implements ValueTypeParser<PhraseTag>, ValueTypeSerializer<PhraseTag> {

    private final Logger log = LoggerFactory.getLogger(PhraseTagSupport.class);
    
    public static final String TYPE_VALUE = "org.apache.stanbol.enhancer.nlp.ner.PhraseTag";

    
    @Override
    public Class<PhraseTag> getType() {
        return PhraseTag.class;
    }

    @Override
    public ObjectNode serialize(ObjectMapper mapper, PhraseTag value) {
        ObjectNode jTag = mapper.createObjectNode();
        jTag.put("tag",value.getTag());
        if(value.getCategory() != null){
            jTag.put("lc", value.getCategory().ordinal());
        }
        return jTag;
    }


    @Override
    public PhraseTag parse(ObjectNode jValue) {
        JsonNode tag = jValue.path("tag");
        if(!tag.isTextual()){
            throw new IllegalStateException("Unable to parse PhraseTag. The value of the "
                    +"'tag' field MUST have a textual value (json: "+jValue+")");
        }
        JsonNode jCat = jValue.path("lc");
        LexicalCategory lc = null;
        if(jCat.isTextual()){
            try {
                lc = LexicalCategory.valueOf(jCat.getTextValue());
            } catch (IllegalArgumentException e) {
                log.warn("Unable to parse category for PhraseTag from '" 
                        +jCat.getTextValue()+"' (will create with tag only)!",e);
            }
        } else if(jCat.isInt()){
            lc = LexicalCategory.values()[jCat.getIntValue()];
        } else if(!jCat.isMissingNode()){
            log.warn("Unable to parse category for PhraseTag from "+jCat
                +"(will create with tag only)");
        }
        return new PhraseTag(tag.getTextValue(),lc);
    }

}
