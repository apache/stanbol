package org.apache.stanbol.entityhub.model.clerezza.impl;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.entityhub.core.utils.AdaptingIterator.Adapter;
import org.apache.stanbol.entityhub.model.clerezza.RdfResourceUtils;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.model.clerezza.RdfResourceUtils.XsdDataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the Resources (used to store field values in the Clerezza triple
 * store) back to values as defined by the {@link Representation} interface
 * @author Rupert Westenthaler
 *
 * @param <T> the type of the Resource that can be converted to values
 */
public class Resource2ValueAdapter<T extends Resource> implements Adapter<T, Object> {

    Logger log = LoggerFactory.getLogger(Resource2ValueAdapter.class);


    protected final LiteralFactory literalFactory = LiteralFactory.getInstance();

    RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    @Override
    public Object adapt(T value, Class<Object> type) {
        if(value instanceof UriRef){
            return valueFactory.createReference((UriRef)value);
        } else if(value instanceof PlainLiteral){
            return valueFactory.createText((Literal)value);
        } else if(value instanceof TypedLiteral){
            TypedLiteral literal = (TypedLiteral) value;
            if(literal.getDataType() == null){ //if no dataType is defined
                //return a Text without a language
                return valueFactory.createText(literal);
            } else {
                XsdDataTypeEnum mapping = RdfResourceUtils.XSD_DATATYPE_VALUE_MAPPING.get(literal.getDataType());
                if(mapping != null){
                    if(mapping.getMappedClass() != null){
                        return literalFactory.createObject(mapping.getMappedClass(), literal);
                    } else { //if no mapped class
                        //bypass the LiteralFactory and return the string representation
                        return literal.getLexicalForm();
                    }
                } else { //if dataType is not supported
                    /*
                     * this could indicate two things:
                     * 1) the SimpleLiteralFactory supports a new DataType and
                     *    because of that it creates Literals with this Type
                     * 2) Literals with this data type where created by other
                     *    applications.
                     * In the first case one need to update the enumeration. In
                     * the second case using the LexicalForm should be OK
                     * Rupert Westenthaler 2010.10.28
                     */
                    log.warn("Missing Mapping for DataType "+literal.getDataType().getUnicodeString()
                            +" -> return String representation");
                    return literal.getLexicalForm();
                }
            }
        } else {
            log.warn("Unsupported Resource Type "+value.getClass()+" -> return String by using the toString method");
            return value.toString();
        }
    }

}
