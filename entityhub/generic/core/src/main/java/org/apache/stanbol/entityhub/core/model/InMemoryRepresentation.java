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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.UnsupportedTypeException;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.util.TextIterator;
import org.apache.stanbol.entityhub.servicesapi.util.TypeSafeIterator;


public class InMemoryRepresentation implements Representation,Cloneable {

    //private final Logger log = LoggerFactory.getLogger(InMemoryRepresentation.class);
    private static ValueFactory valueFactory = new InMemoryValueFactory();

    private final Map<String,Object> representation;
    private final Map<String,Object> unmodRepresentation;
    private final String id;
    /**
     * creates a new InMemoryRepresentation for the parsed ID
     * @param id the id of the representation
     */
    protected InMemoryRepresentation(String id){
        this(id,null);
    }
    /**
     * Initialise a new InMemoryRepresenation with the parsed map. Note that the
     * parsed map is directly used to store the data. That means that callers
     * MUST keep in minds that changes to that map will influence the internal
     * state of this instance.<br>
     * The intension of this constructor is to allow also to define the actual
     * map implementation used to store the data.
     * If one also wants to directly parse data already contained within an
     * other representation one MUST first create deep copy of the according
     * map!
     * @param id the id for the Representation
     * @param representation the map used by this representation to store it's data
     */
    protected InMemoryRepresentation(String id, Map<String,Object> representation){
        if(id == null){
            throw new IllegalArgumentException("The id of a Representation instance MUST NOT be NULL!");
        }
        this.id = id;
        if(representation == null){
            this.representation = new HashMap<String, Object>();
        } else {
            this.representation = representation;
        }
        unmodRepresentation = Collections.unmodifiableMap(this.representation);
    }
    @SuppressWarnings("unchecked")
    @Override
    public void add(String field, Object parsedValue) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(parsedValue == null){
            throw new IllegalArgumentException("NULL values are not supported by Representations");
        }
        Collection<Object> newValues = new ArrayList<Object>();
        ModelUtils.checkValues(valueFactory, parsedValue, newValues);
        Object values = representation.get(field);
        if(values != null){
            if(values instanceof Collection<?>){
                ((Collection<Object>) values).addAll(newValues);
            } else {
                if(newValues.size() == 1 && values.equals(newValues.iterator().next())){
                    return; //do not create an collection of the current value equals the added
                }
                Collection<Object> collection = new HashSet<Object>();
                //reset the field to the collection
                representation.put(field, collection);
                //add the two values
                collection.add(values);
                collection.addAll(newValues);
            }
        } else {
            //also here do not add the collection if there is only one value!
            representation.put(field, newValues.size() == 1?newValues.iterator().next():newValues);
        }
    }

    @Override
    public void addNaturalText(String field, String text, String... languages) {
        if(text == null){
            throw new IllegalArgumentException("NULL was parsed for the text! NULL values are not supported by Representations");
        }
        if(languages == null || languages.length<1){ //if no language is parse add the default lanugage!
            add(field,valueFactory.createText(text, null));
        } else {
            for(String lang : languages){
                add(field,valueFactory.createText(text, lang));
            }
        }
    }

    @Override
    public void addReference(String field, String reference) {
        if(reference == null){
            throw new IllegalArgumentException("NULL values are not supported by Representations");
        }
        add(field, valueFactory.createReference(reference));
    }
    /**
     * Getter for the values of the field as Collections. If the field is not
     * present it returns an empty Collections!
     * @param field the field
     * @return A read only collection with the values of the field
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> getValuesAsCollection(String field){
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        Object value = representation.get(field);
        if(value == null){
            return Collections.emptySet();
        } else if(value instanceof Collection<?>){
            return (Collection<Object>)value;
        } else {
            return Collections.singleton(value);
        }
    }
    @Override
    public <T> Iterator<T> get(String field, Class<T> type) throws UnsupportedTypeException {
        Collection<Object> values = getValuesAsCollection(field);
        return new TypeSafeIterator<T>(values.iterator(), type);
    }

    @Override
    public Iterator<Object> get(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        return getValuesAsCollection(field).iterator();
    }
    @Override
    public Iterator<Text> getText(String field) {
        Collection<Object> values = getValuesAsCollection(field);
        return values != null?new TextIterator(valueFactory,values.iterator()):null;
    }

    @Override
    public Iterator<Text> get(String field, String... languages) {
        final Collection<Object> values = getValuesAsCollection(field);
        return new TextIterator(valueFactory,values.iterator(), languages);
    }

    @Override
    public Iterator<String> getFieldNames() {
        return unmodRepresentation.keySet().iterator();
    }

    @Override
    public <T> T getFirst(String field, Class<T> type) throws UnsupportedTypeException {
        Iterator<T> values = get(field,type);
        return values.hasNext()?values.next():null;
    }

    @Override
    public Object getFirst(String field) {
        Iterator<Object> values = get(field);
        return values.hasNext()?values.next():null;
    }

    @Override
    public Text getFirst(String field, String... languages) {
        Iterator<Text> values = get(field,languages);
        return values.hasNext()?values.next():null;
    }

    @Override
    public String getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void remove(String field, Object parsedValue) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        Collection<Object> removeValues = new ArrayList<Object>();
        ModelUtils.checkValues(valueFactory, parsedValue, removeValues);
        Object values = representation.get(field);
        if(values == null) {
            return;
        } else if(removeValues.contains(values)){
            //in case this field has a single value and this values is part of
            //the values to remove -> remove the whole field
            representation.remove(field);
        } else if(values instanceof Collection<?>){
            if(((Collection<Object>)values).removeAll(removeValues) && //remove all Elements
                    ((Collection<Object>)values).size()<2){ //if removed check for size
                if(((Collection<Object>)values).size()==1){
                    //only one element remaining -> replace the collection with a Object
                    representation.put(field, ((Collection<Object>)values).iterator().next());
                } else {
                    //if no element remains, remove the field
                    representation.remove(field);
                }
            }
        } //else ignore (single value for field && value not to be removed)
    }

    @Override
    public void removeAll(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        representation.remove(field);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeAllNaturalText(String field, String... languages) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        Object values = representation.get(field);
        if(values == null) {
            return;
        }
        if(values instanceof Collection<?>){
            int removed = 0;
            for(Iterator<Text> it = new TextIterator(valueFactory,
                    ((Collection<Object>)values).iterator(),
                    languages);it.hasNext();){
                it.next();//go to the next Element
                it.remove(); //and remove ist
                removed++;
            }
            if(removed>0){ //if some elements where removed
                //check if there is only a singe or no elements left for the field
                int size = ((Collection<Object>)values).size();
                if(size==1){
                    representation.put(field, ((Collection<Object>)values).iterator().next());
                } else if(size<1){
                    representation.remove(field);
                }
            }
        } else if(isNaturalLanguageValue(values, languages)){
            representation.remove(field);
        } //else there is a single value that does not fit -> nothing todo
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeNaturalText(String field, String text, String... languages) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        Object values = representation.get(field);
        if(values == null) {
            return;
        }
        if(values instanceof Collection<?>){
            int removed = 0;
            for(Iterator<Text> it = new TextIterator(valueFactory,
                    ((Collection<Object>)values).iterator(),
                    languages);it.hasNext();){
                Text label = it.next();//go to the next element
                if(text.equals(label.getText())){
                    it.remove();//and remove it
                    removed++;
                }
            }
            if(removed>0){ //if some elements where removed
                //check if there is only a singe or no elements left for the field
                int size = ((Collection<Object>)values).size();
                if(size==1){
                    representation.put(field, ((Collection<Object>)values).iterator().next());
                } else if(size<1){
                    representation.remove(field);
                }
            }
        } else if(text.equals(getNaturalLanguageValue(values, languages))){
            representation.remove(field);
        } //else there is a single value that does not fit -> nothing todo

    }

    @Override
    public void removeReference(String field, String reference) {
        try {
            remove(field,new URI(reference));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("parsed reference needs to be an valid URI",e);
        }
    }

    @Override
    public void set(String field, Object value) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        representation.remove(field);
        if(value != null){
            add(field,value);
        }

    }

    @Override
    public void setNaturalText(String field, String text, String... languages) {
        removeAllNaturalText(field, languages);
        if(text != null){
            addNaturalText(field, text, languages);
        }
    }

    @Override
    public void setReference(String field, String reference) {
        removeAll(field);
        if(reference != null){
            addReference(field, reference);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() throws CloneNotSupportedException {
        Map<String,Object> clone = new HashMap<String, Object>();
        for(Entry<String,Object> e : representation.entrySet()){
            if(e.getValue() instanceof HashSet<?>){
                clone.put(e.getKey(), ((HashSet<?>)e.getValue()).clone());
            } else if(e.getValue() instanceof Collection<?>){
                HashSet<Object> valuesClone = new HashSet<Object>();
                for(Iterator<Object> it = ((Collection<Object>)e.getValue()).iterator();it.hasNext();valuesClone.add(it.next()));
                clone.put(e.getKey(), valuesClone);
            } else {
                clone.put(e.getKey(), e.getValue());
            }
        }
        return new InMemoryRepresentation(id, clone);
    }
    @Override
    public Reference getFirstReference(String field) {
        Iterator<Reference> it = getReferences(field);
        return it.hasNext()?it.next():null;
    }
    @Override
    public Iterator<Reference> getReferences(String field) {
        Collection<Object> values = getValuesAsCollection(field);
        return new TypeSafeIterator<Reference>(values.iterator(), Reference.class);
    }
    protected static String getNaturalLanguageValue(Object check,Set<String> langSet,boolean isNullLanguage){
        if(check instanceof Text){
            Text text = (Text)check;
            if(langSet == null || langSet.contains(text.getLanguage())){
                return text.getText();
            } // else empty arrey -> filter
        } else if(isNullLanguage && check instanceof String){
            return (String)check;
        } //type does not fit -> ignore
        return null; //no label found
    }
    protected static String getNaturalLanguageValue(Object check,String...languages){
        Set<String> langSet;
        boolean isNullLanguage;
        if(languages != null && languages.length>1){
            langSet = new HashSet<String>(Arrays.asList(languages));
            isNullLanguage = langSet.contains(null);
        } else {
            langSet = null;
            isNullLanguage = true;
        }
        return getNaturalLanguageValue(check,langSet,isNullLanguage);
    }
    /**
     * @param check
     * @param languages
     * @return
     */
    protected static boolean isNaturalLanguageValue(Object check,String...languages){
        return getNaturalLanguageValue(check,languages) != null;
    }
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + getId();
    }
    @Override
    public int hashCode() {
        return getId().hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Representation && ((Representation)obj).getId().equals(getId());
    }
}
