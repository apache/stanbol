package org.apache.stanbol.explanation.api;

import java.util.Set;

public interface SchemaCatalogManager {

    String CATALOG_LOCATIONS = "org.apache.stanbol.explanation.catalogs";

    void addCatalog(SchemaCatalog catalog);

    /**
     * Returns the set of <i>loaded</i> schema catalogs.
     * 
     * @return
     */
    Set<SchemaCatalog> getCatalogs();

    void removeCatalog(String id);

}
