package org.apache.stanbol.entityhub.yard.solr.model;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;

/**
 * This class is used to define index types. It is only used to provide an unique
 * ID and an human readable name for such types.<p>
 * This type is necessary, because we assume that indices only support string
 * values. Therefore any java type needs to be converted to its string
 * representation. This type is used to preserve the type information. <p>
 * How types are encoded within the index depends on the actual full text index
 * used. The functionality of mapping of this index data types to the actual
 * types as supported by the index is provided by the used {@link FieldMapper}
 * implementation.<p>
 * It is suggested to use the XSD Datatypes as id's for instances where suitable.
 * see <a herf=http://www.w3.org/TR/xmlschema-2/#built-in-datatypes> XSD built in
 * datatypes</a> for more information.
 *
 * @author Rupert Westenthaler
 *
 */
public final class IndexDataType {
    /**
     * The default index type is defined as xsd:string (http://www.w3.org/2001/XMLSchema/string)
     */
    public static final IndexDataType DEFAULT = new IndexDataType(NamespaceEnum.xsd+"string");
    /**
     * Prefix used by this type
     */
    private final String id;
    /**
     * Suffix used by this type
     */
    private final String name;
    public IndexDataType(String id) {
        this(id,null);
    }
    /**
     * Creates a new index data type, by defining it's id and name. The id MUST NOT
     * be <code>null</code> nor empty. If the name is <code>null</code>, than the
     * name is generated based on the id by searching the last index of '#', '/' or
     * ':'.
     * @param id the unique id used for this data type. Values MUST NOT be
     * <code>null</code> nor empty.
     * @param name the name for this data type.
     *
     */
    public IndexDataType(String id,String name) {
        this.id = id;
        if(name == null){
            if(id.lastIndexOf("#")>=0){
                name = id.substring(id.lastIndexOf("#")+1);
            } else if(id.lastIndexOf("/")>=0){
                name = id.substring(id.lastIndexOf("/")+1);
            } else if(id.lastIndexOf(":")>=0){
                name = id.substring(id.lastIndexOf(":")+1);
            } else {
                name = id;
            }
            //convert first char to lower case
            name = name.substring(0,1).toLowerCase()+name.substring(1);
        }
        this.name = name;
    }
    /**
     * Getter for the prefix
     * @return the prefix used by this type or <code>null</code> if this
     * type does not use a prefix.
     */
    public final String getId(){
        return id;
    }
    /**
     * Getter for the name
     * @return the name of this dataType
     */
    public final String getName(){
        return name;
    }
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof IndexDataType && ((IndexDataType)obj).id.equals(id);
    }
    /**
     * Returns the id of the dataType. Use {@link #getName()} if you need a
     * short variant
     * @return the id of the dataType
     */
    @Override
    public String toString() {
        return name;
    }
}
