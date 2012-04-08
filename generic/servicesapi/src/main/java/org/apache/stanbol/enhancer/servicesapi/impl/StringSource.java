package org.apache.stanbol.enhancer.servicesapi.impl;

import java.nio.charset.Charset;

import org.apache.stanbol.enhancer.servicesapi.ContentSource;

/**
 * Allows to use a String as a Source for Content.
 * @author Rupert Westenthaler
 *
 */
public class StringSource extends ByteArraySource implements ContentSource {
    //TODO: validate that parsed MediaTypes to NOT contain the charset parameter
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final String TEXT_PLAIN = "text/plain";
    /**
     * Creates a String source with the content type "text/plain".
     * @param value
     */
    public StringSource(String value) {
        this(value,null);
    }
    /**
     * Allows to creates a StringSource with an other media type that text/plain
     * @param value the value
     * @param mt the MediaType. Do not use the "charset" parameter as this will
     * be set to the internally used charset used to convert the parsed value
     * to an byte array.
     */
    public StringSource(String value, String mt){
        super(value == null ? null : value.getBytes(UTF8),
                (mt != null ? mt : TEXT_PLAIN)+"; charset="+UTF8.name());
    }
    /**
     * Allows to creates a StringSource with an other media type that text/plain
     * and an custom {@link Charset} used to encode the String
     * @param value the value
     * @param charset the charset or <code>null</code> to use the default
     * "UTF-8". To use the System default parse 
     * <code>{@link Charset#defaultCharset()}</code>
     * @param mt the MediaType. Do not use the "charset" parameter as this will
     * be set to the internally used charset used to convert the parsed value
     * to an byte array.
     */
    public StringSource(String value, Charset charset, String mt){
        super(value == null ? null : value.getBytes(charset == null ? UTF8 : charset),
            (mt != null ? mt : TEXT_PLAIN)+"; charset="+(charset == null ? 
                    UTF8 : charset).name());
    }
    

}
