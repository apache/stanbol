package eu.iksproject.rick.servicesapi.model;

import eu.iksproject.rick.servicesapi.model.Sign.SignTypeEnum;

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
	 * @throws UnsupportedTypeException if the type of the parsed object is not
	 * can not be used to create Text instances
	 */
	public Text createText(Object value) throws UnsupportedTypeException;
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
	 * @throws UnsupportedTypeException if the type of the parsed object can 
	 * not be converted to a Reference.
	 */
	public Reference createReference(Object value) throws UnsupportedTypeException;
	/**
	 * Creates an empty representation instance of with the type {@link SignTypeEnum#Sign}
	 * for the parsed ID
	 * @param id The id of the representation
	 * @return the representation
	 */
	public Representation createRepresentation(String id);
//	/**
//	 * Creates a value of the parsed data type for the parsed object
//	 * @param dataTypeUri the data type of the created object
//	 * @param value the source object
//	 * @return the value or <code>null</code> if the parsed value can not be
//	 * converted.
//	 * @throws UnsupportedTypeException if the type of the parsed object is
//	 * not compatible to the requested dataType
//	 * @throws UnsupportedDataTypeException Implementation need to ensure support
//	 * for data types specified in {@link DataTypeEnum}. However implementations
//	 * may support additional data types. When using such types one needs to
//	 * check for this Exception.
//	 */
//	public Object createValue(String dataTypeUri,Object value) throws UnsupportedTypeException, UnsupportedDataTypeException;
}