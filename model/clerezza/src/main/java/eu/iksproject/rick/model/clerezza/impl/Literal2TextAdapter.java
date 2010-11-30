package eu.iksproject.rick.model.clerezza.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.TypedLiteral;

import eu.iksproject.rick.core.utils.AdaptingIterator.Adapter;
import eu.iksproject.rick.model.clerezza.RdfResourceUtils;
import eu.iksproject.rick.model.clerezza.RdfValueFactory;
import eu.iksproject.rick.servicesapi.model.Text;
/**
 * This Adapter does two things:
 * <ol>
 * <li> It filters {@link Literal}s based on the languages parsed in the 
 *      constructor. If no languages are parsed, than all languages are accepted
 * <li> It converts {@link Literal}s to {@link Text}. Only {@link PlainLiteral}
 *      and {@link TypedLiteral} with an xsd data type present in the 
 *      {@link RdfResourceUtils#STRING_DATATYPES} are converted. All other literals are
 *      filtered (meaning that <code>null</code> is returned)
 * </ol>
 * The difference of this Adapter to the {@link LiteralAdapter} with the generic
 * type {@link Text} is that the LiteralAdapter can not be used to filter
 * literals based on there language.
 * @author Rupert Westenthaler
 *
 */
public class Literal2TextAdapter<T extends Literal> implements Adapter<T,Text> {

	/**
	 * Unmodifiable set of the active languages 
	 */
	protected final Set<String> languages;
	private final boolean containsNull;
	protected final RdfValueFactory valueFactory = RdfValueFactory.getInstance();
	/**
	 * Filters Literals in the parsed Iterator based on the parsed languages and
	 * convert matching Literals to Text
	 * @param it the iterator
	 * @param lang the active languages. If <code>null</code> or empty, all
	 * languages are active. If <code>null</code> is parsed as an element, that
	 * also Literals without a language are returned
	 */
	public Literal2TextAdapter(String...lang){
		if(lang != null && lang.length>0){
			this.languages = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(lang)));
			this.containsNull = languages.contains(null);
		} else{
			this.languages = null;
			this.containsNull = true;
		}
		//init the first element
	}
	@Override
	public Text adapt(T value, Class<Text> type) {
		if(value instanceof PlainLiteral){
			if(languages == null || languages.contains(((PlainLiteral) value).getLanguage())){
				return valueFactory.createText(value);
			} //else wrong language -> filter
		} else if(containsNull){
			/*
			 * if the null language is active, than we can also return
			 * "normal" literals (with no known language).
			 * But first we need to check the Datatype!
			 */
			return valueFactory.createText(value);
		} // else no language defined -> filter
		return null;
	}
}
