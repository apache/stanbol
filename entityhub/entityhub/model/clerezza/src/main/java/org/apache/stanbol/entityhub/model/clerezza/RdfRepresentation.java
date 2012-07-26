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

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.NoConvertorException;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.stanbol.entityhub.core.utils.AdaptingIterator;
import org.apache.stanbol.entityhub.core.utils.FilteringIterator;
import org.apache.stanbol.entityhub.core.utils.TypeSafeIterator;
import org.apache.stanbol.entityhub.model.clerezza.impl.Literal2TextAdapter;
import org.apache.stanbol.entityhub.model.clerezza.impl.LiteralAdapter;
import org.apache.stanbol.entityhub.model.clerezza.impl.NaturalTextFilter;
import org.apache.stanbol.entityhub.model.clerezza.impl.Resource2ValueAdapter;
import org.apache.stanbol.entityhub.model.clerezza.impl.UriRef2ReferenceAdapter;
import org.apache.stanbol.entityhub.model.clerezza.impl.UriRefAdapter;
import org.apache.stanbol.entityhub.model.clerezza.utils.Resource2StringAdapter;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.UnsupportedTypeException;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RdfRepresentation implements Representation{

    private static final Logger log = LoggerFactory.getLogger(RdfRepresentation.class);

    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    private final GraphNode graphNode;

    protected final GraphNode getGraphNode() {
        return graphNode;
    }

    protected RdfRepresentation(UriRef resource, TripleCollection graph) {
        this.graphNode = new GraphNode(resource, graph);
    }

    /**
     * Getter for the read only view onto the RDF data of this representation.
     *
     * @return The RDF graph of this Representation
     */
    public TripleCollection getRdfGraph(){
        return graphNode.getGraph();
    }

//    protected UriRef getRepresentationType(){
//        Iterator<UriRef> it = this.graphNode.getUriRefObjects(REPRESENTATION_TYPE_PROPERTY);
//        return it.hasNext()?it.next():null;
//    }
    @Override
    public void add(String field, Object value) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(value == null){
            throw new IllegalArgumentException("NULL values are not supported by Representations");
        }
        UriRef fieldUriRef = new UriRef(field);
        Collection<Object> values = new ArrayList<Object>();
        //process the parsed value with the Utility Method ->
        // this converts Objects as defined in the specification
        ModelUtils.checkValues(valueFactory, value, values);
        //We still need to implement support for specific types supported by this implementation
        for (Object current : values){
            if (current instanceof Resource){ //native support for Clerezza types!
                graphNode.addProperty(fieldUriRef, (Resource)current);
            } else if (current instanceof RdfReference){
                //treat RDF Implementations special to avoid creating new instances
                graphNode.addProperty(fieldUriRef, ((RdfReference) current).getUriRef());
            } else if (current instanceof Reference){
                graphNode.addProperty(fieldUriRef, new UriRef(((Reference) current).getReference()));
            } else if (current instanceof RdfText){
                //treat RDF Implementations special to avoid creating new instances
                graphNode.addProperty(fieldUriRef,((RdfText) current).getLiteral());
            } else if (current instanceof Text){
                addNaturalText(fieldUriRef, ((Text)current).getText(), ((Text)current).getLanguage());
            } else { //else add an typed Literal!
                addTypedLiteral(fieldUriRef, current);
            }
        }
    }

    private void addTypedLiteral(UriRef field, Object literalValue){
        Literal literal;
        try {
            literal = RdfResourceUtils.createLiteral(literalValue);
        } catch (NoConvertorException e){
            log.info("No Converter for value type "+literalValue.getClass()
                    +" (parsed for field "+field+") use toString() to get String representation");
            literal = RdfResourceUtils.createLiteral(literalValue.toString(), null);
        }
        graphNode.addProperty(field, literal);
    }
    @Override
    public void addReference(String field, String reference) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(reference == null){
            throw new IllegalArgumentException("NULL values are not supported by Representations");
        } else if (reference.isEmpty()) {
            throw new IllegalArgumentException("References MUST NOT be empty!");
        }
        graphNode.addProperty(new UriRef(field), new UriRef(reference));
    }
    @Override
    public void addNaturalText(String field, String text, String...languages) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(text == null){
            throw new IllegalArgumentException("NULL values are not supported by Representations");
        }
        this.addNaturalText(new UriRef(field), text, languages);
    }
    private void addNaturalText(UriRef field, String text, String...languages) {
        if(languages == null || languages.length == 0){
            languages = new String []{null};
        }
        for(String language : languages){
            graphNode.addProperty(field, RdfResourceUtils.createLiteral(text, language));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Iterator<T> get(String field, final Class<T> type) throws UnsupportedTypeException {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        UriRef fieldUriRef = new UriRef(field);
        if(Resource.class.isAssignableFrom(type)){ //native support for Clerezza types
            return new TypeSafeIterator<T>(graphNode.getObjects(fieldUriRef), type);
// NOTE: (Rupert Westenthaler 12.01.2011)
//     Converting everything to String is not an intended functionality. When
//     someone parsed String.class he rather assumes that he gets only string
//     values and not also string representations for Dates, Integer ...
//       
//        } else if(type.equals(String.class)){ //support to convert anything to String
//            return (Iterator<T>) new AdaptingIterator<Resource,String>(
//                    graphNode.getObjects(fieldUriRef),
//                    new Resource2StringAdapter<Resource>(),
//                    String.class);
        } else if(type.equals(URI.class) || type.equals(URL.class)){ //support for References
            return new AdaptingIterator<UriRef, T>(
                    graphNode.getUriRefObjects(fieldUriRef),
                    new UriRefAdapter<T>(),
                    type);
        } else if(Reference.class.isAssignableFrom(type)){
            return (Iterator<T>) new AdaptingIterator<UriRef,Reference>(
                    graphNode.getUriRefObjects(fieldUriRef),
                    new UriRef2ReferenceAdapter(),Reference.class);
        } else if(Text.class.isAssignableFrom(type)){
            return (Iterator<T>)new AdaptingIterator<Literal, Text>(
                    graphNode.getLiterals(fieldUriRef),
                    new Literal2TextAdapter<Literal>(),
                    Text.class);
        } else { //support for Literals -> Type conversions
            return new AdaptingIterator<Literal, T>(
                    graphNode.getLiterals(fieldUriRef),
                    new LiteralAdapter<Literal, T>(),
                    type);
        }
    }

    @Override
    public Iterator<Reference> getReferences(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        return new AdaptingIterator<UriRef,Reference>(
                graphNode.getUriRefObjects(new UriRef(field)),
                new UriRef2ReferenceAdapter(),Reference.class);
    }

    @Override
    public Iterator<Text> getText(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        return new AdaptingIterator<Literal, Text>(
                graphNode.getLiterals(new UriRef(field)),
                new Literal2TextAdapter<Literal>(),
                Text.class);
    }

    @Override
    public Iterator<Object> get(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        return new AdaptingIterator<Resource, Object>(graphNode.getObjects(new UriRef(field)),
                new Resource2ValueAdapter<Resource>(),Object.class);
    }

    @Override
    public Iterator<Text> get(String field, String...languages) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        return new AdaptingIterator<Literal, Text>(
                graphNode.getLiterals(new UriRef(field)),
                new Literal2TextAdapter<Literal>(languages),
                Text.class);
    }

    @Override
    public Iterator<String> getFieldNames() {
        return new AdaptingIterator<UriRef, String>(graphNode.getProperties(),
                new Resource2StringAdapter<UriRef>(), String.class);
    }

    @Override
    public <T> T getFirst(String field, Class<T> type) throws UnsupportedTypeException {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        Iterator<T> it = get(field,type);
        if(it.hasNext()){
            return it.next();
        } else {
            return null;
        }
    }

    @Override
    public Object getFirst(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        Iterator<Object> it = get(field);
        if(it.hasNext()){
            return it.next();
        } else {
            return null;
        }
    }
    @Override
    public Reference getFirstReference(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        Iterator<Reference> it = getReferences(field);
        return it.hasNext()?it.next():null;
    }
    @Override
    public Text getFirst(String field, String...languages) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(languages == null){
            log.debug("NULL parsed as languages -> replacing with \"new String []{null}\"" +
                    " -> assuming a missing explicit cast to (String) in the var arg");
            languages = new String []{null};
        }
        Iterator<Text> it = get(field,languages);
        if(it.hasNext()){
            return it.next();
        } else {
            return null;
        }
    }
    @Override
    public String getId() {
        return getNode().getUnicodeString();
    }
    /**
     * Getter for the UriRef representing the ID of this Representation.
     * @return The UriRef representing the ID of this Representation.
     */
    public UriRef getNode(){
        return (UriRef)graphNode.getNode();
    }

    @Override
    public void remove(String field, Object parsedValue) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(parsedValue == null){
            log.warn("NULL parsed as value in remove method for symbol "+getId()
                    +" and field "+field+" -> call ignored");
            return;
        }
        UriRef fieldUriRef = new UriRef(field);
        Collection<Object> removeValues = new ArrayList<Object>();
        
        ModelUtils.checkValues(valueFactory, parsedValue, removeValues);
        //We still need to implement support for specific types supported by this implementation
        for (Object current : removeValues){
            if (current instanceof Resource){ //native support for Clerezza types!
                graphNode.deleteProperty(fieldUriRef, (Resource)current);
            } else if (current instanceof RdfReference){
                //treat RDF Implementations special to avoid creating new instances
                graphNode.deleteProperty(fieldUriRef, ((RdfReference) current).getUriRef());
            } else if (current instanceof Reference){
                graphNode.deleteProperty(fieldUriRef, new UriRef(((Reference) current).getReference()));
            } else if (current instanceof RdfText){
                //treat RDF Implementations special to avoid creating new instances
                graphNode.deleteProperty(fieldUriRef,((RdfText) current).getLiteral());
            } else if (current instanceof Text){
                removeNaturalText(field,((Text)current).getText(),((Text)current).getLanguage());
            } else { //else add an typed Literal!
                removeTypedLiteral(fieldUriRef, current);
            }
        }
    }

    @Override
    public void removeReference(String field, String reference) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(reference == null){
            log.warn("NULL parsed as value in remove method for symbol "+getId()+" and field "+field+" -> call ignored");
        }
        graphNode.deleteProperty(new UriRef(field), new UriRef(reference));
    }
    protected void removeTypedLiteral(UriRef field, Object object){
        Literal literal;
        try{
            literal = RdfResourceUtils.createLiteral(object);
        } catch (NoConvertorException e){
            log.info("No Converter for value type "+object.getClass()
                    +" (parsed for field "+field+") use toString() Method to get String representation");
            literal = RdfResourceUtils.createLiteral(object.toString(), null);
        }
        graphNode.deleteProperty(field,literal);
    }
    @Override
    public void removeNaturalText(String field, String value, String... languages) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(value == null){
            log.warn("NULL parsed as value in remove method for symbol "+getId()+" and field "+field+" -> call ignored");
        }
        if(languages == null || languages.length == 0){ //null or no language
            //need to be interpreted as default language
            languages = new String []{null};
        }
        UriRef fieldUriRef = new UriRef(field);
        for(String language : languages){
            graphNode.deleteProperty(fieldUriRef,RdfResourceUtils.createLiteral(value, language));
            if(language == null){ //if the language is null
                //we need also try to remove a typed Literal with the data type
                //xsd:string and the parsed value!
                graphNode.deleteProperty(fieldUriRef,RdfResourceUtils.createLiteral(value));
            }
        }
    }
    @Override
    public void removeAll(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        graphNode.deleteProperties(new UriRef(field));
    }
    @Override
    public void removeAllNaturalText(String field, String... languages) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
//        if(languages == null || languages.length == 0){
//            languages = new String []{null};
//        }
        UriRef fieldUriRef = new UriRef(field);
        //get all the affected Literals
        Collection<Literal> toRemove = new ArrayList<Literal>();
        Iterator<Literal> it =  new FilteringIterator<Literal>(
                graphNode.getLiterals(fieldUriRef),
                new NaturalTextFilter(languages),Literal.class);
        while(it.hasNext()){
            toRemove.add(it.next());
        }
        for(Literal l : toRemove){
            graphNode.deleteProperty(fieldUriRef, l);
        }
    }

    @Override
    public void set(String field, Object value) {
        removeAll(field);
        if(value != null){
            add(field,value);
        }
    }

    @Override
    public void setReference(String field, String reference) {
        removeAll(field);
        if(reference != null){
            addReference(field, reference);
        }
    }

    @Override
    public void setNaturalText(String field, String text, String...languages) {
        removeAllNaturalText(field, languages);
        if(text != null){
            addNaturalText(field, text, languages);
        }
    }
    
    @Override
    public String toString() {
        return RdfRepresentation.class.getSimpleName()+getId();
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
