package eu.iksproject.rick.test.model;

import eu.iksproject.rick.servicesapi.model.Representation;

public abstract class RepresentationTest {
    /**
     * Subclasses implement this method to provide implementation instances of
     * {@link Representation}. This method may be called an arbitrary amount of time,
     * independently whether previously returned MGraph are still in use or not.
     *
     * @return an empty {@link Representation} of the implementation to be tested
     */
    protected abstract Representation getEmptyRepresentation();

    //TODO: Write the tests!
}
