package eu.iksproject.rick.model.clerezza.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.PlainLiteral;

import eu.iksproject.rick.core.utils.AdaptingIterator.Adapter;
/**
 * TODO: refactor to use the {@link Adapter} interface
 * @author Rupert Westenthaler
 *
 */
public class NaturalLanguageLiteralIterator implements Iterator<Literal> {

	/**
	 * Unmodifiable set of the active languages 
	 */
	protected final Set<String> languages;
	private final boolean containsNull;
	protected final Iterator<Literal> it;
	protected Literal next;
	public NaturalLanguageLiteralIterator(Iterator<Literal> it, String...lang){
		if(it == null){
			throw new IllegalArgumentException("Parameter Iterator<Literal> MUST NOT be NULL!");
		}
		if(lang == null){
			throw new IllegalArgumentException("Parameter languages MUST NOT be NULL!");
		}
		if(lang.length==0){
			throw new IllegalArgumentException("At least one language MUST be present");
		}
		this.it = it;
		this.languages = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(lang)));
		containsNull = languages.contains(null);
		//init the first element
		next = prepareNext();
	}
	@Override
	public final boolean hasNext() {
		return next != null;
	}

	@Override
	public Literal next() {
		Literal current  = next;
		next = prepareNext();
		return current;
	}

	@Override
	public final void remove() {
		it.remove();
	}

	protected final Literal prepareNext(){
		Literal current;
		while(it.hasNext()){
			current = it.next();
			if(current instanceof PlainLiteral){
				if(languages.contains(((PlainLiteral) current).getLanguage())){
					return current;
				} //else wrong language -> filter
			} else if(containsNull){
				/*
				 * if the null language is active, than we can also return
				 * "normal" literals (with no known language)
				 */
				return current;
			} // else no language defined -> filter
		}
		return null; //no more elements -> return null
	}
}
