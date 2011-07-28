package org.apache.stanbol.entityhub.it.query;

public final class ReferencedSiteFieldQueryTest extends DbpediaQueryTest {
    public static final String REFERENCED_SITE = "dbpedia";
    public static final String REFERENCED_SITE_PATH = "/entityhub/site/"+REFERENCED_SITE;

    /**
     * Executes the {@link DbpediaQueryTest} on the 'dbpedia' referenced
     * site (assuming the default dataset
     */
    public ReferencedSiteFieldQueryTest() {
        super(REFERENCED_SITE_PATH, REFERENCED_SITE);
    }

}
