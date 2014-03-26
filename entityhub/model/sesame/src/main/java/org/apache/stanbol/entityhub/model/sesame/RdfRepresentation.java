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

import static org.apache.commons.collections.PredicateUtils.instanceofPredicate;
import static org.apache.commons.collections.PredicateUtils.notPredicate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.UnsupportedTypeException;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Representation} implementation backed by a Sesame {@link Model}
 * @author Rupert Westenthaler
 */
public class RdfRepresentation implements Representation, RdfWrapper {

    private Logger log = LoggerFactory.getLogger(RdfRepresentation.class);
    
    private URI subject;
    private final Model model;
    private final RdfValueFactory factory;
    private final org.openrdf.model.ValueFactory sesameFactory;
    
    /**
     * Emits {@link Statement#getObject()}
     */
    protected Transformer objectTransFormer = new Transformer() {
        
        @Override
        public Value transform(Object input) {
            return ((Statement)input).getObject();
        }
    };
    
    /**
     * Creates a {@link Representation} for the parsed subject. Data will be
     * added to the model.
     * @param subject the subject
     * @param model the model
     * @param factory the factory
     */
    protected RdfRepresentation(URI subject, Model model, RdfValueFactory factory){
        this.subject = subject;
        this.model = model;
        this.factory = factory;
        this.sesameFactory = factory.getSesameFactory();
    }
    
    @Override
    public void add(String field, Object value) throws IllegalArgumentException {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(value == null){
            throw new IllegalArgumentException("NULL values are not supported by Representations");
        }
        URI property = sesameFactory.createURI(field);
        Collection<Object> values = new ArrayList<Object>();
        //process the parsed value with the Utility Method ->
        // this converts Objects as defined in the specification
        ModelUtils.checkValues(factory, value, values);
        //We still need to implement support for specific types supported by this implementation
        for (Object current : values){
            if (current instanceof Value){ //native support for Sesame types!
                addValue(property, (Value)current);
            } else if (current instanceof RdfWrapper){
                //for Sesame RDF wrapper we can directly use the Value
                addValue(property,  ((RdfWrapper) current).getValue());
            } else if (current instanceof Reference){
                addValue(property, sesameFactory.createURI(((Reference) current).getReference()));
            } else if (current instanceof Text){
                addValue(property, sesameFactory.createLiteral(
                    ((Text)current).getText(), ((Text)current).getLanguage()));
            } else { //else add an typed Literal!
                addValue(property, createTypedLiteral(current));
            }
        }
    }
    /**
     * Converts a Java object to a Sesame typed Literal
     * @param value the java value
     * @return the Sesame literal
     * @throws IllegalArgumentException it the parsed object could not be
     * converted to a Sesame typed literal
     */
    private Literal createTypedLiteral(Object value){
        final Literal literal;
        if(value instanceof Number){
            Number n = (Number)value;
            if(value instanceof Integer){
                literal = sesameFactory.createLiteral(n.intValue());
            } else if(value instanceof Float){
                literal = sesameFactory.createLiteral(n.floatValue());
            } else if(value instanceof Long){
                literal = sesameFactory.createLiteral(n.longValue());
            } else if(value instanceof Double){
                literal = sesameFactory.createLiteral(n.doubleValue());
            } else if(value instanceof Short){
                literal = sesameFactory.createLiteral(n.shortValue());
            } else if(value instanceof Byte){
                literal = sesameFactory.createLiteral(n.byteValue());
            } else {
                literal = null;
            }
        } else if(value instanceof Boolean){
            literal = sesameFactory.createLiteral(((Boolean)value).booleanValue());
        } else if(value instanceof Date){
            literal = sesameFactory.createLiteral((Date)value);
        }  else if(value instanceof BigInteger){
            literal = sesameFactory.createLiteral(value.toString(),
                DataTypeEnum.Integer.getUri());
        }  else if(value instanceof BigDecimal){
            literal = sesameFactory.createLiteral(value.toString(),
                DataTypeEnum.Decimal.getUri());
        } else if(value instanceof XMLGregorianCalendar){
            literal = sesameFactory.createLiteral((XMLGregorianCalendar)value);
        } else if(value instanceof Duration){
            literal = sesameFactory.createLiteral(value.toString(),
                XMLSchema.DURATION);
        } else if(value instanceof String){ //String type literals
            literal = sesameFactory.createLiteral(value.toString(),
                XMLSchema.STRING);
        } else {
            literal = null;
        }
        if(literal == null){
            throw new IllegalArgumentException("Unable to convert value '" 
                + value + "' to a Sesame typed literal because the java type " 
                + value.getClass().getName() + " can not be mapped to an "
                + "XML DataType.");
        }
        return literal;
    }

