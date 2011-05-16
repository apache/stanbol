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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.apache.stanbol.entityhub.test.Utils.asCollection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.junit.Test;

public abstract class YardTest {

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
}
