/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.entityhub.model.sesame;

import java.util.Iterator;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * Set of {@link Transformer}s used to convert Sesame {@link Value}s to
 * Entityhub model instances.
 * 
 * @author Rupert Westenthaler
 *
 */
public final class ModelUtils {

    /**
     * Restrict instantiation
     */
    private ModelUtils() {}

   /**
     * Transforms typed literals with datatype {@link XMLSchema#STRING} to
     * {@link Text} instances as required by some {@link Representation}
     * methods. This transformer is usually used in front of the
     * {@link ModelUtils#VALUE_TRANSFORMER}.<p>
     * <b>NOTE</b> that input values other as String literals are NOT transformed!
     */
    protected static Transformer STRING_LITERAL_TO_TEXT_TRANSFORMER = new Transformer() {
        
        @Override
        public Object transform(Object input) {
            if(input instanceof Literal && XMLSchema.STRING.equals(((Literal)input).getDatatype())){
                return new RdfText((Literal)input);
            }
            return input;
        }
    };
    /**
     * A {@link Value} to {@link Object} transformer intended to be used for
     * {@link IteratorUtils#transformedIterator(Iterator, Transformer)} to
     * convert 
     */
    public static Transformer VALUE_TRANSFORMER = new Transformer() {
        
        @Override
        public Object transform(Object input) {
            if(input instanceof Value){
                Value sesameValue = (Value) input;
                if(sesameValue instanceof URI){
                    return new RdfReference((URI)sesameValue);
                } else if(sesameValue instanceof Literal){
                    Literal literal = (Literal)sesameValue;
                    if(literal.getDatatype() == null){ //TODO: adapt to RDF1.1
                        return new RdfText(literal);
                    } else {
                        return ModelUtils.transformTypedLiteral(literal);
                    }
                } else {
                    return new RdfBNode((BNode)sesameValue);
                }
            } else { //do not transform objects of other types (incl. null)
                return input;
            }
        }    
    };
    /**
     * Transforms Sesmae {@link Value}s to {@link String}
     * Emits {@link Value#stringValue()}
     */
    public static Transformer VALUR_TO_STRING_TRANSFORMER = new Transformer() {
        
        @Override
        public String transform(Object input) {
            return ((Value)input).stringValue();
        }
    };
    /**
     * Transforms a typed literal to the according java type.
     * @param literal
     * @return
     */
    private static Object transformTypedLiteral(Literal literal){
        URI dataType = literal.getDatatype();
        if(XMLSchema.INT.equals(dataType)){
            return literal.intValue();
        } else if(XMLSchema.LONG.equals(dataType)){
            return literal.longValue();
        } else if(XMLSchema.FLOAT.equals(dataType)){
            return literal.floatValue();
        } else if(XMLSchema.DOUBLE.equals(dataType)){
            return literal.doubleValue();
        } else if(XMLSchema.BOOLEAN.equals(dataType)){ 
            return literal.booleanValue();
        }else if(XMLSchema.INTEGER.equals(dataType)){
            return literal.integerValue();
        } else if(XMLSchema.DECIMAL.equals(dataType)){
            return literal.decimalValue();
        } else if(XMLSchema.STRING.equals(dataType)){ //explicit handle string
            //to avoid going to a lot of equals checks
            return literal.stringValue();
        } else if(XMLDatatypeUtil.isCalendarDatatype(dataType)){
            return literal.calendarValue().toGregorianCalendar().getTime();
        } else if(XMLSchema.BYTE.equals(dataType)){
            return literal.byteValue();
        } else if(XMLSchema.SHORT.equals(dataType)){
            return literal.shortValue();
        //Start with the more exotic types at the end (for performance reasons)
        } else if(XMLSchema.NON_NEGATIVE_INTEGER.equals(dataType) ||
                XMLSchema.NON_POSITIVE_INTEGER.equals(dataType) ||
                XMLSchema.NEGATIVE_INTEGER.equals(dataType) ||
                XMLSchema.POSITIVE_INTEGER.equals(dataType)){
            return literal.longValue();
        } else if(XMLSchema.GDAY.equals(dataType) ||
                XMLSchema.GMONTH.equals(dataType) ||
                XMLSchema.GMONTHDAY.equals(dataType) ||
                XMLSchema.GYEAR.equals(dataType) ||
                XMLSchema.GYEARMONTH.equals(dataType)){
            return literal.calendarValue().toGregorianCalendar().getTime();
        } else if(XMLSchema.UNSIGNED_BYTE.equals(dataType)){
            return literal.shortValue();
        } else if(XMLSchema.UNSIGNED_SHORT.equals(dataType)){
            return literal.intValue();
        } else if(XMLSchema.UNSIGNED_INT.equals(dataType)){
            return literal.longValue();
        } else{
            return literal.stringValue();
        }
    }

}
