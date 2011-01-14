/**
 *
 */
package eu.iksproject.rick.model.clerezza;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;

import eu.iksproject.rick.servicesapi.model.Text;

public class RdfText implements Text, Cloneable {
    private final Literal literal;
    private final boolean isPlain;

    protected RdfText(String text, String lang) {
        if(text == null){
            throw new NullPointerException("The parsed text MUST NOT be NULL");
        } else if(text.isEmpty()){
            throw new IllegalArgumentException("Tha parsed Text MUST NOT be empty!");
        }
        if(lang != null && lang.isEmpty()){ //we need to avoid empty languages, because Clerezza don't like them!
            lang = null;
        }
        this.literal = new PlainLiteralImpl(text, lang != null ? new Language(lang) : null);
        this.isPlain = true;
    }

    protected RdfText(Literal literal) {
        this.literal = literal;
        this.isPlain = literal instanceof PlainLiteral;
    }

    @Override
    public String getLanguage() {
        return isPlain && 
            ((PlainLiteral) literal).getLanguage() != null ? 
                ((PlainLiteral) literal).getLanguage().toString() : null;
    }

    @Override
    public String getText() {
        return literal.getLexicalForm();
    }

    public Literal getLiteral() {
        return literal;
    }

    @Override
    public Object clone() {
        Language language = isPlain ? ((PlainLiteral) literal).getLanguage() : null;
        return new RdfText(new PlainLiteralImpl(literal.getLexicalForm(), language));
    }

    @Override
    public int hashCode() {
        return literal.getLexicalForm().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Text && ((Text) obj).getText().equals(getText())) {
            return (getLanguage() == null && ((Text) obj).getLanguage() == null)
                    || (getLanguage() != null && getLanguage().equals(((Text) obj).getLanguage()));
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return literal.getLexicalForm();
    }
}
