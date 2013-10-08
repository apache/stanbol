package org.apache.stanbol.entityhub.model.sesame;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.collections.Predicate;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * Hold the Java Object to xml schema mappings for Sesame. 
 * Used as {@link Predicate} for filtering Objects of statements. 
 * @author Rupert Westenthaler
 *
 * @param <T>
 */
class ValueTypeFilter<T> implements Predicate {

    private boolean referenceState;
    private boolean plainLiteralState;
    private Set<URI> xmlTypes;
    private Set<String> languages;
    private Class<? extends Value> sesameType;
    
    public ValueTypeFilter(String...languages) {
        if(languages == null || languages.length < 1){
            this.languages = null;
        } else if( languages.length == 1){
            String language = languages[0];
            if(language != null){
                language = language.toLowerCase(Locale.ROOT);
            }
            this.languages = Collections.singleton(language);
        } else {
            this.languages = new HashSet<String>();
            for(String language : languages){
                if(language != null){
                    language = language.toLowerCase(Locale.ROOT);
                }
                this.languages.add(language);
            }
        }
        this.plainLiteralState = true;
    }
    
    public ValueTypeFilter(Class<T> type) {
        if(type.equals(Text.class)){
            plainLiteralState = true;
        } else if (type.equals(Reference.class)){
            referenceState = true;
        } else if(Value.class.isAssignableFrom(type)){
            sesameType = type.asSubclass(Value.class);
        } else {
            xmlTypes = RdfValueFactory.JAVA_TO_XML_DATATYPE_MAPPINGS.get(type);
        }
    }
    
    @Override
    public boolean evaluate(Object object) {
        if(sesameType != null){
            return sesameType.isAssignableFrom(object.getClass());
        } else if(referenceState && object instanceof URI || object instanceof BNode){
            return true;
        } else if(object instanceof Literal){
            Literal literal = (Literal)object;
            //TODO: adapt to RDF1.1:
            if(plainLiteralState && ( literal.getDatatype() == null || 
                    XMLSchema.STRING.equals(literal.getDatatype())) && (
                    languages == null || languages.contains(literal.getLanguage()))){
                return true;
            } else if(!plainLiteralState && xmlTypes != null && literal.getDatatype() != null){
                return xmlTypes.contains(literal.getDatatype());
            } else { //wrong literal type
                return false;
            }
        } else { //wrong Value type or not a Value at all
            return false;
        }
    }
}