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
package org.apache.stanbol.entityhub.core.model;

import java.io.Serializable;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;

public class InMemoryValueFactory implements ValueFactory {

    private static InMemoryValueFactory instance;

    public static InMemoryValueFactory getInstance(){
        if(instance == null){
            instance = new InMemoryValueFactory();
        }
        return instance;
    }
    protected InMemoryValueFactory(){
        super();
    }

    @Override
    public Reference createReference(Object value) {
        if(value == null){
            throw new IllegalArgumentException("The parsed value MUST NOT be NULL");
        }
        return new ReferenceImpl(value.toString());
    }

    @Override
    public Text createText(Object value) {
        if(value == null){
            throw new IllegalArgumentException("The parsed value MUST NOT be NULL");
        }
        return createText(value.toString(),null);
    }

    @Override
    public Text createText(String text, String language) {
        return new TextImpl(text,language);
    }

    protected static class ReferenceImpl implements Reference, Serializable,Cloneable{

        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 2571082550530948667L;
        private final String value;

        protected ReferenceImpl(String value) {
            super();
            if(value == null){
                throw new IllegalArgumentException("The value of the reference MUST NOT be NULL");
            }
            if(value.isEmpty()){
                throw new IllegalArgumentException("The value of the reference MUST NOT be empty");
            }
            this.value = value;
        }

        public final String getReference() {
            return value;
        }
        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ReferenceImpl && ((ReferenceImpl)obj).value.equals(value);
        }
        @Override
        public String toString() {
            return value;
        }
        @Override
        public ReferenceImpl clone() throws CloneNotSupportedException {
            return new ReferenceImpl(value);
        }
    }
    protected static class TextImpl implements Text, Serializable,Cloneable {

        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = -5646936810374934435L;

        private final String value;
        private final String language;
        protected TextImpl(String value) throws IllegalArgumentException {
            this(value,null);
        }
        protected TextImpl(String value, String language) throws IllegalArgumentException {
            super();
            if(value == null){
                throw new IllegalArgumentException("The value of the Text MUST NOT be NULL!");
            }
            this.value = value;
            this.language = language;
        }
        public final String getText() {
            return value;
        }
        public final String getLanguage() {
            return language;
        }
        @Override
        public int hashCode() {
            return value.hashCode()+(language!=null?language.hashCode():0);
        }
        @Override
        public String toString() {
            return value+(language!=null && !language.isEmpty() ? ('@'+language):"");
        }
        @Override
        public boolean equals(Object obj) {
            if(obj instanceof TextImpl && ((TextImpl)obj).value.equals(value)){
                if(((TextImpl)obj).language == null){
                    return language == null;
                } else {
                    return ((TextImpl)obj).language.equals(language);
                }
            } else {
                return false;
            }
        }
        @Override
        protected Object clone() throws CloneNotSupportedException {
            return new TextImpl(value, language);
        }
    }
    @Override
    public Representation createRepresentation(String id) {
        if (id == null){
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL!");
         } else if(id.isEmpty()){
             throw new IllegalArgumentException("The parsed id MUST NOT be empty!");
         } else {
             return new InMemoryRepresentation(id);
        }
    }
//    @Override
//    public Object createValue(String dataTypeUri, Object value) throws UnsupportedTypeException, UnsupportedDataTypeException {
//
//        return null;
//    }
}
