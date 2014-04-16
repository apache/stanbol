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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.test.model.RepresentationTest;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.model.vocabulary.XMLSchema;


public class RdfRepresentationTest extends RepresentationTest {

    protected RdfValueFactory valueFactory;

    
    @Before
    public void init(){
        this.valueFactory = RdfValueFactory.getInstance();
    }

    @Override
    protected Object getUnsupportedValueInstance() {
        return null; //indicates that all kinds of Objects are supported!
    }
    
    @Override
    protected ValueFactory getValueFactory() {
        return valueFactory;
    }
    /*--------------------------------------------------------------------------
     * Additional Tests for special Features of the Clerezza based implementation
     * 
     * This includes mainly support for additional types like PlainLiteral,
     * TypedLiteral, UriRefs. The conversion to such types as well as getter for
     * such types.
     *--------------------------------------------------------------------------
     */
    /**
     * {@link PlainLiteral} is used for natural language text in the Clerezza
     * RDF API. This tests if adding {@link PlainLiteral}s to the
     * {@link Representation#add(String, Object)} method makes them available
     * as {@link Text} instances via the {@link Representation} API (e.g. 
     * {@link Representation#get(String, String...)}).
     */
    @Test
    public void testPlainLiteralToTextConversion(){
        String field = "urn:test.RdfRepresentation:test.field";
        Literal noLangLiteral = valueFactory.getSesameFactory().createLiteral("A plain literal without Language");
        Literal enLiteral = valueFactory.getSesameFactory().createLiteral("An english literal","en");
        Literal deLiteral = valueFactory.getSesameFactory().createLiteral("Ein Deutsches Literal","de");
        Literal deATLiteral = valueFactory.getSesameFactory().createLiteral("Ein Topfen Verband hilft bei Zerrungen","de-AT");
        Collection<Literal> plainLiterals = Arrays.asList(noLangLiteral,enLiteral,deLiteral,deATLiteral);
        Representation rep = createRepresentation(null);
        rep.add(field, plainLiterals);
        //now test, that the Plain Literals are available as natural language
        //tests via the Representation Interface!
        //1) one without a language
        Iterator<Text> noLangaugeTexts = rep.get(field, (String)null);
        assertTrue(noLangaugeTexts.hasNext());
        Text noLanguageText = noLangaugeTexts.next();
        assertEquals(noLangLiteral.getLabel(), noLanguageText.getText());
        assertNull(noLanguageText.getLanguage());
        assertFalse(noLangaugeTexts.hasNext()); //only a single result
        //2) one with a language
        Iterator<Text> enLangaugeTexts = rep.get(field, "en");
        assertTrue(enLangaugeTexts.hasNext());
        Text enLangageText = enLangaugeTexts.next();
        assertEquals(enLiteral.getLabel(), enLangageText.getText());
        assertEquals(enLiteral.getLanguage(), enLangageText.getLanguage());
        assertFalse(enLangaugeTexts.hasNext());//only a single result
        //3) test to get all natural language values
        Set<String> stringValues = new HashSet<String>();
        for(Literal plainLiteral : plainLiterals){
            stringValues.add(plainLiteral.getLabel());
        }
        Iterator<Text> texts = rep.getText(field);
        while(texts.hasNext()){
            assertTrue(stringValues.remove(texts.next().getText()));
        }
        assertTrue(stringValues.isEmpty());
    }
    /**
     * {@link TypedLiteral}s are used to represent literal values for different
     * xsd dataTypes within Clerezza. This method tests of {@link TypedLiteral}s
     * with the data type xsd:string are correctly treated like {@link String}
     * values. This tests especially if they are treated as natural language
     * texts without language.
     */
    @Test
    public void testTypedLiteralToTextConversion(){
        String field = "urn:test.RdfRepresentation:test.field";
        Literal stringLiteral = valueFactory.getSesameFactory().createLiteral("This is a stirng value", XMLSchema.STRING);
        //also add an integer to test that other typed literals are not used as texts
        Literal integerLiteral = valueFactory.getSesameFactory().createLiteral(5);
        Representation rep = createRepresentation(null);
        rep.add(field, Arrays.asList(stringLiteral,integerLiteral));
        //test if the literal is returned when asking for natural language text without language
        Iterator<Text> noLangTexts = rep.get(field, (String)null);
        assertTrue(noLangTexts.hasNext());
        assertEquals(stringLiteral.getLabel(), noLangTexts.next().getText());
        assertFalse(noLangTexts.hasNext());
        //test that string literals are returned when asking for all natural language text values
        Iterator<Text> texts = rep.getText(field);
        assertTrue(texts.hasNext());
        assertEquals(stringLiteral.getLabel(), texts.next().getText());
        assertFalse(texts.hasNext());
    }
    /**
     * {@link TypedLiteral}s are used to represent literal values for different
     * xsd dataTypes within Clerezza. This method tests if xsd dataTypes are
     * converted to the corresponding java types. 
     * This is dependent on the {@link LiteralFactory} implementation used by
     * the {@link RdfRepresentation} implementation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTypedLiteralToValueConversion(){
        String field = "urn:test.RdfRepresentation:test.field";
        Integer integerValue = 5;
        Literal integerLiteral = valueFactory.getSesameFactory().createLiteral(integerValue);
        Date dateValue = new Date();
        Literal dateLiteeral = valueFactory.getSesameFactory().createLiteral(dateValue);
        Double doubleValue = Math.PI;
        Literal doubleLiteral = valueFactory.getSesameFactory().createLiteral(doubleValue);
        String stringValue = "This is a string literal value";
        Literal stringLiteral = valueFactory.getSesameFactory().createLiteral(stringValue,XMLSchema.STRING);
        Representation rep = createRepresentation(null);
        Collection<Literal> typedLiterals = 
            Arrays.asList(integerLiteral,doubleLiteral,stringLiteral,dateLiteeral);
        rep.add(field, typedLiterals);
        
        //now check that such values are available via Sesame Literal
        Iterator<Literal> typedLiteralValues = rep.get(field, Literal.class);
        int size = 0;
        while(typedLiteralValues.hasNext()){
            Literal next = typedLiteralValues.next();
            assertTrue(typedLiterals.contains(next));
            size++;
        }
        assertTrue(typedLiterals.size() == size);
        
        //now check that the values are available via the java object types
        //1) integer
        Iterator<Integer> intValues = rep.get(field, Integer.class);
        assertTrue(intValues.hasNext());
        assertEquals(integerValue, intValues.next());
        assertFalse(intValues.hasNext());
        //2) double
        Iterator<Double> doubleValues = rep.get(field, Double.class);
        assertTrue(doubleValues.hasNext());
        assertEquals(doubleValue, doubleValues.next());
        assertFalse(doubleValues.hasNext());
        //3) string
        Iterator<String> stringValues = rep.get(field, String.class);
        assertTrue(stringValues.hasNext());
        String value = stringValues.next();
        assertEquals(stringValue, value);
        assertFalse(stringValues.hasNext());
        //4) date
        Iterator<Date> dateValues = rep.get(field,Date.class);
        assertTrue(dateValues.hasNext());
        assertEquals(dateValue, dateValues.next());
        assertFalse(dateValues.hasNext());
    }
    /**
     * Test for STANBOL-1301
     */
    @Test
    public void testBNodeFiltering(){
        URI concept = new URIImpl("http://example.org/mySkos#Concept123");
        Representation r = createRepresentation(concept.stringValue());
        assertTrue(r instanceof RdfRepresentation);
        RdfRepresentation rep = (RdfRepresentation)r;
        //add the example as listed in STANBOL-1301 to directly to the
        //Sesame Model backing the created Representation
        Model m  = rep.getModel();
        m.add(concept,RDF.TYPE,SKOS.CONCEPT);
        m.add(concept,DCTERMS.IDENTIFIER, new LiteralImpl("123"));
        m.add(concept, SKOS.PREF_LABEL, new LiteralImpl("Concept123","en"));
        
        BNode note1 = new BNodeImpl("5d8580be71044a88bcfe9852d1e9cfb6node17c4j452vx19576");
        m.add(concept, SKOS.SCOPE_NOTE, note1);
        m.add(note1, DCTERMS.CREATOR, new LiteralImpl("User1"));
        m.add(note1, DCTERMS.CREATED, new LiteralImpl("2013-03-03T02:02:02Z",XMLSchema.DATETIME));
        m.add(note1, RDFS.COMMENT, new LiteralImpl("The scope of this example global","en"));

        BNode note2 = new BNodeImpl("5d8580be71044a88bcfe9852d1e9cfb6node17c4j452vx19634");
        m.add(concept, SKOS.SCOPE_NOTE, note2);
        m.add(note2, DCTERMS.CREATOR, new LiteralImpl("User2"));
        m.add(note2, DCTERMS.CREATED, new LiteralImpl("2013-03-03T04:04:04Z",XMLSchema.DATETIME));
        m.add(note2, RDFS.COMMENT, new LiteralImpl("Der Geltungsbereich ist Global","de"));
        
        //now assert that BNodes are not reported via the Representation API
        Iterator<Object> scopeNotes = rep.get(SKOS.SCOPE_NOTE.stringValue());
        assertFalse(scopeNotes.hasNext());
        
        Iterator<Reference> scopeNoteRefs = rep.getReferences(SKOS.SCOPE_NOTE.stringValue());
        assertFalse(scopeNoteRefs.hasNext());
    }
    
    //TODO add tests for adding Integers, Doubles, ... and getting TypedLiterals
    public static void main(String[] args) {
        RdfRepresentationTest test = new RdfRepresentationTest();
        test.init();
        test.testTypedLiteralToValueConversion();
    }
}
