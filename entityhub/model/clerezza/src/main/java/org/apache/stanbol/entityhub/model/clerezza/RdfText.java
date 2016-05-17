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
package org.apache.stanbol.entityhub.model.clerezza;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.stanbol.entityhub.servicesapi.model.Text;

public class RdfText implements Text, Cloneable {
    private final Literal literal;
    private final boolean isPlain;

    protected RdfText(String text, String lang) {
        if(text == null){
            throw new IllegalArgumentException("The parsed text MUST NOT be NULL");
        } else if(text.isEmpty()){
            throw new IllegalArgumentException("Tha parsed Text MUST NOT be empty!");
        }
        if(lang != null && lang.isEmpty()){ //we need to avoid empty languages, because Clerezza don't like them!
            lang = null;
        }
        this.literal = new PlainLiteralImpl(text, lang != null ? new Language(lang) : null);
        this.isPlain = true;
    }

    protected RdfText(Literal literal) {
        this.literal = literal;
        this.isPlain = literal instanceof Literal;
    }

    @Override
    public String getLanguage() {
        return isPlain && 
            (literal).getLanguage() != null ? 
                (literal).getLanguage().toString() : null;
    }

    @Override
    public String getText() {
        return literal.getLexicalForm();
    }

    public Literal getLiteral() {
        return literal;
    }

    @Override
    public RdfText clone() {
        Language language = isPlain ? (literal).getLanguage() : null;
        return new RdfText(new PlainLiteralImpl(literal.getLexicalForm(), language));
    }

    @Override
    public int hashCode() {
        return literal.getLexicalForm().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Text && ((Text) obj).getText().equals(getText())) {
            return (getLanguage() == null && ((Text) obj).getLanguage() == null)
                    || (getLanguage() != null && getLanguage().equals(((Text) obj).getLanguage()));
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return literal.getLexicalForm();
    }
}
