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
package org.apache.stanbol.entityhub.test.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.apache.stanbol.entityhub.test.Utils.asCollection;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.UnsupportedTypeException;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.junit.Test;

public abstract class RepresentationTest {
    /**
     * Subclasses must implement this method and provide a {@link ValueFactory} instance that is used to test
     * the actual model implementation
     * 
     * @return the {@link ValueFactory} for the Entityhub model implementation to be tested
     */
    protected abstract ValueFactory getValueFactory();

    /**
     * Getter for an instance of an unsupported type. For add/set operation such values are converted to the
     * lexical form by using the {@link Object#toString()} method. For
     * {@link Representation#get(String, Class)} and {@link Representation#getFirst(String, Class)} an
     * {@link UnsupportedTypeException} need to be thrown when requesting values of the
     * {@link Object#getClass()} of the returned instance.
     * 
     * @return An instance of a class that is not supported by the tested {@link Representation}
     *         implementation or <code>null</code> if the tested implementation does support any type (this
     *         will deactivate such kind of tests).
     */
    protected abstract Object getUnsupportedValueInstance();

    /*--------------------------------------------------------------------------
     * Set of Tests that check if all Methods correctly throw an IllegalArgumentException
     * when parsing null as field!
     *  - important to prevent NULL fields within the Entityhub
     *--------------------------------------------------------------------------
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldAdd() {
        Representation rep = createRepresentation(null);
        rep.add(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldAddNaturalText() {
        Representation rep = createRepresentation(null);
        rep.addNaturalText(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldAddReference() {
        Representation rep = createRepresentation(null);
        rep.addReference(null, "urn:test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGet() {
        Representation rep = createRepresentation(null);
        rep.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGetDataType() {
        Representation rep = createRepresentation(null);
        rep.get(null, Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGetNaturalLanguateText() {
        Representation rep = createRepresentation(null);
        rep.get(null, "en");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGetFirst() {
        Representation rep = createRepresentation(null);
        rep.getFirst(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGetFirstReference() {
        Representation rep = createRepresentation(null);
        rep.getFirstReference(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGetFirstDataType() {
        Representation rep = createRepresentation(null);
        rep.getFirst(null, Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGetFirstNaturalLanguage() {
        Representation rep = createRepresentation(null);
        rep.getFirst(null, "en");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGetReferences() {
        Representation rep = createRepresentation(null);
        rep.getReferences(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldGetText() {
        Representation rep = createRepresentation(null);
        rep.getText(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldRemove() {
        Representation rep = createRepresentation(null);
        rep.remove(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldRemoveAll() {
        Representation rep = createRepresentation(null);
        rep.removeAll(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldRemoveAllNaturalText() {
        Representation rep = createRepresentation(null);
        rep.removeAllNaturalText(null, "de");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldRemoveNaturalText() {
        Representation rep = createRepresentation(null);
        rep.removeNaturalText(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldRemoveReference() {
        Representation rep = createRepresentation(null);
        rep.removeReference(null, "urn:test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldSet() {
        Representation rep = createRepresentation(null);
        rep.set(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldSetNaturalText() {
        Representation rep = createRepresentation(null);
        rep.setNaturalText(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullFieldSetReference() {
        Representation rep = createRepresentation(null);
        rep.setReference(null, "urn:test");
    }

    /*--------------------------------------------------------------------------
     * Set of Tests that check if all Methods correctly throw a IllegalArgumentExceptions
     * when parsing an empty string as field
     *  - important to prevent "" fields within the Entityhub
     *--------------------------------------------------------------------------
     */
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldAdd() {
        Representation rep = createRepresentation(null);
        rep.add("", "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldAddNaturalText() {
        Representation rep = createRepresentation(null);
        rep.addNaturalText("", "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldAddReference() {
        Representation rep = createRepresentation(null);
        rep.addReference("", "urn:test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGet() {
        Representation rep = createRepresentation(null);
        rep.get("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGetDataType() {
        Representation rep = createRepresentation(null);
        rep.get("", Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGetNaturalLanguateText() {
        Representation rep = createRepresentation(null);
        rep.get("", "en");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGetFirst() {
        Representation rep = createRepresentation(null);
        rep.getFirst("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGetFirstReference() {
        Representation rep = createRepresentation(null);
        rep.getFirstReference("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGetFirstDataType() {
        Representation rep = createRepresentation(null);
        rep.getFirst("", Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGetFirstNaturalLanguage() {
        Representation rep = createRepresentation(null);
        rep.getFirst("", "en");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGetReferences() {
        Representation rep = createRepresentation(null);
        rep.getReferences("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldGetText() {
        Representation rep = createRepresentation(null);
        rep.getText("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldRemove() {
        Representation rep = createRepresentation(null);
        rep.remove("", "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldRemoveAll() {
        Representation rep = createRepresentation(null);
        rep.removeAll("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldRemoveAllNaturalText() {
        Representation rep = createRepresentation(null);
        rep.removeAllNaturalText("", "de");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldRemoveNaturalText() {
        Representation rep = createRepresentation(null);
        rep.removeNaturalText("", "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldRemoveReference() {
        Representation rep = createRepresentation(null);
        rep.removeReference("", "urn:test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldSet() {
        Representation rep = createRepresentation(null);
        rep.set("", "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldSetNaturalText() {
        Representation rep = createRepresentation(null);
        rep.setNaturalText("", "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyFieldSetReference() {
        Representation rep = createRepresentation(null);
        rep.setReference("", "urn:test");
    }

    /**
     * Tests if value iterators for non existing fields return an Iterator with no elements (Here it is
     * important, that in such cases methods do not return <code>null</code>).
     */
    @Test
    public void testNonExistingFields() {
        String field = "urn:this.field:does.not:exist";
        // Iterators MUST NOT be NULL but MUST NOT contain any element
        Representation rep = createRepresentation(null);
        Iterator<String> fieldIt = rep.getFieldNames();
        assertNotNull(fieldIt);
        assertFalse(fieldIt.hasNext());
        Iterator<Object> valueIt = rep.get(field);
        assertNotNull(valueIt);
        assertFalse(valueIt.hasNext());
        Iterator<Reference> refIt = rep.getReferences(field);
        assertNotNull(refIt);
        assertFalse(refIt.hasNext());
        Iterator<Text> textIt = rep.get(field, (String[]) null);
        assertNotNull(textIt);
        assertFalse(textIt.hasNext());
    }

    /*--------------------------------------------------------------------------
     * Set of Tests that check if all add methods correctly throw an 
     * IllegalArgumentExceptions when parsing NULL as value
     *  - important to prevent NULL values within Entityhub
     *--------------------------------------------------------------------------
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddNullReference() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        rep.addReference(field, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullText() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        rep.addNaturalText(field, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullObject() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        rep.add(field, null);
    }

    /*--------------------------------------------------------------------------
     * Set of Tests that check if Methods correctly process UnsupportedTypes
     * This means that the toString Method is used to get the lexical
     * representation of such types
     *--------------------------------------------------------------------------
     */
    /**
     * Adding an unsupported type should use the {@link Object#toString()} to store the parsed instance
     */
    @Test
    public void testAddUnsupportedType() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        Object value = getUnsupportedValueInstance();
        if (value == null) { // any type is supported by the representation
            return; // this test is not needed
        }
        rep.add(field, value);
        Iterator<Object> valueIterator = rep.get(field);
        assertNotNull(valueIterator);
        assertTrue(valueIterator.hasNext());
        Object repValue = valueIterator.next();
        assertEquals(value.toString(), repValue.toString());
    }

    @Test
    public void testSetUnsupportedType() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        Object value = getUnsupportedValueInstance();
        if (value == null) { // any type is supported by the representation
            return; // this test is not needed
        }
        rep.set(field, value); // this does not test that set removes previous values
        Iterator<Object> valueIterator = rep.get(field);
        assertNotNull(valueIterator);
        assertTrue(valueIterator.hasNext());
        Object repValue = valueIterator.next();
        assertEquals(value.toString(), repValue.toString());
    }

    @Test(expected = UnsupportedTypeException.class)
    public void testGetValueWithUnsupportedType() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        Object value = getUnsupportedValueInstance();
        if (value == null) { // any type is supported by the representation
            // this test is not needed therefore return an dummy Exception
            throw new UnsupportedTypeException(Object.class,
                    "dummy exception to successfully complete this unnecessary test");
        } else {
            Class<?> unsupported = value.getClass();
            rep.get(field, unsupported);
        }
    }

    @Test(expected = UnsupportedTypeException.class)
    public void testGetFirstValueWithUnsupportedType() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        Object value = getUnsupportedValueInstance();
        if (value == null) { // any type is supported by the representation
            // this test is not needed therefore return an dummy Exception
            throw new UnsupportedTypeException(Object.class,
                    "dummy exception to successfully complete this unnecessary test");
        } else {
            Class<?> unsupported = value.getClass();
            rep.getFirst(field, unsupported);
        }
    }

    /**
     * If <code>null</code> is parsed as value for any setter method, than all values need to be removed for
     * the field. this means that <code>set**(field,null)</code> has the same effect as
     * <code>{@link Representation#removeAll(String) removeAll(field)} </code>
     */
    @Test
    public void testSetToNullAsRemoveAll() {
        String field = "urn:the.field:used.for.this.Test";
        String testReference = "http://www.test.org/test";
        String testText = "This is a Test";
        Integer testValue = 42;
        Representation rep = createRepresentation(null);
        rep.add(field, testValue);
        rep.addNaturalText(field, testText, "en");
        rep.addReference(field, testReference);
        rep.set(field, null); // need to remove all values
        Iterator<Object> values = rep.get(field);
        assertNotNull(values);
        assertFalse(values.hasNext());
        // test specific setter (also specific setters need to remove all values)
        rep.add(field, testValue);
        rep.addNaturalText(field, testText, "en");
        rep.addReference(field, testReference);
        rep.setNaturalText(field, null);
        assertNotNull(values);
        assertFalse(values.hasNext());

        rep.add(field, testValue);
        rep.addNaturalText(field, testText, "en");
        rep.addReference(field, testReference);
        rep.setReference(field, null);
        assertNotNull(values);
        assertFalse(values.hasNext());
    }

    @Test
    public void testFieldRemoval() throws URISyntaxException {
        String field = "urn:the.field:used.for.this.Test";
        ValueFactory vf = getValueFactory();
        Representation rep = createRepresentation(null);
        // Test removal for References
        String strRef = "urn:testValue";
        rep.addReference(field, strRef);
        assertTrue(asCollection(rep.getFieldNames()).contains(field));
        rep.removeReference(field, strRef);
        assertFalse(asCollection(rep.getFieldNames()).contains(field));

        Reference ref = vf.createReference("urn:testValue2");
        rep.add(field, ref);
        assertTrue(asCollection(rep.getFieldNames()).contains(field));
        rep.remove(field, ref);
        assertFalse(asCollection(rep.getFieldNames()).contains(field));

        // test removal for texts (with and without language)
        String strText = "test text";
        String strTextLang = "en";
        rep.addNaturalText(field, strText, strTextLang);
        assertTrue(asCollection(rep.getFieldNames()).contains(field));
        rep.removeNaturalText(field, strText, strTextLang);
        assertFalse(asCollection(rep.getFieldNames()).contains(field));

        String strTextNoLang = "test text without lang";
        rep.addNaturalText(field, strTextNoLang);
        assertTrue(asCollection(rep.getFieldNames()).contains(field));
        rep.removeNaturalText(field, strTextNoLang);
        assertFalse(asCollection(rep.getFieldNames()).contains(field));

        // there is also the possibility to explicitly parse null as language
        // could internally case differences however externally this is the same
        rep.addNaturalText(field, strTextNoLang, (String) null);
        assertTrue(asCollection(rep.getFieldNames()).contains(field));
        rep.removeNaturalText(field, strTextNoLang, (String) null);
        assertFalse(asCollection(rep.getFieldNames()).contains(field));

        Text text = vf.createText("Das ist ein Text zum testen des Text Objektes", "de");
        rep.add(field, text);
        assertTrue(asCollection(rep.getFieldNames()).contains(field));
        rep.remove(field, text);
        assertFalse(asCollection(rep.getFieldNames()).contains(field));

        // Test a dataTypes values
        Integer intValue = 42;
        rep.add(field, intValue);
        assertTrue(asCollection(rep.getFieldNames()).contains(field));
        rep.remove(field, intValue);
        assertFalse(asCollection(rep.getFieldNames()).contains(field));

        // Some Values are converted by the add(String field,Object value) Method
        // to other data types. This MUST also be assured for removal
        // NOTE: testing the conversions is done in other test methods!
        URI testURI = new URI("http://www.test.org/test");
        rep.add(field, testURI);
        assertTrue(asCollection(rep.getFieldNames()).contains(field));
        rep.remove(field, testURI);
        assertFalse(asCollection(rep.getFieldNames()).contains(field));
    }

    /**
     * Tests if {@link Reference} instances are correctly generated for {@link URI}. This test also depends on
     * the correct implementation of the {@link Reference#equals(Object)} method
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testURIToReferenceConversion() throws URISyntaxException {
        String field = "urn:the.field:used.for.this.Test";
        URI uri = new URI("http://www.test.org/uriTest");
        ValueFactory vf = getValueFactory();
        Representation rep = createRepresentation(null);
        // test conversion
        rep.add(field, uri);
        Iterator<Reference> refs = rep.getReferences(field);
        assertTrue(refs.hasNext());
        assertEquals(refs.next().getReference(), uri.toString());
        assertFalse(refs.hasNext());
        // test multiple adds do not generate duplicate References
        rep.add(field, uri);
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test adding a equivalent reference
        rep.add(field, vf.createReference(uri.toString()));
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test removing
        rep.remove(field, uri);
        assertFalse(rep.get(field).hasNext());
    }

    /**
     * Tests if {@link Reference} instances are correctly generated for {@link URL}. This test also depends on
     * the correct implementation of the {@link Reference#equals(Object)} method
     * 
     * @throws MalformedURLException
     */
    @Test
    public void testURLToReferenceConversion() throws MalformedURLException {
        String field = "urn:the.field:used.for.this.Test";
        URL url = new URL("http://www.test.org/urlTest");
        ValueFactory vf = getValueFactory();
        Representation rep = createRepresentation(null);
        // test empty reference
        Iterator<Reference> refs = rep.getReferences(field);
        assertFalse(refs.hasNext());
        // test conversion
        rep.add(field, url);
        refs = rep.getReferences(field);
        assertTrue(refs.hasNext());
        assertEquals(refs.next().getReference(), url.toString());
        assertFalse(refs.hasNext());
        // test multiple adds do not generate duplicate References
        rep.add(field, url);
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test adding a equivalent reference
        rep.add(field, vf.createReference(url.toString()));
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test removing
        rep.remove(field, url);
        assertFalse(rep.get(field).hasNext());

    }

    /**
     * Parsing a String Array with null as first element MUST NOT add a value (because null values are not
     * supported by {@link Representation}).
     */
    @Test
    public void testStringArrayWithNullTextConversion() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        rep.add(field, new String[] {null, "en"});
        assertFalse(rep.get(field).hasNext());
    }

    /**
     * Checks if {@link Text} instances are correctly generated for String[]. This test depends also on the
     * correct implementation of the {@link Text#equals(Object)} method
     */
    @Test
    public void testStringArrayToTextConversion() {
        String field = "urn:the.field:used.for.this.Test";
        ValueFactory vf = getValueFactory();
        Representation rep = createRepresentation(null);
        // test conversion of String[] with language as second element
        String[] textWithLang = new String[] {"Test text with language", "en"};
        rep.add(field, textWithLang);
        Iterator<Text> refs = rep.get(field, (String[]) null);
        assertTrue(refs.hasNext());
        Text test = refs.next();
        assertEquals(textWithLang[1], test.getLanguage());
        assertEquals(textWithLang[0], test.getText());
        assertFalse(refs.hasNext());
        // test multiple adds do not generate duplicate References
        rep.add(field, textWithLang);
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test adding a equivalent reference
        rep.add(field, vf.createText(textWithLang[0], textWithLang[1]));
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test removing
        rep.remove(field, textWithLang);
        assertFalse(rep.get(field).hasNext());

        // test conversion of String[] with only one element (default language)
        String[] textWithoutLang = new String[] {"Test text without language"};
        rep.add(field, textWithoutLang);
        refs = rep.get(field, (String[]) null);
        assertTrue(refs.hasNext());
        test = refs.next();
        assertNull(test.getLanguage());
        assertEquals(textWithoutLang[0], test.getText());
        assertFalse(refs.hasNext());
        // test multiple adds do not generate duplicate References
        rep.add(field, textWithoutLang);
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test adding a equivalent reference
        rep.add(field, vf.createText(textWithoutLang[0]));
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test removing
        rep.remove(field, textWithoutLang);
        assertFalse(rep.get(field).hasNext());

        // test conversion of String[] with null as second element (default language)
        String[] textWithDefaultLang = new String[] {"Test text with default language", null};
        rep.add(field, textWithDefaultLang);
        refs = rep.get(field, (String[]) null);
        assertTrue(refs.hasNext());
        test = refs.next();
        assertNull(test.getLanguage());
        assertEquals(textWithDefaultLang[0], test.getText());
        assertFalse(refs.hasNext());
        // test multiple adds do not generate duplicate References
        rep.add(field, textWithDefaultLang);
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test adding a equivalent reference
        rep.add(field, vf.createText(textWithDefaultLang[0], null));
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test removing
        rep.remove(field, textWithDefaultLang);
        assertFalse(rep.get(field).hasNext());

        // finally test if additional Elements are correctly ignored
        String[] ignoreAdditionalElements = new String[] {"Test if additional elements are ignored", "en",
                                                          "ignored1", "ignored2", null, "ignored4"};
        String[] sameText = new String[] {"Test if additional elements are ignored", "en"};
        rep.add(field, ignoreAdditionalElements);
        refs = rep.get(field, (String[]) null);
        assertTrue(refs.hasNext());
        test = refs.next();
        assertEquals(ignoreAdditionalElements[1], test.getLanguage());
        assertEquals(ignoreAdditionalElements[0], test.getText());
        assertFalse(refs.hasNext());
        // test multiple adds do not generate duplicate References
        rep.add(field, ignoreAdditionalElements);
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test if an Array with only the first two elements generate the same Text
        rep.add(field, sameText);
        assertTrue(asCollection(rep.get(field)).size() == 1);
        // test removing
        rep.remove(field, ignoreAdditionalElements);
        assertFalse(rep.get(field).hasNext());
    }

    @Test
    public void testMultipleAddAndRemove() throws MalformedURLException, URISyntaxException {
        String field = "urn:the.field:used.for.this.Test";
        ValueFactory vf = getValueFactory();
        Representation rep = createRepresentation(null);
        Reference ref = vf.createReference("http://www.test.org/test");
        Text text = vf.createText("test", "en");
        Integer i = 42;
        Double d = Math.PI;
        URI uri = new URI("http://www.test.org/uriTest");
        URL url = new URL("http://www.test.org/urlTest");
        String[] textAsArray = new String[] {"Test text as Array", "en"};
        Collection<Object> values = Arrays.asList(ref, text, i, d);
        Collection<Object> convertedValues = Arrays.asList((Object) url, uri, textAsArray);
        Collection<Object> allValues = Arrays.asList(ref, text, i, d, uri, url, textAsArray);
        // test adding of collections
        rep.add(field, values);
        assertTrue(asCollection(rep.get(field)).size() == 4);
        rep.remove(field, values);
        assertFalse(rep.get(field).hasNext());
        // test adding of Iterators
        rep.add(field, values.iterator());
        assertTrue(asCollection(rep.get(field)).size() == 4);
        rep.remove(field, values.iterator());
        assertFalse(rep.get(field).hasNext());
        // test adding of Enumerations
        Vector<Object> v = new Vector<Object>(values);
        rep.add(field, v.elements());
        assertTrue(asCollection(rep.get(field)).size() == 4);
        rep.remove(field, v.elements());
        assertFalse(rep.get(field).hasNext());
        // test adding and removing elements that need to be converted
        // only for collections this time -> want to test only converting is
        // applied for both add and remove
        rep.add(field, convertedValues);
        assertTrue(asCollection(rep.get(field)).size() == 3);
        rep.remove(field, convertedValues);
        assertFalse(rep.get(field).hasNext());
        // a final test to ensure, that remove does not only delete all values
        rep.add(field, allValues);
        assertTrue(asCollection(rep.get(field)).size() == 7);
        rep.remove(field, convertedValues);
        assertTrue(asCollection(rep.get(field)).size() == 4);
    }

    @Test
    public void testReferences() {
        String field = "urn:the.field:used.for.this.Test";
        Set<String> refs = new HashSet<String>(Arrays.asList("http://www.test.org/test1",
            "urn:test.org:test.1"));
        Representation rep = createRepresentation(null);
        for (String ref : refs) {
            rep.addReference(field, ref);
        }
        Iterator<Reference> refIterator = rep.getReferences(field);
        assertNotNull(refIterator);
        while (refIterator.hasNext()) {
            Reference ref = refIterator.next();
            assertTrue(refs.remove(ref.getReference()));
        }
        assertTrue(refs.isEmpty());
    }

    private static final String NL_TEST_string = "String value that has to be treated similar as texts with no language";
    private static final String NL_TEST_noLang = "kani ofie sfgoeyd";
    private static final String NL_TEST_en = "This is an English text";
    private static final String NL_TEST_en2 = "A second English text";
    private static final String NL_TEST_de = "Das ist ein Deutscher Text";
    private static final String NL_TEST_de_AT = "Saig kent ma bei uns a nu ois Deutsch durch geh loss'n";
    private static final Collection<String> NL_TEST_all = Arrays.asList(NL_TEST_string, NL_TEST_noLang,
        NL_TEST_en, NL_TEST_en2, NL_TEST_de, NL_TEST_de_AT);

    /**
     * Internally used to initialise the representation used for the various tests for natural language texts.
     * Tests using this initialisation method expect the current configuration. If one changes the values one
     * needs also do adapt the according tests.
     * 
     * @param field
     *            the field used to add the test values
     * @param valueSet
     *            if not <code>null</code> the added values are added to this set
     * @return the initialised representation
     */
    private Representation initNaturalLanguageTest(String field) {
        Representation rep = createRepresentation(null);
        rep.add(field, NL_TEST_string);
        rep.addNaturalText(field, NL_TEST_noLang);
        rep.addNaturalText(field, NL_TEST_en, "en");
        rep.addNaturalText(field, NL_TEST_en2, "en");
        rep.addNaturalText(field, NL_TEST_de, "de");
        rep.addNaturalText(field, NL_TEST_de_AT, "de-AT");
        return rep;
    }

    @Test
    public void testGetNaturalTextWithNoLanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // Note that also String values need to be converted to texts with no language
        Set<String> textSet = new HashSet<String>(NL_TEST_all);
        Text text;
        Iterator<Text> noLangTexts = rep.get(field, (String) null);
        assertNotNull(noLangTexts);
        while (noLangTexts.hasNext()) {
            text = noLangTexts.next();
            assertNull(text.getLanguage());
            assertTrue(textSet.remove(text.getText()));
        }
        assertTrue(textSet.size() == 4); // check that both text where found
    }

    @Test
    public void testGetNaturalTextWithLanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // test de texts
        Iterator<Text> deTexts = rep.get(field, "de");
        assertNotNull(deTexts);
        assertTrue(deTexts.hasNext()); // there is one German text in the test set
        Text text = deTexts.next();
        assertEquals(text.getLanguage(), "de"); // "de" lang
        assertEquals(text.getText(), NL_TEST_de); // the de lang text
        assertFalse(deTexts.hasNext());// only one Result
        // test en labels (2 results)
        Iterator<Text> enTexts = rep.get(field, "en");
        assertNotNull(enTexts);
        Set<String> textSet = new HashSet<String>(Arrays.asList(NL_TEST_en, NL_TEST_en2));
        while (enTexts.hasNext()) {
            text = enTexts.next();
            assertEquals("en", text.getLanguage());
            assertTrue(textSet.remove(text.getText())); // remove the found
        }
        assertTrue(textSet.isEmpty()); // all texts found
    }

    @Test
    public void testGetNaturalTextWithMultipleLanguages() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // test multiple language query
        Iterator<Text> allDeTexts = rep.get(field, "de", "de-AT");
        assertNotNull(allDeTexts);
        Set<String> textSet = new HashSet<String>(Arrays.asList(NL_TEST_de, NL_TEST_de_AT));
        while (allDeTexts.hasNext()) {
            Text text = allDeTexts.next();
            assertTrue(text.getLanguage().equalsIgnoreCase("de") || 
                text.getLanguage().equalsIgnoreCase("de-AT"));
            assertTrue(textSet.remove(text.getText())); // remove the found
        }
        assertTrue(textSet.isEmpty()); // all texts found
    }

    @Test
    public void testGetNaturalTextWithLanguagesWithoutValues() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // test not present language
        Iterator<Text> esTexts = rep.get(field, "es");
        assertNotNull(esTexts);
        assertFalse(esTexts.hasNext());
        // test multiple not present languages
        Iterator<Text> frItTexts = rep.get(field, "fr", "it");
        assertNotNull(frItTexts);
        assertFalse(frItTexts.hasNext());
    }

    @Test
    public void testGetNaturalTextWithAnyLanguageByParsingAnEmptyArray() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // test Iterator for any language (by parsing an empty list)
        Iterator<Text> allTexts = rep.get(field, new String[] {});
        assertNotNull(allTexts);
        assertTrue(asCollection(allTexts).size() == NL_TEST_all.size());
    }

    @Test
    public void testGetNaturalTextWithAnyLanguageByParsingNullAsArray() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // test Iterator for any language (by parsing null)
        Iterator<Text> allTexts = rep.get(field, (String[]) null);
        assertNotNull(allTexts);
        assertTrue(asCollection(allTexts).size() == NL_TEST_all.size());
    }

    @Test
    public void testRemoveNaturalTextWithWrongLanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // Test removal of natural language
        // remove a specific test, but wrong language -> no effect
        rep.removeNaturalText(field, NL_TEST_en2, "de");
        assertTrue(asCollection(rep.get(field)).size() == NL_TEST_all.size());
    }

    @Test
    public void testRemoveNaturalTextWithWrongNullLanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // remove a specific text, but with wrong null language -> also no effect
        rep.removeNaturalText(field, NL_TEST_de, (String) null);
        assertTrue(asCollection(rep.get(field)).size() == NL_TEST_all.size());
    }

    @Test
    public void testRemoveNaturalTextWithCorrectAndWrongLanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // remove a specific text, parse one correct and one wrong lang
        rep.removeNaturalText(field, NL_TEST_en2, "de", "en");
        Set<String> textSet = new HashSet<String>(NL_TEST_all);
        // remove all remaining values
        for (Iterator<Text> texts = rep.getText(field); texts.hasNext(); textSet.remove(texts.next()
                .getText()))
            ;
        // and check that the value we expect to be removed is still in the set
        assertTrue(textSet.size() == 1);
        assertTrue(textSet.contains(NL_TEST_en2));
    }

    /**
     * String values are treated the same as natural language values with the default (<code>null</code>)
     * language. <br>
     * Removing a natural language value with parsing null as language MUST therefore also remove a string
     * value with the parse same text.
     */
    @Test
    public void testRemoveStringValuesByRemovingNaturalLanguageTextsWithNullLanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        Set<String> textSet = new HashSet<String>(NL_TEST_all);

        // remove a string value by parsing the text and null as language
        rep.removeNaturalText(field, NL_TEST_string, (String) null);
        for (Iterator<Text> texts = rep.getText(field); texts.hasNext(); textSet.remove(texts.next()
                .getText()))
            ;
        assertTrue(textSet.size() == 1); // only one element should be removed
        assertTrue(textSet.remove(NL_TEST_string)); // and this should be the stringTest
    }

    /**
     * String values are treated the same as natural language values with the default (<code>null</code>)
     * language. <br>
     * Removing a natural language value with no defined language MUST therefore also remove a string value
     * with the parse same text.
     */
    @Test
    public void testRemoveStringValuesByRemovingNaturalLanguageTextsWithNoLanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        Set<String> textSet = new HashSet<String>(NL_TEST_all);
        // remove a string value by parsing only the text
        rep.removeNaturalText(field, NL_TEST_string);
        textSet.addAll(Arrays.asList(NL_TEST_string, NL_TEST_noLang, NL_TEST_en, NL_TEST_en2, NL_TEST_de,
            NL_TEST_de_AT));
        for (Iterator<Text> texts = rep.getText(field); texts.hasNext(); textSet.remove(texts.next()
                .getText()))
            ;
        assertTrue(textSet.size() == 1); // only one element should be removed
        assertTrue(textSet.remove(NL_TEST_string)); // and this should be the stringTest
    }

    /**
     * String values are treated the same as natural language values with the default (<code>null</code>)
     * language. <br>
     * Removing a natural language value with an empty language array MUST be interpreted as default language
     * and therefore remove the String value.
     */
    @Test
    public void testRemoveStringValuesByRemovingNaturalLanguageTextsWithEmptyLanguageArray() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        Set<String> textSet = new HashSet<String>(NL_TEST_all);
        // remove a string value by parsing an empty list of languages
        rep.removeNaturalText(field, NL_TEST_string);
        textSet.addAll(Arrays.asList(NL_TEST_string, NL_TEST_noLang, NL_TEST_en, NL_TEST_en2, NL_TEST_de,
            NL_TEST_de_AT));
        for (Iterator<Text> texts = rep.getText(field); texts.hasNext(); textSet.remove(texts.next()
                .getText()))
            ;
        assertTrue(textSet.size() == 1); // only one element should be removed
        assertTrue(textSet.remove(NL_TEST_string)); // and this should be the stringTest
        rep.add(field, NL_TEST_string); // re add the value for further tests
    }

    /**
     * Tests the feature to add one and the same natural language text for multiple languages
     */
    public void testAddNaturalLanguageTextForMultipleLanguages() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        // add the same label for multiple language
        String[] languageArray = new String[] {"en", "de", null};
        rep.addNaturalText(field, NL_TEST_noLang, languageArray);
        Set<String> languages = new HashSet<String>(Arrays.asList(languageArray));
        Iterator<Text> texts = rep.get(field, (String[]) null);
        while (texts.hasNext()) {
            Text text = texts.next();
            assertTrue(languages.remove(text.getLanguage()));
            assertEquals(NL_TEST_noLang, text.getText());
        }
        assertTrue(languages.isEmpty());
    }

    /**
     * Tests the feature to remove one and the same natural language text for multiple languages
     */
    public void testRemoveNaturalLanguageValueInMultipleLanguages() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = createRepresentation(null);
        // add the same label for multiple languages
        rep.addNaturalText(field, NL_TEST_noLang, "en", "de", null);
        rep.removeNaturalText(field, NL_TEST_noLang, "en", null);
        Iterator<Text> texts = rep.get(field, (String[]) null);
        assertTrue(texts.hasNext());
        Text text = texts.next();
        assertFalse(texts.hasNext());
        assertEquals("de", text.getLanguage());
        assertEquals(NL_TEST_noLang, text.getText());
    }

    @Test
    public void testRemoveAllTextsOfALanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        Set<String> textSet = new HashSet<String>(NL_TEST_all);
        // remove all texts of a specific language
        rep.removeAllNaturalText(field, "en");
        for (Iterator<Text> texts = rep.getText(field); texts.hasNext(); textSet.remove(texts.next()
                .getText()))
            ;
        assertTrue(textSet.size() == 2);
        assertTrue(textSet.remove(NL_TEST_en2));
        assertTrue(textSet.remove(NL_TEST_en));
    }

    @Test
    public void testRemoveAllTextsOfMultipleLanguages() {
        // remove all texts of multiple languages
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        Set<String> textSet = new HashSet<String>(NL_TEST_all);
        rep.removeAllNaturalText(field, "de", "de-AT");
        for (Iterator<Text> texts = rep.getText(field); 
                texts.hasNext(); 
                textSet.remove(texts.next().getText()));
        assertTrue(textSet.size() == 2);
        assertTrue(textSet.remove(NL_TEST_de));
        assertTrue(textSet.remove(NL_TEST_de_AT));
    }

    @Test
    public void testRemoveAllTextsWithNullLanguage() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        Set<String> textSet = new HashSet<String>(NL_TEST_all);
        // test removal of null language
        rep.removeAllNaturalText(field, (String) null);
        for (Iterator<Text> texts = rep.getText(field); texts.hasNext(); textSet.remove(texts.next()
                .getText()))
            ;
        assertTrue(textSet.size() == 2);
        assertTrue(textSet.remove(NL_TEST_noLang));
        assertTrue(textSet.remove(NL_TEST_string)); // and this should be the stringTest
    }

