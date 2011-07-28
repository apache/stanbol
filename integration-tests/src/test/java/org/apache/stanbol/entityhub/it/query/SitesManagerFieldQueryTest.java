package org.apache.stanbol.entityhub.it.query;

public final class SitesManagerFieldQueryTest extends DbpediaQueryTest {

    public static final String SITES_MANAGER_PATH = "/entityhub/sites";

    /**
     * Executes the {@link DbpediaQueryTest} on the Entityhub Sites Manager
     * service (/entityhub/sites)
     */
    public SitesManagerFieldQueryTest() {
        super(SITES_MANAGER_PATH, null);
    }

}
