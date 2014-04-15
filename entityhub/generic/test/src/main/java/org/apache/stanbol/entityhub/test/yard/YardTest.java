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
package org.apache.stanbol.entityhub.test.yard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.apache.stanbol.entityhub.test.Utils.asCollection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.RangeConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.junit.Assert;
import org.junit.Test;

public abstract class YardTest {

    /**
     * used for {@link FieldQuery} tests added by STANBOL-1202 
     */
    private FieldQueryTestData fieldQueryTestData;

    /**
     * Getter for the {@link Yard} instance to be tested
     * 
     * @return the {@link Yard} instance to be tested
     */
    protected abstract Yard getYard();

    /**
     * Stores all the IDs of Representations created by the create(..) methods. This collection is used to
     * clean up the store after all the unit tests are executed.
     */
    protected static Collection<String> representationIds = new HashSet<String>();

    protected Representation create() throws YardException {
        Representation r = getYard().create();
        representationIds.add(r.getId());
        return r;
    }

    protected Representation create(String id, boolean store) throws YardException {
        Representation r;
        if (store) {
            r = getYard().create(id);
        } else if (id != null && !id.isEmpty()) {
            r = getYard().getValueFactory().createRepresentation(id);
        } else {
            throw new IllegalArgumentException("If store is FALSE the id MUST NOT be NULL nor EMPTY!");
        }
        representationIds.add(r.getId());
        return r;
    }

    @Test
    public void testGetValueFactory() {
        assertNotNull("The ValueFactory MUST NOT be NULL", getYard().getValueFactory());
    }

    @Test
    public void testGetQueryFactory() {
        assertNotNull("The QueryFactory MUST NOT be NULL", getYard().getQueryFactory());
    }

    @Test
    public void testYardId() {
        assertNotNull("The ID of the Yard MUST NOT be NULL", getYard().getId());
    }

    @Test
    public void testYardName() {
        assertNotNull("The Name of the Yard MUST NOT be NULL", getYard().getName());
        assertFalse("The Name of the Yard MUST NOT be an empty String", getYard().getName().isEmpty());
    }

    @Test
    public void testDefaultCreate() throws YardException {
        Yard yard = getYard();
        Representation test = yard.create();
        assertNotNull(test);
        Representation test2 = yard.create();
        assertNotNull(test2);
        assertNotSame(test, test2);
        assertFalse("Two Representation created with create() MUST NOT be equals", test.equals(test2));
    }

