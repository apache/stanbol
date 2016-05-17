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
package org.apache.stanbol.entityhub.model.clerezza.impl;

import org.apache.clerezza.rdf.core.InvalidLiteralTypeException;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.NoConvertorException;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator.Adapter;
import org.apache.stanbol.entityhub.model.clerezza.RdfResourceUtils;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This Adapter supports:
 * <ul>
 * <li> String: Converts all Literal to there lexical form
 * <li> Text: Converts {@link PlainLiteral}s and {@link TypedLiteral}s with a
 * data type constrained in {@link RdfResourceUtils#STRING_DATATYPES} to Text instances
 * <li> Int, Long, IRI ... : Converts {@link TypedLiteral}s to the according
 * Java Object by using the Clerezza {@link LiteralFactory} (see {@link SimpleLiteralFactory})
 * </ul>
 *
 * @author Rupert Westenthaler
 *
 * @param <T> All types of Literals
 * @param <A> See above documentation
 */
public class LiteralAdapter<T extends Literal,A> implements Adapter<T, A> {

    private static Logger log = LoggerFactory.getLogger(LiteralAdapter.class);

    private LiteralFactory lf = LiteralFactory.getInstance();
    private RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    @SuppressWarnings("unchecked")
    @Override
    public final A adapt(T value, Class<A> type) {
// NOTE: (Rupert Westenthaler 12.01.2011)
//      Converting everything to String is not an intended functionality. When
//      someone parsed String.class he rather assumes that he gets only string
//      values and not also string representations for Dates, Integer ...
//      If someone needs this kind of functionality he can anyway use the 
//      the Resource2StringAdapter.
//        if(type.equals(String.class)){
//            return (A) value.getLexicalForm();
//        } else 
        if(Text.class.isAssignableFrom(type)){
            if(RdfResourceUtils.STRING_DATATYPES.contains(value.getDataType())){
                            return (A)valueFactory.createText(value);
            } else { //this Literal can not be converted to Text!
                log.warn("Literal of type"+value.getClass()+" are not supported by this Adapter");
                return null;
            }
        } else if(Literal.class.isAssignableFrom(value.getClass())){
            try {
                return lf.createObject(type, value);
            } catch (NoConvertorException e) {
                //This usually indicates a missing converter ... so log in warning
                log.warn("unable to convert "+value+" to "+type,e);
                return null;
            } catch (InvalidLiteralTypeException e) {
                //This usually indicated a wrong type of the literal so log in debug
                log.debug("unable to Literal "+value+" to the type"+type,e);
                return null;
            }
        } else {
            //indicates, that someone wants to convert non TypedLiterals to an
            //specific data type
            log.warn("Converting Literals without type information to types other " +
                    "String is not supported (requested type: "+type+")! -> return null");
            return null;
        }
    }
}
