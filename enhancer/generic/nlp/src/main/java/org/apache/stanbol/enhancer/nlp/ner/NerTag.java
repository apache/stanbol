package org.apache.stanbol.enhancer.nlp.ner;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.nlp.model.tag.Tag;

public class NerTag extends Tag<NerTag> {

    private UriRef type;
    
    public NerTag(String tag) {
        super(tag);
    }
    public NerTag(String tag,UriRef type) {
        super(tag);
        this.type = type;
    }

    /**
     * The <code>dc:type</code> of the Named Entity
     * @return the <code>dc:type</code> of the Named Entity
     * as also used by the <code>fise:TextAnnotation</code>
     */
    public UriRef getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return type == null ? super.toString() : 
            String.format("%s %s (type: %s)", getClass().getSimpleName(),tag,type);
    }
    
}
