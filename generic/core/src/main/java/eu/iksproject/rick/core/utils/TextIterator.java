package eu.iksproject.rick.core.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import eu.iksproject.rick.servicesapi.model.Text;
import eu.iksproject.rick.servicesapi.model.ValueFactory;

public class TextIterator implements Iterator<Text> {
    protected final Iterator<?> it;
    private Text next;
    protected final Set<String> languages;
    private final boolean isNullLanguage;
    protected final ValueFactory valueFactory;
    /**
     * Creates an instance that iterates over values and returns {@link Text}
     * instances that confirm to the active languages. If no languages are parsed
     * or <code>null</code> is parsed as a language, this Iterator also creates
     * and returns {@link Text} instances for {@link String} values.
     * @param valueFactory the factory used to create text instances for String values
     * @param it the iterator
     * @param languages The active languages or no values to accept all languages
     */
    public TextIterator(ValueFactory valueFactory,Iterator<Object> it,String...languages){
        if(it == null){
            throw new IllegalArgumentException("Parsed iterator MUST NOT be NULL!");
        }
        if(valueFactory == null){
            throw new IllegalArgumentException("Parsed ValueFactory MUST NOT be NULL!");
        }
        this.it = it;
        this.valueFactory = valueFactory;
        if(languages != null && languages.length>0){
            this.languages = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(languages)));
            isNullLanguage = this.languages.contains(null);
        } else {
            this.languages = null;
            isNullLanguage = true;
        }
        //init next ...
        next = prepareNext();
    }
    @Override
    public final void remove() {
        it.remove();

    }

    @Override
    public final Text next() {
        Text current = next;
        next = prepareNext();
        return current;
    }

    @Override
    public final boolean hasNext() {
        return next != null;
    }
    protected Text prepareNext(){
        Object check;
        while(it.hasNext()){
            check = it.next();
            if(check instanceof Text){
                Text text = (Text)check;
                if(languages == null || languages.contains(text.getLanguage())){
                    return text;
                }
            } else if(isNullLanguage && check instanceof String){
                return valueFactory.createText((String)check);
            } //type does not fit -> ignore
        }
        //no more element and still nothing found ... return end of iteration
        return null;
    }
}
