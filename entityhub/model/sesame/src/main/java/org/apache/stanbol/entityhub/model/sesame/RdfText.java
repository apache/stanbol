package org.apache.stanbol.entityhub.model.sesame;

import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

/**
 * A {@link Text} implementation backed by a Sesame {@link Literal}
 * @author Rupert Westenthaler
 *
 */
public class RdfText implements Text, RdfWrapper {
    private final Literal literal;

    protected RdfText(Literal literal) {
        this.literal = literal;
    }

    @Override
    public String getLanguage() {
        return literal.getLanguage();
    }

    @Override
    public String getText() {
        return literal.getLabel();
    }
    /**
     * The wrapped Sesame {@link Literal}
     * @return the Literal
     */
    public Literal getLiteral() {
        return literal;
    }
    @Override
    public Value getValue() {
        return literal;
    }
    
    @Override
    public int hashCode() {
        return literal.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Text && 
                getText().equals(((Text)obj).getText())){
            //check the language
            String l1 = literal.getLanguage();
            String l2 = ((Text)obj).getLanguage();
            if(l1 == null){
                return l2 == null;
            } else {
                return l1.equalsIgnoreCase(l2);
            }
        } else {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return literal.toString();
    }
    
}