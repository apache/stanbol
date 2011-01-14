package eu.iksproject.rick.servicesapi.model;

import java.net.URI;
import java.net.URL;

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
     * @throws IllegalArgumentException If the parsed instance type is supported, but
     * the parsed instance can not be used to create a text instance
     * @throws NullPointerException If <code>null</code> is parsed
     */
    Text createText(Object value) throws UnsupportedTypeException, IllegalArgumentException,NullPointerException;
    /**
     * Creates a Text instance for a language
     * @param text the text
     * @param language the language or <code>null</code>.
     * @return the Text instance
     * @throws IllegalArgumentException if the parsed parameter can not be used to create a Text instance
     * @throws NullPointerException if <code>null</code> is parsed as text.
     */
    Text createText(String text,String language) throws IllegalArgumentException, NullPointerException;
    /**
     * Creates a reference instance for the parsed value. Implementations might
     * support special support for specific classes. As an default the
     * {@link Object#toString()} method is used to get the unicode representation
     * of the reference.<p>
     * Implementation MUST support at least the following types:<ul>
     * <li> {@link String}: The parsed string need not be be checked for a valid
     *      IRI, URI or URL in any form. However in case of an empty String a 
     *      {@link IllegalArgumentException} MUST BE thrown)
     * <li> {@link URI}: Any valid URI MUST BE accepted
     * <li> {@link URL}: any valid URL MUST BE accepted
     * </ul>
     * @param value the unicode representation of the reference
     * @return the reference instance
     * @throws UnsupportedTypeException if the type of the parsed object can
     * not be converted to a Reference.
     * @throws IllegalArgumentException if the parsed value can not be used to
     * create a valid Reference (e.g. when parsing an empty String)
     * @throws NullPointerException if the parsed value is <code>null</code>
     */
    Reference createReference(Object value) throws UnsupportedTypeException, IllegalArgumentException, NullPointerException;
    /**
     * Creates an empty representation instance of with the type {@link SignTypeEnum#Sign}
     * for the parsed ID. The id MUST not be <code>null</code> nor empty
     * @param id The id of the representation
     * @return the representation
     * @throws NullPointerException If <code>null</code> is parsed as ID
     * @throws IllegalArgumentException if an empty string is parsed as ID
     */
    Representation createRepresentation(String id) throws NullPointerException, IllegalArgumentException;
//    /**
//     * Creates a value of the parsed data type for the parsed object
//     * @param dataTypeUri the data type of the created object
//     * @param value the source object
//     * @return the value or <code>null</code> if the parsed value can not be
//     * converted.
//     * @throws UnsupportedTypeException if the type of the parsed object is
//     * not compatible to the requested dataType
//     * @throws UnsupportedDataTypeException Implementation need to ensure support
//     * for data types specified in {@link DataTypeEnum}. However implementations
//     * may support additional data types. When using such types one needs to
//     * check for this Exception.
//     */
//    public Object createValue(String dataTypeUri,Object value) throws UnsupportedTypeException, UnsupportedDataTypeException;
}
