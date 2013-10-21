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

import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

/**
 * A {@link Text} implementation backed by a Sesame {@link Literal}
 * @author Rupert Westenthaler
 *
 */
public class RdfText implements Text, RdfWrapper {
    private final Literal literal;

    protected RdfText(Literal literal) {
        this.literal = literal;
    }

    @Override
    public String getLanguage() {
        return literal.getLanguage();
    }

    @Override
    public String getText() {
        return literal.getLabel();
    }
    /**
     * The wrapped Sesame {@link Literal}
     * @return the Literal
     */
    public Literal getLiteral() {
        return literal;
    }
    @Override
    public Value getValue() {
        return literal;
    }
    
    @Override
    public int hashCode() {
        return literal.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Text && 
                getText().equals(((Text)obj).getText())){
            //check the language
            String l1 = literal.getLanguage();
            String l2 = ((Text)obj).getLanguage();
            if(l1 == null){
                return l2 == null;
            } else {
                return l1.equalsIgnoreCase(l2);
            }
        } else {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return literal.toString();
    }
    
}