    @Test
    public void testRemoveAllNaturalLanguageValues() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // add a reference to ensure that only texts (and strings) are removed
        String testReference = "http://www.test.org/test";
        rep.addReference(field, testReference);
        // test removal of all natural language values by parsing no languages
        rep.removeAllNaturalText(field);
        Iterator<Text> texts = rep.get(field, (String[]) null);
        assertFalse(texts.hasNext()); // not texts any more
        assertTrue(rep.get(field).hasNext()); // but still a reference!
    }

    @Test
    public void testRemoveAllNaturalLanguageValuesByParsingAnEmptyArray() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // add a reference to ensure that only texts (and strings) are removed
        String testReference = "http://www.test.org/test";
        rep.addReference(field, testReference);
        // test removal of all natural language values by parsing an empty language array
        rep.removeAllNaturalText(field);
        Iterator<Text> texts = rep.get(field, (String[]) null);
        assertFalse(texts.hasNext());
        assertTrue(rep.get(field).hasNext()); // text of the added reference is still present
    }

    @Test
    public void testRemoveAllNaturalLanguageValuesByParsingNullAsLanguageArray() {
        String field = "urn:the.field:used.for.this.Test";
        Representation rep = initNaturalLanguageTest(field);
        // add a reference to ensure that only texts (and strings) are removed
        String testReference = "http://www.test.org/test";
        rep.addReference(field, testReference);
        // test removal of all natural language values by parsing only a single argument
        rep.removeAllNaturalText(field);
        Iterator<Text> texts = rep.get(field, (String[]) null);
        assertFalse(texts.hasNext());
        assertTrue(rep.get(field).hasNext()); // text of the added reference is still present
    }

    /**
     * Default ID for {@link Representation} used for testing
     */
    private static final String DEFAULT_REPRESENTATION_ID = "urm:test:representation.defaultId";

    /**
     * Creates a {@link Representation} instance by using the parsed ID or
     * {@link RepresentationTest#DEFAULT_REPRESENTATION_ID} if <code>null</code> is parsed as ID.
     * 
     * @param id
     *            The ID or <code>null</code> to use the default ID
     * @return the Representation.
     */
    protected Representation createRepresentation(String id) {
        if (id == null) {
            id = DEFAULT_REPRESENTATION_ID;
        }
        return getValueFactory().createRepresentation(id);
    }

}
