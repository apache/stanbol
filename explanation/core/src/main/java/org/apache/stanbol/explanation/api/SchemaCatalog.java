package org.apache.stanbol.explanation.api;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;

/**
 * A registry of knowledge schemas to be matched against in order to filter relevant statements for
 * explanations.
 * 
 * @author alessandro
 * 
 */
public interface SchemaCatalog {

    public static final String CUSTOM_SCHEMAS = "org.apache.stanbol.explanation.schema.catalog.customschemas";
    
    public static final String ID = "org.apache.stanbol.explanation.schema.catalog.id";

    public static final String LOCATION = "org.apache.stanbol.explanation.schema.catalog.location";

    /**
     * Adds or updates a schema in the catalog. If the exact same or an equal schema is already present in the
     * catalog, no action is taken. If a different schema with the same ID is present, it will be replaced.
     * 
     * @param schema
     */
    void addSchema(Schema schema);

    /**
     * Removes all schemas from the catalog.
     */
    void clearSchems();

    /**
     * Returns the catalog identifier.
     * 
     * @return the catalog identifier.
     */
    String getId();

    /**
     * 
     * @param schemaID
     * @return the requested schema, or null if not present.
     */
    Schema getSchema(IRI schemaID);

    /**
     * Returns all the schemas in the catalog.
     * 
     * @return
     */
    Set<Schema> getSchemas();

    /**
     * Determines if a schema with the given ID is already present in the catalog.
     * 
     * @param schemaID
     * @return true if a schema with <code>schemaID</code> is present, false otherwise.
     */
    boolean hasSchema(IRI schemaID);

    /**
     * Removes any schema whose ID is equal to the supplied one. If no such schema is present, no action is
     * taken.
     * 
     * @param schemaId
     */
    void removeSchema(IRI schemaId);

    /**
     * Tries to remove. Note that if a schema with the same ID as the supplied one, but which is not
     * equivalent by {@link Object#equals(Object)}, is found, it will not be removed. To ensure that any
     * schema with that ID is removed, use {@link #removeSchema(IRI)}.
     * 
     * @param schema
     */
    void removeSchema(Schema schema);
}