    @Test
    public void testCreateWithNull() throws YardException {
        Yard yard = getYard();
        Representation test = yard.create(null);
        assertNotNull("Parsing NULL to create(String) MUST create a valid Representation", test);
        representationIds.add(test.getId()); // add id to cleanup list
        Representation test2 = yard.create(null);
        assertNotNull("Parsing NULL to create(String) MUST create a valid Representation", test2);
        representationIds.add(test2.getId()); // add id to cleanup list
        assertNotSame(test, test2);
        assertFalse("Two Representation created with create(null) MUST NOT be equals", test.equals(test2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithEmptyString() throws YardException {
        getYard().create("");// throws an IllegalArgumentException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithExistingId() throws YardException {
        Yard yard = getYard();
        Representation test = create();
        assertNotNull(test);
        yard.create(test.getId()); // throws an IllegalArgumentException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsRepresentationWithNull() throws YardException {
        getYard().isRepresentation(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsRepresentationWithEmptyString() throws YardException {
        getYard().isRepresentation("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStoreRepresentationWithNull() throws YardException {
        getYard().store((Representation) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStoreRepresentationsWithNull() throws YardException {
        getYard().store((Iterable<Representation>) null);
    }

    @Test
    public void testStoreRepresentation() throws YardException {
        // NOTE: this does not test if the updated view of the representation is
        // stored, but only that the store method works for representations
        // that are already in the Yard AND representations that are not yet
        // present within the yard
        String testId = "urn:yard.test.testStoreRepresentation:representation.id1";
        String testId2 = "urn:yard.test.testStoreRepresentation:representation.id2";
        Yard yard = getYard();
        Representation test = create(testId, false);
        Representation added = yard.store(test); // this adds the representation
        assertEquals(test, added);
        Representation test2 = create(testId2, true); // this creates and adds the representation
        // now test that the representation can also be updated
        added = yard.store(test2);
        assertEquals(test2, added);
    }

    @Test
    public void testStoreRepresentations() throws YardException {
        // NOTE: this does not test if the updated view of the representation is
        // stored, but only that the store method works for representations
        // that are already in the Yard AND representations that are not yet
        // present within the yard
        String testId = "urn:yard.test.testStoreRepresentations:representation.id1";
        String testId2 = "urn:yard.test.testStoreRepresentations:representation.id2";
        String field = "urn:the.field:used.for.this.Test";
        Yard yard = getYard();
        Representation test = create(testId, false);
        Representation test2 = create(testId2, true); // this creates and adds the representation
        // now add both and mix Representations that are already present in the yard
        // with an other that is not yet present in the yard

        // change the representations to be sure to force an update even if the
        // implementation checks for changes before updating a representation
        test2.add(field, "test value 2");
        Iterable<Representation> addedIterable = yard.store(Arrays.asList(test, test2));
        assertNotNull(addedIterable);
        Collection<Representation> added = asCollection(addedIterable.iterator());
        // test that both the parsed Representations where stored (updated & created)
        assertTrue(added.remove(test));
        assertTrue(added.remove(test2));
        assertTrue(added.isEmpty());
    }

    @Test
    public void testStoreRepresentationsWithNullElement() throws YardException {
        String testId = "urn:yard.test.testStoreRepresentationsWithNullElement:representation.id";
        Yard yard = getYard();
        Representation test = create(testId, false);
        Iterable<Representation> added = yard.store(Arrays.asList(test, null));
        // now test that only the valid representation was added and the null
        // value was ignored
        assertNotNull(added);
        Iterator<Representation> addedIt = added.iterator();
        assertTrue(addedIt.hasNext());
        assertEquals(test, addedIt.next());
        assertFalse(addedIt.hasNext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRepresentationWithNull() throws YardException {
        getYard().getRepresentation(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRepresentationWithEmptyString() throws YardException {
        getYard().getRepresentation("");
    }

    @Test
    public void testGetNonExistantRepresentation() throws YardException {
        String id = "urn:yard.test.testGetNonExistantRepresentation:representation.id";
        assertNull(getYard().getRepresentation(id));
    }

    /**
     * This tests that {@link Representation} retrieved from the Yard do not influence other
     * {@link Representation} instances for the same ID. It is important, that when different
     * {@link Representation} instances are returned to different caller, that these do not influence each
     * other.
     * <p>
     * It also tests, that any update to the {@link Representation} as managed by the {@link Yard} does also
     * not influence {@link Representation} instances that where created before the change.
     * 
     * @throws YardException
     */
    @Test
    public void testGetRepresentation() throws YardException {
        String id = "urn:yard.test.testGetRepresentation.id";
        Yard yard = getYard();
        String field = "urn:the.field:used.for.this.Test";
        String value1 = "this is a test";
        // Representations created via the yard need to be created (as empty
        // representation within the yard
        Representation test = create(id, true);
        // retrieve the representation from the store
        Representation retrieved = yard.getRepresentation(id);
        assertNotNull(retrieved);
        // they MUST NOT be the same, but the MUST be equals
        // Note:
        // the fact that two representations with the same id are equals is tested
        // by the unit tests for the representation interface
        assertEquals(test, retrieved);
        assertNotSame("getRepresentation MUST return an new instance for each "
                      + "call, so that changes in one return instance do not influence "
                      + "an other returned instance!", test, retrieved);
        // now add a property to the original one
        test.add(field, value1);
        // and check that the retrieved does not have the value
        assertFalse(retrieved.get(field).hasNext());
        // now store the representation and check that updated are not reflected
        // within the retrieved one
        yard.store(test);
        assertFalse(retrieved.get(field).hasNext());
        // now retrieve again an representation
        retrieved = null;
        retrieved = yard.getRepresentation(id);
        // now the Representation MUST HAVE the new field
        assertTrue(retrieved.get(field).hasNext());
        assertEquals(value1, retrieved.getFirst(field));

        // finally retrieve a second and perform the change test again
        Representation retrieved2 = yard.getRepresentation(id);
        retrieved.removeAll(field);
        // check the value is still in retrieved2
        assertTrue(retrieved2.get(field).hasNext());
        assertEquals(value1, retrieved2.getFirst(field));

    }

    @Test
    public void testIsRepresentation() throws YardException {
        String id = "urn:yard.test.testIsRepresentation:representation.id";
        Yard yard = getYard();
        // Representations created via the yard need to be created (as empty
        // representation within the yard
        Representation test = create();
        assertTrue(yard.isRepresentation(test.getId()));
        // Representations created via the ValueFactory MUST NOT be added to the
        // Yard
        Representation test2 = create(id, false);
        assertFalse(yard.isRepresentation(test2.getId()));
        // now store test2 and test again
        yard.store(test2);
        assertTrue(yard.isRepresentation(test2.getId()));
        // now remove test and test again
        yard.remove(test.getId());
        assertFalse(yard.isRepresentation(test.getId()));
        yard.remove(test2.getId());
        assertFalse(yard.isRepresentation(test2.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRepresentationWithNull() throws YardException {
        getYard().update((Representation) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRepresentationWithNonPresent() throws YardException {
        String id = "urn:yard.test.testUpdateRepresentationWithNonPresent:representation.id";
        Representation test = create(id, false);
        getYard().update(test); // throws an Exception because test is not part of the yard
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRepresentationsWithNull() throws YardException {
        getYard().update((Iterable<Representation>) null);
    }

    @Test
    public void testUpdateRepresentations() throws YardException {
        // NOTE: this does not test if the updated view of the representation is
        // stored, but only that the update method works correctly
        Yard yard = getYard();
        String id1 = "urn:yard.test.testUpdateRepresentations:representation.id1";
        String id2 = "urn:yard.test.testUpdateRepresentations:representation.id2";
        String field = "urn:the.field:used.for.this.Test";
        Representation test1 = create(id1, true);
        Representation test2 = create(id2, true);
        // change the representations to be sure to force an update even if the
        // implementation checks for changes before updating a representation
        test1.add(field, "test value 1");
        test2.add(field, "test value 2");
        Iterable<Representation> updatedIterable = yard.update(Arrays.asList(test1, test2));
        assertNotNull(updatedIterable);
        Collection<Representation> updated = asCollection(updatedIterable.iterator());
        // test that both the parsed Representations where stored (updated & created)
        assertTrue(updated.remove(test1));
        assertTrue(updated.remove(test2));
        assertTrue(updated.isEmpty());
    }

    /**
     * When updating multiple null values need to be ignored.
     * 
     * @throws YardException
     */
    @Test
    public void testUpdateRepresentationsWithNullElement() throws YardException {
        // NOTE: this does not test if the updated view of the representation is
        // stored, but only that the update method works correctly
        Yard yard = getYard();
        String id1 = "urn:yard.test.testUpdateRepresentationsWithNullElement:representation.id";
        String field = "urn:the.field:used.for.this.Test";
        Representation test1 = create(id1, true);
        // change the representations to be sure to force an update even if the
        // implementation checks for changes before updating a representation
        test1.add(field, "test value 1");
        Iterable<Representation> updated = yard.update(Arrays.asList(test1, null));
        assertNotNull(updated);
        Iterator<Representation> updatedIt = updated.iterator();
        assertTrue(updatedIt.hasNext());
        assertEquals(test1, updatedIt.next());
        assertFalse(updatedIt.hasNext());
    }

    /**
     * When updating multiple non present representations need to be ignored.
     * 
     * @throws YardException
     */
    @Test
    public void testUpdateRepresentationsWithNonPresent() throws YardException {
        // NOTE: this does not test if the updated view of the representation is
        // stored, but only that the update method works correctly
        Yard yard = getYard();
        String id1 = "urn:yard.test.testUpdateRepresentationsWithNonPresent:representation.id1";
        String id2 = "urn:yard.test.testUpdateRepresentationsWithNonPresent:representation.id2";
        String field = "urn:the.field:used.for.this.Test";
        Representation test1 = create(id1, true);
        // change the representations to be sure to force an update even if the
        // implementation checks for changes before updating a representation
        test1.add(field, "test value 1");
        // create a 2nd Representation by using the ValueFactory (will not add it
        // to the yard!)
        Representation test2 = create(id2, false);
        // now test1 is present and test2 is not.
        Iterable<Representation> updated = yard.update(Arrays.asList(test1, test2));
        // We expect that only test1 is updated and test2 is ignored
        assertNotNull(updated);
        Iterator<Representation> updatedIt = updated.iterator();
        assertTrue(updatedIt.hasNext());
        assertEquals(test1, updatedIt.next());
        assertFalse(updatedIt.hasNext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRepresentationWithNull() throws YardException {
        getYard().remove((String) null);
    }

    /**
     * Tests two things:
     * <ol>
     * <li>if representations are removed form the yard
     * <li>if the representation instance is still valid after it is removed
     * </ol>
     * The second is very important, because Representations might still be used by other components after
     * they are remove from the store they where created in
     * 
     * @throws YardException
     */
    @Test
    public void testRemoveRepresentation() throws YardException {
        // NOTE: This test needs not to use the create(..) method, because we
        // remove the created representation form the store anyway as part of the
        // test
        String id = "urn:yard.test.tesrRemoveRepresentation:representation.id";
        String field = "urn:the.field:used.for.this.Test";
        String testValue = "This is a test";
        Yard yard = getYard();
        Representation test = yard.getValueFactory().createRepresentation(id);
        test.add(field, testValue);
        yard.store(test); // store the representation
        assertTrue(yard.isRepresentation(test.getId())); // test if stored
        test = null; // free the initial
        // create the instance form the store to test (2)
        test = yard.getRepresentation(id);
        assertEquals(id, test.getId()); // only to be sure ...
        yard.remove(test.getId()); // test (1) - the remove
        assertFalse(yard.isRepresentation(test.getId()));// test if removed
        // test if the test object is not destroyed by removing the representation
        // it represents form the store (2)
        assertEquals(testValue, test.getFirst(field));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRepresentationsWithNull() throws YardException {
        getYard().remove((Iterable<String>) null);
    }

    /**
     * Tests that multiple Representations are removed.
     * 
     * @throws YardException
     */
    @Test
    public void testRemoveRepresentations() throws YardException {
        // NOTE: This test needs not to use the create(..) method, because we
        // remove the created representation form the store anyway as part of the
        // test
        String id = "urn:yard.test.testRemoveRepresentationsWithNullValue:representation.id1";
        String id2 = "urn:yard.test.testRemoveRepresentationsWithNullValue:representation.id2";
        String field = "urn:the.field:used.for.this.Test";
        String testValue = "This is a test";
        Yard yard = getYard();
        // use both ways to add the two Representations (should make no differences,
        // but one never can know ...
        Representation test1 = yard.create(id); // create and add
        Representation test2 = yard.getValueFactory().createRepresentation(id2); // create
        test2.add(field, testValue); // add value
        yard.store(test2);// store
        assertTrue(yard.isRepresentation(test1.getId())); // test if stored
        assertTrue(yard.isRepresentation(test2.getId()));
        yard.remove(Arrays.asList(test1.getId(), test2.getId())); // remove
        assertFalse(yard.isRepresentation(test1.getId())); // test if removed
        assertFalse(yard.isRepresentation(test2.getId()));
    }
    /**
     * Tests that multiple Representations are removed.
     * 
     * @throws YardException
     */
    @Test
    public void testRemoveAll() throws YardException {
        // NOTE: This test needs not to use the create(..) method, because we
        // remove the created representation form the store anyway as part of the
        // test
        String id = "urn:yard.test.testRemoveAll:representation.id1";
        String id2 = "urn:yard.test.testRemoveAll:representation.id2";
        String field = "urn:the.field:used.for.this.Test";
        String testValue = "This is a test";
        Yard yard = getYard();
        // use both ways to add the two Representations (should make no differences,
        // but one never can know ...
        Representation test1 = yard.create(id); // create and add
        test1.add(field, testValue); // add value
        yard.store(test1);// store
        Representation test2 = yard.getValueFactory().createRepresentation(id2); // create
        test2.add(field, testValue); // add value
        yard.store(test2);// store
        assertTrue(yard.isRepresentation(test1.getId())); // test if stored
        assertTrue(yard.isRepresentation(test2.getId()));
        yard.removeAll(); // remove
        assertFalse(yard.isRepresentation(test1.getId())); // test if removed
        assertFalse(yard.isRepresentation(test2.getId()));
        //test that Yard is still useable after removeAll
        yard.store(test1);// store
        assertTrue(yard.isRepresentation(test1.getId())); // test if stored
        yard.removeAll(); // remove
        assertFalse(yard.isRepresentation(test1.getId()));
    }
    /**
     * Tests if <code>null</code> values within the Iterable are ignored and do not cause an Exception
     * 
     * @throws YardException
     */
    @Test
    public void testRemoveRepresentationsWithNullValue() throws YardException {
        // NOTE: This test needs not to use the create(..) method, because we
        // remove the created representation form the store anyway as part of the
        // test
        String id = "urn:yard.test.testRemoveRepresentationsWithNullValue:representation.id";
        Yard yard = getYard();
        Representation test = yard.create(id); // create and add
        assertTrue(yard.isRepresentation(test.getId()));
        yard.remove(Arrays.asList(test.getId(), null));
        assertFalse(yard.isRepresentation(test.getId()));
    }

    /**
     * Tests that {@link Representation} IDs that are not stored by the yard are ignored by the multiple
     * remove method
     * 
     * @throws YardException
     */
    @Test
    public void testRemoveRepresentationsWithNonStoredValue() throws YardException {
        // NOTE: This test needs not to use the create(..) method, because we
        // remove the created representation form the store anyway as part of the
        // test
        String id = "urn:yard.test.testRemoveRepresentationsWithNullValue:representation.stored";
        String id2 = "urn:yard.test.testRemoveRepresentationsWithNullValue:representation.notStored";
        Yard yard = getYard();
        Representation test = yard.create(id); // create and add
        assertTrue(yard.isRepresentation(test.getId()));
        yard.remove(Arrays.asList(test.getId(), id2));
        assertFalse(yard.isRepresentation(test.getId()));
        assertFalse(yard.isRepresentation(id2));
    }

    /**
     * The {@link Representation} as stored in the Yard MUST NOT change if the source {@link Representation}
     * stored to the {@link Yard} or an Representation retrieved from the Yard is changed. Only the
     * {@link Yard#store(Representation))} and the {@link Yard#update(Representation)} methods are allowed to
     * synchronised the Representation within the Yard with the state (changes) of the parsed value.
     * 
     * @throws YardException
     */
    @Test
    public void testChangeRepresentation() throws YardException {
        String id = "urn:yard.test.testChangeRepresentation:representation.id";
        String field = "urn:the.field:used.for.this.Test";
        String testValue = "This is a test";
        Yard yard = getYard();
        // use the ValueFactory to create the representation to ensure that this
        // instance has nothing to do with the store
        Representation test = create(id, false);
        // now store the empty Representation
        yard.store(test);
        // now add a values
        test.add(field, testValue);
        // now get the representation from the yard
        Representation stored = yard.getRepresentation(id);
        // test if the stored version does not contain the value
        assertFalse(stored.get(field).hasNext());
        stored = null;
        // now store the updated version
        yard.store(test);
        // now check that the updated value is stored
        stored = yard.getRepresentation(id);
        assertEquals(testValue, stored.getFirst(field));

        // now we need to test if modifications of an Representation returned
        test = stored;
        stored = null;
        String testValue2 = "This is an ohter test value";
        test.add(field, testValue2);

        // now get the representation from the yard and check that it still has
        // only one value
        stored = yard.getRepresentation(id);
        Collection<Object> values = asCollection(stored.get(field));
        assertTrue(values.remove(testValue)); // test that it contains the value
        assertTrue(values.isEmpty()); // and that there is only this value
        values = null;
        // now update the stored version with the new state
        stored = null;
        stored = yard.update(test);
        values = asCollection(stored.get(field));
        assertTrue(values.remove(testValue)); // test that it contains the original
        assertTrue(values.remove(testValue2)); // test that it contains the 2nd value
        assertTrue(values.isEmpty()); // and that there are only these two values
        values = null;
    }
    /*
     * All the follow up tests where added as part of STANBOL-1202
     */
    /**
     * Simple test for the QueryFactory
     */
    @Test
    public void testQeuryFactory(){
        FieldQueryFactory qf = getYard().getQueryFactory();
        Assert.assertNotNull("The getter for the FieldQueryFactory MUST NOT return NULL!", qf);
        FieldQuery query = qf.createFieldQuery();
        Assert.assertNotNull("The FieldQueryFactory returned NULL as query", query);
    }
    /**
     * Test a simple {@link TextConstraint} for any language
     */
    @Test
    public void testFindText(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //query for all languages and value1
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(data.textValue1.getText()));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList( data.r1.getId(), data.r1en.getId(), data.r1de.getId()), 
                Arrays.asList(data.textField, data.refField));
        
        //same for value2
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(data.textValue2.getText()));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList( data.r2.getId(), data.r2en.getId(), data.r2de.getId()), 
                Arrays.asList(data.textField, data.refField));
    }
    /**
     * Same as {@link #testFindText()} but using 
     * {@link Yard#findRepresentation(FieldQuery)} to execute the queries
     */
    @Test
    public void testFindRepresentationText(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //query for all languages and value1
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(data.textValue1.getText()));
        validateQueryResults(query, getYard().findRepresentation(query), 
                Arrays.asList( data.r1.getId(), data.r1en.getId(), data.r1de.getId()), 
                Arrays.asList(data.textField, data.refField, data.intField));
        
        //same for value2
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(data.textValue2.getText()));
        validateQueryResults(query, getYard().findRepresentation(query), 
                Arrays.asList( data.r2.getId(), data.r2en.getId(), data.r2de.getId()), 
                Arrays.asList(data.textField, data.refField, data.intField));
    }
    /**
     * Same as {@link #testFindText()} but using 
     * {@link Yard#findReferences(FieldQuery)} to execute the queries
     */
    @Test
    public void testFindReferencesText(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //query for all languages and value1
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(data.textValue1.getText()));
        validateQueryResults(query, getYard().findReferences(query), 
                Arrays.asList(data.r1.getId(), data.r1en.getId(), data.r1de.getId()));
        
        //same for value2
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(data.textValue2.getText()));
        validateQueryResults(query, getYard().findReferences(query),
                Arrays.asList( data.r2.getId(), data.r2en.getId(), data.r2de.getId()));
    }

    /**
     * Test a simple {@link TextConstraint} for any language
     */
    @Test
    public void testFindTextOfLanguage(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //value1@en
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(
                data.textValue1.getText(), "en"));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1en.getId()), 
                Arrays.asList(data.textField, data.refField));
        
        //value2@de
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(
                data.textValue2.getText(), "de"));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r2de.getId()), 
                Arrays.asList(data.textField, data.refField));

        //value1@null
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(
                data.textValue1.getText(), (String)null));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1.getId()), 
                Arrays.asList(data.textField, data.refField));

        //value1@null,en
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(
                data.textValue1.getText(), null, "en"));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1.getId(),data.r1en.getId()), 
                Arrays.asList(data.textField, data.refField));

        //value1@en,de
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(
                data.textValue1.getText(), "en", "de"));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1en.getId(),data.r1de.getId()), 
                Arrays.asList(data.textField, data.refField));
    }
    
    @Test
    public void testFindTextWildcards(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //prefix search with *
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        String wildcard = data.textValue1.getText();
        wildcard = wildcard.substring(0, wildcard.length()-1) + "*";
        query.setConstraint(data.textField, new TextConstraint(wildcard,PatternType.wildcard,false, "en"));
        query.addSelectedField(data.refField);
        query.addSelectedField(data.textField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1en.getId(), data.r2en.getId()), 
                Arrays.asList(data.refField, data.textField));
        
        //wildcard with ?
        query = getYard().getQueryFactory().createFieldQuery();
        //selects r1en and r2en
        wildcard = data.textValue1.getText();
        wildcard = wildcard.substring(0, wildcard.length()-1) + "?";
        query.setConstraint(data.textField, new TextConstraint(wildcard,PatternType.wildcard,false, "de"));
        query.addSelectedField(data.refField);
        query.addSelectedField(data.textField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1de.getId(), data.r2de.getId()), 
                Arrays.asList(data.refField, data.textField));
    }
    
    /**
     * Tests a TextConstraint with multiple optional values
     */
    @Test
    public void testfindOptionalTexts(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //value1@en || value2@en
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(
                Arrays.asList(data.textValue1.getText(), data.textValue2.getText()),
                "en"));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1en.getId(),data.r2en.getId()), 
                Arrays.asList(data.textField, data.refField));
        
        //value1@en,de || value2@en,de
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.textField, new TextConstraint(
                Arrays.asList(data.textValue1.getText(), data.textValue2.getText()),
                "en", "de"));
        query.addSelectedField(data.textField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1en.getId(),data.r1de.getId(), 
                        data.r2en.getId(), data.r2de.getId()), 
                Arrays.asList(data.textField, data.refField));
    }
    
    /**
     * Test a {@link ReferenceConstraint}
     */
    @Test
    public void testFindReference(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //query for all languages and value1
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.refField, new ReferenceConstraint(data.refValue1.getReference()));
        query.addSelectedField(data.intField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1.getId(), data.r1en.getId(), data.r1de.getId(), data.r5.getId()), 
                Arrays.asList(data.intField, data.refField));
        
        //same for value2
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.refField, new ReferenceConstraint(data.refValue2.getReference()));
        query.addSelectedField(data.intField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r2.getId(), data.r2en.getId(), data.r2de.getId(), data.r10.getId()), 
                Arrays.asList(data.intField, data.refField));
    }
    
    /**
     * Tests simple {@link ValueConstraint}s
     */
    @Test
    public void testFindValues(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //query for all languages and value1
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.intField, new ValueConstraint(data.intValue1));
        query.addSelectedField(data.intField);
        query.addSelectedField(data.textField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1.getId(), data.r1en.getId(), data.r1de.getId()), 
                Arrays.asList(data.intField, data.textField));
        
        //same for value2
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.intField, new ValueConstraint(data.intValue2));
        query.addSelectedField(data.intField);
        query.addSelectedField(data.textField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r2.getId(), data.r2en.getId(), data.r2de.getId()), 
                Arrays.asList(data.intField, data.textField));
        
    }
    /**
     * Tests simple {@link RangeConstraint}
     */
    @Test
    public void testFindRange(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //query for all languages and value1
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.intField, new RangeConstraint(data.intValue2,data.intValue5,true));
        query.addSelectedField(data.intField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r2.getId(), data.r2en.getId(), data.r2de.getId(), data.r5.getId()), 
                Arrays.asList(data.intField, data.refField));
        
        //same for value2
        query = getYard().getQueryFactory().createFieldQuery();
        query.setConstraint(data.intField, new RangeConstraint(data.intValue2,data.intValue10,false));
        query.addSelectedField(data.intField);
        query.addSelectedField(data.textField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r5.getId()), 
                Arrays.asList(data.intField, data.textField));
    }
    /**
     * Tests simple {@link RangeConstraint}
     */
    @Test
    public void testFindMultipleConstraints(){
        //init the test data
        FieldQueryTestData data = getFieldQueryTestData();
        //Integer Range and reference query
        FieldQuery query = getYard().getQueryFactory().createFieldQuery();
        //selects r2, r2en, r2de, r5
        query.setConstraint(data.intField, new RangeConstraint(data.intValue2,data.intValue5, true));
        //selects r1, r1en, r1de, r5
        query.setConstraint(data.refField, new ReferenceConstraint(data.refValue1.getReference()));
        query.addSelectedField(data.intField);
        query.addSelectedField(data.refField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r5.getId()), 
                Arrays.asList(data.intField, data.refField));
        
        //text and reference
        query = getYard().getQueryFactory().createFieldQuery();
        //selects r1en and r2en
        String wildcard = data.textValue1.getText();
        wildcard = wildcard.substring(0, wildcard.length()-1) + "*";
        query.setConstraint(data.textField, new TextConstraint(wildcard,PatternType.wildcard,false, "en"));
        //selects r1, r1en, r1de, r5
        query.setConstraint(data.refField, new ReferenceConstraint(data.refValue1.getReference()));
        query.addSelectedField(data.refField);
        query.addSelectedField(data.textField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r1en.getId()), 
                Arrays.asList(data.refField, data.textField));
        
        //text and value
        query = getYard().getQueryFactory().createFieldQuery();
        //selects r1de and r2de
        query.setConstraint(data.textField, new TextConstraint(wildcard,PatternType.wildcard,false, "de"));
        //selects r2, r2en, r2de
        query.setConstraint(data.intField, new ValueConstraint(data.intValue2));
        query.addSelectedField(data.refField);
        query.addSelectedField(data.textField);
        validateQueryResults(query, getYard().find(query), 
                Arrays.asList(data.r2de.getId()), 
                Arrays.asList(data.refField, data.textField));
    }

    /**
     * Used to validate the results of the query against expected results. 
     * Supports also the validation of selected fields
     * @param query the query
     * @param results the results of the query
     * @param parsedExpectedResults read-only list of expected results
     * @param parsedExpectedFields read-only list of expected selected fields or
     * <code>null</code> to deactivate validation of fields
     */
    protected final void validateQueryResults(FieldQuery query, 
            QueryResultList<Representation> results,
            Collection<String> parsedExpectedResults, 
            Collection<String> parsedExpectedFields) {
        Set<String> expectedResults = parsedExpectedResults == null ?
                new HashSet<String>() : new HashSet<String>(parsedExpectedResults);
        Set<String> expectedFields = parsedExpectedFields == null ? null : 
            new HashSet<String>(parsedExpectedFields);
        FieldQueryTestData data = getFieldQueryTestData();
        Assert.assertNotNull("NULL result for query "+query+"!", results);
        for(Representation result : results){
            Assert.assertTrue("Result '"+ result.getId() + "' is missing for Query "
                    + query +"!", expectedResults.remove(result.getId()));
            if(expectedFields != null){ //validate fields
                for(String field : expectedFields){
                    Set<Object> expectedFieldValues = ModelUtils.asSet(
                            data.representations.get(result.getId()).get(field));
                    Iterator<Object> fieldValues = result.get(field);
                    while(fieldValues.hasNext()){
                        Object fieldValue = fieldValues.next();
                        Assert.assertTrue("Unexpected value '" + fieldValue
                                + " of selected field '" + field + "' of result "
                                + result.getId(),expectedFieldValues.remove(fieldValue));
                    }
                    Assert.assertTrue("Missing expected value(s) "+ expectedFieldValues
                            + " of selected field '" + field + "' of result "
                            + result.getId(), expectedFieldValues.isEmpty());
                }
            }
        }
        Assert.assertTrue("Missing expected result(s) " + expectedResults
                + "for query" + query +"!", expectedResults.isEmpty());
    }
    
    /**
     * Used to validate {@link Yard#findReferences(FieldQuery)} results
     * @param query the query
     * @param results the results
     * @param parsedExpectedResults the expected results
     */
    protected final void validateQueryResults(FieldQuery query, 
            QueryResultList<String> results,
            Collection<String> parsedExpectedResults) {
        Set<String> expectedResults = parsedExpectedResults == null ?
                new HashSet<String>() : new HashSet<String>(parsedExpectedResults);
        Assert.assertNotNull("NULL result for query "+query+"!", results);
        for(String id : results){
            Assert.assertTrue("Result "+ id + "is missing for Query "
                    + query +"!", expectedResults.remove(id));
        }
        Assert.assertTrue("Missing expected result(s) " + expectedResults
                + "for query" + query +"!", expectedResults.isEmpty());
    }       

    /**
     * Class representing the test data for the {@link FieldQuery} tests added
     * by STANBOL-1202
     * 
     * @author Rupert Westenthaler
     *
     */
    protected final class FieldQueryTestData {
        /**
         * The field used for {@link Text} values
         */
        protected final String textField;
        protected final Text textValue1;
        protected final Text textValue1en;
        protected final Text textValue1de;
        protected final Text textValue2;
        protected final Text textValue2en;
        protected final Text textValue2de;
        /**
         * The field used for {@link Reference} values
         */
        protected final String refField;
        protected final Reference refValue1;
        protected final Reference refValue2;
        /**
         * The field used for {@link Integer} values
         */
        protected final String intField;
        protected final Integer intValue1;
        protected final Integer intValue2;
        protected final Integer intValue5;
        protected final Integer intValue10;
        /*
         * The Entities created by the test
         */
        /**
         * Entity with {@link #textValue1}, {@link #refValue1} and
         * {@link #intValue1}
         */
        protected final Representation r1;
        /**
         * Entity with {@link #textValue1en}, {@link #refValue1} and
         * {@link #intValue1}
         */
        protected final Representation r1en;
        /**
         * Entity with {@link #textValue1de}, {@link #refValue1} and
         * {@link #intValue1}
         */
        protected final Representation r1de;
        /**
         * Entity with {@link #textValue2}, {@link #refValue2} and
         * {@link #intValue2}
         */
        protected final Representation r2;
        /**
         * Entity with {@link #textValue2en}, {@link #refValue2} and
         * {@link #intValue2}
         */
        protected final Representation r2en;
        /**
         * Entity with {@link #textValue2de}, {@link #refValue2} and
         * {@link #intValue2}
         */
        protected final Representation r2de;
        /**
         * Entity with {@link #intValue5} and {@link #refValue1}
         */
        protected final Representation r5;
        /**
         * Entity with {@link #intValue10} and {@link #refValue2}
         */
        protected final Representation r10;
        /**
         * Read-only {@link Map} allowing to lookup {@link Representation}s
         * used by this Test by {@link Representation#getId() ID}.
         */
        protected final Map<String,Representation> representations;
        
        /**
         * Creates and initializes the query test data for the tested
         * {@link YardTest#getYard() Yard}.
         */
        private FieldQueryTestData(){
            textField = "urn:yard.test:find:field.text";
            textValue1 = getYard().getValueFactory().createText("value1", null);
            textValue1en = getYard().getValueFactory().createText("value1", "en");
            textValue1de = getYard().getValueFactory().createText("value1", "de");
            textValue2 = getYard().getValueFactory().createText("value2", null);
            textValue2en = getYard().getValueFactory().createText("value2", "en");
            textValue2de = getYard().getValueFactory().createText("value2", "de");

            refField = "urn:yard.test:find:field.reference";
            refValue1 = getYard().getValueFactory().createReference("urn:yard.test:find:reference1");
            refValue2 = getYard().getValueFactory().createReference("urn:yard.test:find:reference2");

            intField = "urn:yard.test:find:field.integer";
            intValue1 = 1;
            intValue2 = 2;
            intValue5 = 5;
            intValue10 = 10;
            
            //and some Representations with a different set of values
            Map<String,Representation> representations = new HashMap<String, Representation>();
            r1 = create("urn:yard.test:find:entity.r1", false);
            r1.add(textField, textValue1);
            r1.add(intField, intValue1);
            r1.add(refField, refValue1);
            getYard().store(r1);
            representations.put(r1.getId(), r1);
            r1en = create("urn:yard.test:find:entity.r1en", false);
            r1en.add(textField, textValue1en);
            r1en.add(intField, intValue1);
            r1en.add(refField, refValue1);
            getYard().store(r1en);
            representations.put(r1en.getId(), r1en);
            r1de = create("urn:yard.test:find:entity.r1de", false);
            r1de.add(textField, textValue1de);
            r1de.add(intField, intValue1);
            r1de.add(refField, refValue1);
            getYard().store(r1de);
            representations.put(r1de.getId(), r1de);
            
            r2 = create("urn:yard.test:find:entity.r2", false);
            r2.add(textField, textValue2);
            r2.add(intField, intValue2);
            r2.add(refField, refValue2);
            getYard().store(r2);
            representations.put(r2.getId(), r2);
            r2en = create("urn:yard.test:find:entity.r2en", false);
            r2en.add(textField, textValue2en);
            r2en.add(intField, intValue2);
            r2en.add(refField, refValue2);
            getYard().store(r2en);
            representations.put(r2en.getId(), r2en);
            r2de = create("urn:yard.test:find:entity.r2de", false);
            r2de.add(textField, textValue2de);
            r2de.add(intField, intValue2);
            r2de.add(refField, refValue2);
            getYard().store(r2de);
            representations.put(r2de.getId(), r2de);
            
            r5 = create("urn:yard.test:find:entity.r5", false);
            r5.add(intField, intValue5);
            r5.add(refField, refValue1);
            getYard().store(r5);
            representations.put(r5.getId(), r5);

            r10 = create("urn:yard.test:find:entity.r10", false);
            r10.add(intField, intValue10);
            r10.add(refField, refValue2);
            getYard().store(r10);           
            representations.put(r10.getId(), r10);
            this.representations = Collections.unmodifiableMap(representations);
        }
    }
    
    /**
     * Getter for the {@link FieldQueryTestData}. This will also initialize
     * the data for the tested {@link Yard}
     * @return the {@link FieldQueryTestData}
     */
    protected final FieldQueryTestData getFieldQueryTestData() {
        if(fieldQueryTestData == null){ //if not yet initialized for this test
            //create (and add) the test data for this test
            fieldQueryTestData = new FieldQueryTestData(); 
        }
        return fieldQueryTestData;

    }
}
