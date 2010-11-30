package eu.iksproject.rick.servicesapi.model;

import eu.iksproject.rick.servicesapi.model.Representation.RepresentationTypeEnum;

/**
 * FactoryInterface for {@link Text} and {@link Reference} instances
 * TODO: Still not sure if we need that
 * @author Rupert Westenthaler
 */
public interface ValueFactory {
	/**
	 * Creates a Text instance without an language
	 * @param value The value if the text. Implementations might support special
	 * support for specific classes. As an default the {@link Object#toString()}
	 * method is used to get the lexical form of the text from the parsed value
	 * and <code>null</code> should be used as language. 
	 * @return the Text instance for the parsed object
	 */
	public Text createText(Object value);
	/**
	 * Creates a Text instance for a language
	 * @param text the text
	 * @param language the language or <code>null</code>.
	 * @return the Text instance
	 */
	public Text createText(String text,String language);
	/**
	 * Creates a reference instance for the parsed value. Implementations might 
	 * support special support for specific classes. As an default the 
	 * {@link Object#toString()} method is used to get the unicode representation
	 * of the reference.
	 * @param value the unicode representation of the reference
	 * @return the reference instance
	 */
	public Reference createReference(Object value);
	/**
	 * Creates an empty representation instance of with the type {@link RepresentationTypeEnum#Entity}
	 * for the parsed ID
	 * @param id The id of the representation
	 * @return the representation
	 */
	public Representation createRepresentation(String id);
	/**
	 * Creates an empty representation instance
	 * @param id The id
	 * @param type The type. If <code>null</code> the type is set to {@link RepresentationTypeEnum#Entity} 
	 * @return the representation
	 */
	public Representation createRepresentation(String id,RepresentationTypeEnum type);
}