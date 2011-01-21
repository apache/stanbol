package org.apache.stanbol.entityhub.servicesapi.mapping;

import java.util.Collection;

import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

public interface FieldMapper extends Cloneable{

    /**
     * Adds a FieldMapping.
     */
    void addMapping(FieldMapping mapping);

    //    private static String getPrefix(String fieldPattern){
    //        return fieldPattern.split("[\\?\\*]")[0];
    //    }
    /**
     * Removes a FieldMapping.
     * @param mapping the Mapping to remove
     */
    void removeFieldMapping(FieldMapping mapping);

    /**
     * Uses the state of the source representation and the configured mappings
     * to update the state of the target {@link Representation}. The state of the
     * source {@link Representation} is not modified. Existing values in the
     * target are not removed nor modified. If the same instance is parsed as
     * source and target representation, fields created by the mapping process
     * are NOT used as source fields for further mappings.
     * @param source the source for the mapping process
     * @param target the target for the processed mappings (can be the same as source)
     * @return the {@link Representation} parsed as target.
     * TODO: This Method should return a MappingReport, that can be stored with
     * the {@link EntityMapping}. However the MappingActivity functionality is
     * not yet designed/implemented!
     */
    Representation applyMappings(Representation source, Representation target);
    /**
     * Getter for the unmodifiable collection of all mappings
     * @return the configured mappings
     */
    Collection<FieldMapping> getMappings();
    /**
     * Creates a clone of this FieldMapper instance with shallow copies of the
     * {@link FieldMapping} instances
     * @return the clone
     */
    FieldMapper clone();
}
