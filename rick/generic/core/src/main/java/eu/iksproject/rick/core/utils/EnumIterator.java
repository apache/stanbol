package eu.iksproject.rick.core.utils;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnumIterator<T extends Enum<T>> implements Iterator<T> {

    private static final Logger log = LoggerFactory.getLogger(EnumIterator.class);

    protected final Iterator<String> it;
    protected final Class<T> type;
    private T next;
    public EnumIterator(Iterator<String> it,Class<T> enumClass) {
        this.it = it;
        this.type = enumClass;
        //init the first value
        this.next = prepareNext();
    }
    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public T next() {
        T current = next;
        next = prepareNext();
        return current;
    }

    @Override
    public void remove() {
        it.remove();
    }
    protected T prepareNext(){
        while(it.hasNext()){
            String currentString = it.next();
            try {
                return Enum.valueOf(type, currentString);
            } catch(IllegalArgumentException e){
                //ignore
                log.debug("Value "+currentString+" not part of enumeration "+type+" -> filter value",e);
            }
        }
        return null;
    }

}