    @Override
    public void addNaturalText(String field, String text, String... languages) throws IllegalArgumentException {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        if(text == null){
            throw new IllegalArgumentException("NULL values are not supported by Representations");
        }
        URI property = sesameFactory.createURI(field);
        if(languages == null || languages.length == 0){
            languages = new String []{null};
        }
        for(String language : languages){
            Literal value = sesameFactory.createLiteral(text, language);
            addValue(property, value);
        }
    }

    @Override
    public void addReference(String field, String reference) throws IllegalArgumentException {
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
        addValue(sesameFactory.createURI(field), sesameFactory.createURI(reference));
    }
    /**
     * Adds a value to a property and handles a possible 
     * {@link RepositoryException} while doing so
     * @param property
     * @param value
     * @throws IllegalStateException in case of a {@link RepositoryException}
     * while adding the value.
     */
    private void addValue(URI property, Value value) {
        model.add(subject, property, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Object> get(String field) throws IllegalArgumentException {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        URI property = sesameFactory.createURI(field);
        return IteratorUtils.transformedIterator(
            IteratorUtils.filteredIterator(
                IteratorUtils.transformedIterator(
                    model.filter(subject, property, null).iterator(), 
                    objectTransFormer), // get the object from the statement
                notPredicate(instanceofPredicate(BNode.class))),
            org.apache.stanbol.entityhub.model.sesame.ModelUtils.VALUE_TRANSFORMER); // transform the values
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterator<T> get(String field, final Class<T> type) throws UnsupportedTypeException {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        URI property = sesameFactory.createURI(field);
        //filter for values that are compatible with the parsed type
        Iterator<?> iterator = IteratorUtils.filteredIterator(
            IteratorUtils.transformedIterator(
                model.filter(subject, property, null).iterator(),
                objectTransFormer), // get the object from the statement
            new ValueTypeFilter<T>(type));
        if(!Value.class.isAssignableFrom(type)){
            //if the requested type is not a Sesame value, we need also to
            //transform results
            iterator = IteratorUtils.transformedIterator(
                iterator, // the already filtered values
                org.apache.stanbol.entityhub.model.sesame.ModelUtils.VALUE_TRANSFORMER); // need to be transformed
        }
        return (Iterator<T>)iterator; 
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Text> get(String field, String...languages) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        URI property = sesameFactory.createURI(field);
        return IteratorUtils.transformedIterator(
            IteratorUtils.transformedIterator(
                IteratorUtils.filteredIterator(
                    IteratorUtils.transformedIterator(
                        model.filter(subject, property, null).iterator(), 
                        objectTransFormer), // get the object from the statement
                    new ValueTypeFilter<Text>(languages)), //filter languages
                org.apache.stanbol.entityhub.model.sesame.ModelUtils.STRING_LITERAL_TO_TEXT_TRANSFORMER), //transform strings to Text
            org.apache.stanbol.entityhub.model.sesame.ModelUtils.VALUE_TRANSFORMER); //transform to Text instances
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<String> getFieldNames() {
        return (Iterator<String>)IteratorUtils.transformedIterator(
            model.predicates().iterator(), org.apache.stanbol.entityhub.model.sesame.ModelUtils.VALUR_TO_STRING_TRANSFORMER);
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
    public String getId() {
        return subject.stringValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Reference> getReferences(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        URI property = sesameFactory.createURI(field);
        return IteratorUtils.transformedIterator(
            IteratorUtils.filteredIterator(
                IteratorUtils.transformedIterator(
                    model.filter(subject, property, null).iterator(), 
                    objectTransFormer), // get the object from the statement
                new ValueTypeFilter<Reference>(Reference.class)), //filter references
            org.apache.stanbol.entityhub.model.sesame.ModelUtils.VALUE_TRANSFORMER); //transform to Text instances
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Text> getText(String field) {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        URI property = sesameFactory.createURI(field);
        return IteratorUtils.transformedIterator(
            IteratorUtils.transformedIterator(
                IteratorUtils.filteredIterator(
                    IteratorUtils.transformedIterator(
                        model.filter(subject, property, null).iterator(), 
                        objectTransFormer), // get the object from the statement
                    new ValueTypeFilter<Text>(Text.class)), //filter plain literals
                org.apache.stanbol.entityhub.model.sesame.ModelUtils.STRING_LITERAL_TO_TEXT_TRANSFORMER),
            org.apache.stanbol.entityhub.model.sesame.ModelUtils.VALUE_TRANSFORMER); //transform to Text instances
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
        URI property = sesameFactory.createURI(field);
        
        Collection<Object> values = new ArrayList<Object>();
        ModelUtils.checkValues(factory, parsedValue, values);
        for(Object value : values){
            if (value instanceof Value){ //native support for Sesame types!
                removeValue(property, (Value)value);
            } else if (value instanceof RdfWrapper){
                //for Sesame RDF wrapper we can directly use the Value
                removeValue(property,  ((RdfWrapper) value).getValue());
            } else if (value instanceof Reference){
                removeValue(property, sesameFactory.createURI(((Reference) value).getReference()));
            } else if (value instanceof Text){
                removeValue(property, sesameFactory.createLiteral(
                    ((Text)value).getText(), ((Text)value).getLanguage()));
            } else { //else add an typed Literal!
                removeValue(property, createTypedLiteral(value));
            }
        }
    }
    /**
     * Removes the value from the parsed property
     * @param property
     * @param value
     */
    private boolean removeValue(URI property, Value value){
        if(value != null){
            return model.remove(subject, property, value);
        } else {
            return false;
        }
    }
    
    @Override
    public void removeAll(String field) throws IllegalArgumentException {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        model.remove(subject, sesameFactory.createURI(field), null);
    }

    @Override
    public void removeAllNaturalText(String field, String... languages) throws IllegalArgumentException {
        if(field == null){
            throw new IllegalArgumentException("The parsed field MUST NOT be NULL");
        } else if(field.isEmpty()){
            throw new IllegalArgumentException("The parsed field MUST NOT be Empty");
        }
        ValueTypeFilter<Literal> vtf = new ValueTypeFilter<Literal>(languages);
        Iterator<Statement> statements = model.filter(
            subject, sesameFactory.createURI(field), null).iterator();
        while(statements.hasNext()){
            Statement statement = statements.next();
            if(vtf.evaluate(statement.getObject())){
                statements.remove();
            }
        }
    }

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
        URI property = sesameFactory.createURI(field);
        for(String language : languages){
            removeValue(property, sesameFactory.createLiteral(value, language));
            if(language == null){ //we need also to remove xsd:string labels
                removeValue(property, sesameFactory.createLiteral(value, XMLSchema.STRING));
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
        } else {
            removeValue(sesameFactory.createURI(field), sesameFactory.createURI(reference));
        }

    }

    @Override
    public void set(String field, Object value) throws IllegalArgumentException {
        removeAll(field);
        if(value != null){
            add(field,value);
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
    public void setReference(String field, String reference) {
        removeAll(field);
        if(reference != null){
            addReference(field, reference);
        }
    }
    /**
     * Getter for the Model used by this Representation <p>
     * Note that this model might also contain triples with other subjects as
     * the one used by this representation.
     * @return the model used by this representation.
     */
    public Model getModel() {
        return model;
    }
    
    public URI getURI() {
        return subject;
    }
    
    @Override
    public Value getValue() {
        return subject;
    }
    
    @Override
    public int hashCode() {
        return subject.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Representation && 
                getId().equals(((Representation)obj).getId());
    }
    @Override
    public String toString() {
        return subject.toString();
    }
    
}