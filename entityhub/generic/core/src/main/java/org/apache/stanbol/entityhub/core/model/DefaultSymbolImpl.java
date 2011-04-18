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

import java.util.Iterator;

import org.apache.stanbol.entityhub.core.utils.ToStringIterator;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Symbol Interface based on the parsed {@link Representation}.<br>
 * Specific implementations of the entity hub models might want to use this implementation
 * so that they need only to implement the {@link Representation} interface.
 * However implementations might also decides to provide its own implementation
 * of the {@link Symbol} as well as the other interfaces defined by the
 * entityhub model
 * @author Rupert Westenthaler
 *
 */
public class DefaultSymbolImpl extends DefaultSignImpl implements Symbol {

    private static final Logger log = LoggerFactory.getLogger(DefaultSymbolImpl.class);

    private String defaultLanguage = null;

    private static final String[] ALT_LABEL_LANGUAGES = new String[]{null,"en"};

    /**
     * Creates a Symbol Wrapper over the parsed representation
     * @param entityhubId the ID of the Entityhub that manages this Symbol
     * @param representation the representation holding the state of the symbol
     * @throws IllegalArgumentException If the parsed ID is <code>null</code> or
     * empty or the parsed Representation is <code>null</code>.
     */
    protected DefaultSymbolImpl(String entityhubId,Representation representation) throws IllegalArgumentException {
        super(entityhubId,representation);
        //checks no longer required,
//        if(getLabel() == null){
//            throw new IllegalArgumentException("Representation "+getId()+" does not define required field "+Symbol.LABEL);
//        }
//        if(getState() == null){
//            throw new IllegalArgumentException("Representation "+getId()+" does not define required field "+Symbol.STATE);
//        }
    }
    @Override
    public final void addDescription(String description) {
        representation.addNaturalText(Symbol.DESCRIPTION, description,defaultLanguage);
    }

    @Override
    public final void addDescription(String description, String lanugage) {
        representation.addNaturalText(Symbol.DESCRIPTION, description,lanugage);
    }

    @Override
    public final void addPredecessor(String predecessor) {
        representation.addReference(Symbol.PREDECESSOR, predecessor);
    }

    @Override
    public final void addSuccessor(String successor) {
        representation.addReference(Symbol.SUCCESSOR, successor);

    }

    @Override
    public final Iterator<Text> getDescriptions() {
        return representation.getText(Symbol.DESCRIPTION);
    }

    @Override
    public final Iterator<Text> getDescriptions(String lang) {
        return representation.get(Symbol.DESCRIPTION, lang);
    }

    @Override
    public final String getLabel() {
        String label = getLabel(defaultLanguage);
        if(label == null){ //no label for the default language
            //search labels in other languages
            Text altLabel = representation.getFirst(Symbol.LABEL, ALT_LABEL_LANGUAGES);
            if(altLabel == null){
                Iterator<Text> labels = representation.getText(Symbol.LABEL);
                if(labels.hasNext()){
                    altLabel = labels.next();
                }
            }
            return altLabel!=null?altLabel.getText():null;
        } else {
            return label;
        }
    }

    @Override
    public final String getLabel(String lang) {
        Text label = representation.getFirst(Symbol.LABEL, lang);
        return label!=null?label.getText():null;
    }

    @Override
    public final Iterator<String> getPredecessors() {
        return new ToStringIterator(representation.get(Symbol.PREDECESSOR));
    }

    @Override
    public final SymbolState getState() {
        Reference stateUri = representation.getFirstReference(Symbol.STATE);
        if(stateUri != null){
            if(SymbolState.isState(stateUri.getReference())){
                return SymbolState.getState(stateUri.getReference());
            } else {
                log.warn("Value {} for field {} is not a valied SymbolState! -> return null",
                    stateUri,Symbol.STATE);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public final Iterator<String> getSuccessors() {
        return new ToStringIterator(representation.get(Symbol.SUCCESSOR));
    }

    @Override
    public final boolean isPredecessors() {
        return getPredecessors().hasNext();
    }

    @Override
    public final boolean isSuccessor() {
        return getSuccessors().hasNext();
    }

    @Override
    public final void removeDescription(String description) {
        representation.removeNaturalText(Symbol.DESCRIPTION,description,defaultLanguage);
    }

    @Override
    public final void removeDescription(String description, String language) {
        representation.removeNaturalText(Symbol.DESCRIPTION,description,language);
    }

    @Override
    public final void removePredecessor(String predecessor) {
        representation.removeReference(Symbol.PREDECESSOR, predecessor);
    }

    @Override
    public final void removeSuccessor(String successor) {
        representation.removeReference(Symbol.SUCCESSOR, successor);
    }

    @Override
    public final void setLabel(String label) {
        representation.setNaturalText(Symbol.LABEL, label, defaultLanguage);
    }

    @Override
    public final void setLabel(String label, String language) {
        representation.setNaturalText(Symbol.LABEL, label, language);
    }

    @Override
    public final void setState(SymbolState state) throws IllegalArgumentException {
        if(state != null){
            representation.setReference(Symbol.STATE, state.getUri());
        } else {
            throw new IllegalArgumentException("SymbolState can not be set to NULL!");
        }
    }
    /**
     * Getter for the default language used for {@link #getLabel()}
     * @return the preferred language used for {@link #getLabel()}
     */
    public final String getDefaultLanguage() {
        return defaultLanguage;
    }
    /**
     * Setter for the default language used for {@link #getLabel()} (
     * <code>null</code> is supported)
     * @param defaultLanguage the preferred language used for {@link #getLabel()} 
     */
    public final void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

}
